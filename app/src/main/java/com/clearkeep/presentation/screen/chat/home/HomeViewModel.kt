package com.clearkeep.presentation.screen.chat.home

import androidx.lifecycle.*
import com.clearkeep.data.repository.*
import com.clearkeep.domain.repository.*
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Status
import com.clearkeep.data.local.preference.UserPreferencesStorage
import com.clearkeep.domain.model.*
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.notification.RegisterTokenUseCase
import com.clearkeep.domain.usecase.group.FetchGroupsUseCase
import com.clearkeep.domain.usecase.group.GetAllPeerGroupByDomainUseCase
import com.clearkeep.domain.usecase.group.GetAllRoomsUseCase
import com.clearkeep.domain.usecase.message.ClearTempMessageUseCase
import com.clearkeep.domain.usecase.message.ClearTempNotesUseCase
import com.clearkeep.domain.usecase.people.GetListClientStatusUseCase
import com.clearkeep.domain.usecase.people.SendPingUseCase
import com.clearkeep.domain.usecase.people.UpdateAvatarUserEntityUseCase
import com.clearkeep.domain.usecase.people.UpdateStatusUseCase
import com.clearkeep.domain.usecase.server.*
import com.clearkeep.domain.usecase.signalkey.DeleteKeyUseCase
import com.clearkeep.domain.usecase.workspace.GetWorkspaceInfoUseCase
import com.clearkeep.presentation.screen.chat.utils.getLinkFromPeople
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val environment: Environment,
    private val storage: UserPreferencesStorage,
    private val registerTokenUseCase: RegisterTokenUseCase,
    private val fetchGroupsUseCase: FetchGroupsUseCase,
    private val getAllPeerGroupByDomainUseCase: GetAllPeerGroupByDomainUseCase,
    private val clearTempNotesUseCase: ClearTempNotesUseCase,
    private val clearTempMessageUseCase: ClearTempMessageUseCase,
    private val getServerByDomainUseCase: GetServerByDomainUseCase,

    private val getWorkspaceInfoUseCase: GetWorkspaceInfoUseCase,

    private val getListClientStatusUseCase: GetListClientStatusUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
    private val sendPingUseCase: SendPingUseCase,
    private val updateAvatarUserEntityUseCase: UpdateAvatarUserEntityUseCase,

    getDefaultServerProfileAsStateUseCase: GetDefaultServerProfileAsStateUseCase,
    getIsLogoutUseCase: GetIsLogoutUseCase,
    getAllRoomsUseCase: GetAllRoomsUseCase,

    private val deleteKeyUseCase: DeleteKeyUseCase,

    getServersAsStateUseCase: GetServersAsStateUseCase,
    logoutUseCase: LogoutUseCase,
    getActiveServerUseCase: GetActiveServerUseCase,

    private val setActiveServerUseCase: SetActiveServerUseCase
) : BaseViewModel(logoutUseCase) {
    val currentServer = getActiveServerUseCase()
    val servers: LiveData<List<Server>> = getServersAsStateUseCase()

    var profile = getDefaultServerProfileAsStateUseCase()

    val isLogout = getIsLogoutUseCase()

    val selectingJoinServer = MutableLiveData(false)
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    val groups: LiveData<List<ChatGroup>> = getAllRoomsUseCase()

    val isRefreshing = MutableLiveData(false)

    private val _currentStatus = MutableLiveData(UserStatus.ONLINE.value)
    val currentStatus: LiveData<String>
        get() = _currentStatus
    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserInfo: LiveData<List<User>>
        get() = _listUserStatus

    val serverUrlValidateResponse = MutableLiveData<String>()

    private val _isServerUrlValidateLoading = MutableLiveData<Boolean>()
    val isServerUrlValidateLoading: LiveData<Boolean>
        get() = _isServerUrlValidateLoading

    private var checkValidServerJob: Job? = null

    init {
        viewModelScope.launch {
            clearTempNotesUseCase()
            clearTempMessageUseCase()
            fetchGroupsUseCase()
            getStatusUserInDirectGroup()
        }

        sendPing()
    }

    fun onPullToRefresh() {
        isRefreshing.postValue(true)
        viewModelScope.launch {
            fetchGroupsUseCase()
            isRefreshing.postValue(false)
        }
    }


    val chatGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()
        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == UserStateTypeInGroup.ACTIVE.value

            }
        }

        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
            }
        }
        emitSource(result)
    }

    val directGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()

        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            //getStatusUserInDirectGroup()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        emitSource(result)
    }

    private suspend fun getStatusUserInDirectGroup() {
        try {
            val listUserRequest = arrayListOf<User>()
            getAllPeerGroupByDomainUseCase(
                owner = Owner(
                    getDomainOfActiveServer(),
                    getClientIdOfActiveServer()
                )
            )
                .forEach { group ->
                    if (!group.isGroup()) {
                        val user = group.clientList.firstOrNull { client ->
                            client.userId != getClientIdOfActiveServer()
                        }
                        if (user != null) {
                            listUserRequest.add(user)
                        }
                    }
                }
            val listClientStatus = getListClientStatusUseCase(listUserRequest)
            _listUserStatus.postValue(listClientStatus)
            listClientStatus?.forEach {
                currentServer.value?.serverDomain?.let { it1 ->
                    currentServer.value?.ownerClientId?.let { it2 ->
                        Owner(it1, it2)
                    }
                }?.let { it2 -> updateAvatarUserEntityUseCase(it, owner = it2) }
            }
            delay(60 * 1000)
            getStatusUserInDirectGroup()

        } catch (e: Exception) {
            printlnCK("getStatusUserInDirectGroup error: ${e.message}")
        }
    }

    private fun sendPing() {
        viewModelScope.launch {
            delay(60 * 1000)
            sendPingUseCase()
            sendPing()
        }
    }

    fun setUserStatus(status: UserStatus) {
        viewModelScope.launch {
            val result = updateStatusUseCase(status.value)
            if (result) _currentStatus.postValue(status.value)
        }
    }

    fun prepare() {
        viewModelScope.launch {
            _prepareState.value = PrepareProcessing
            updateFirebaseToken()
            _prepareState.value = PrepareSuccess
        }
    }

    fun selectChannel(server: Server) {
        viewModelScope.launch {
            setActiveServerUseCase(server)
            selectingJoinServer.value = false
        }
    }

    fun showJoinServer() {
        selectingJoinServer.value = true
    }

    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    private val _isLogOutProcessing = MutableLiveData<Boolean>()

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                printlnCK("Fetching FCM registration token failed, ${task.exception}")
            }

            // Get new FCM registration token
            val token = task.result
            if (!token.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    storage.setString(FIREBASE_TOKEN, token)
                    pushFireBaseTokenToServer()
                }
            }
        }
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        val token = storage.getString(FIREBASE_TOKEN)
        if (!token.isNullOrEmpty()) {
            registerTokenUseCase(token)
        }
    }

    fun getProfileLink(): String {
        val server = environment.getServer()
        return getLinkFromPeople(
            User(
                userId = server.profile.userId,
                userName = server.profile.userName ?: "",
                domain = server.serverDomain
            )
        )
    }

    fun checkValidServerUrl(url: String) {
        _isServerUrlValidateLoading.value = true
        checkValidServerJob?.cancel()
        checkValidServerJob = viewModelScope.launch {
            if (!isValidServerUrl(url)) {
                printlnCK("checkValidServerUrl local validation invalid")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            if (getServerByDomainUseCase(url) != null) {
                printlnCK("checkValidServerUrl duplicate server")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            val server = environment.getServer()
            val workspaceInfoResponse =
                getWorkspaceInfoUseCase(server.serverDomain, url)
            _isServerUrlValidateLoading.value = false
            if (workspaceInfoResponse.status == Status.ERROR) {
                printlnCK("checkValidServerUrl invalid server from remote")
                serverUrlValidateResponse.value = ""
                return@launch
            }

            serverUrlValidateResponse.value = url
        }
    }

    fun cancelCheckValidServer() {
        _isServerUrlValidateLoading.value = false
        checkValidServerJob?.cancel()
    }

    fun deleteKey() {
        viewModelScope.launch {
            val server = environment.getServer()
            val owner = Owner(server.serverDomain, server.ownerClientId)
            deleteKeyUseCase(owner, currentServer.value!!, chatGroups.value)
        }
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()