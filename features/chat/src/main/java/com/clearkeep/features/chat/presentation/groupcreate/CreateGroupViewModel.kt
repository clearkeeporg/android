package com.clearkeep.features.chat.presentation.groupcreate

import androidx.lifecycle.*
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.group.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val environment: Environment
) : ViewModel() {
    var groupId: Long = -1

    val invitedFriends: MutableList<User> = mutableListOf()

    private val _createGroupState = MutableLiveData<CreateGroupState>()

    val createGroupState: LiveData<CreateGroupState>
        get() = _createGroupState

    fun setFriendsList(friends: List<User>) {
        invitedFriends.clear()
        invitedFriends.addAll(friends)
    }

    fun createGroup(groupName: String, onError: (() -> Unit)? = null) {
        viewModelScope.launch {
            _createGroupState.value = CreateGroupProcessing
            val server = environment.getServer()
            // clone invited list and add me to list
            val list = mutableListOf<User>()
            list.addAll(invitedFriends)
            list.add(
                User(
                    userId = server.profile.userId,
                    userName = server.profile.userName ?: "",
                    domain = server.serverDomain,
                    server.profile.userName,
                    phoneNumber = server.profile.phoneNumber,
                    avatar = server.profile.avatar,
                    email = server.profile.email
                )
            )
            val res = createGroupUseCase(server.profile.userId, groupName, list, true)
            if (res?.data != null) {
                groupId = res.data!!.groupId
                _createGroupState.value = CreateGroupSuccess
            } else {
                onError?.invoke()
                _createGroupState.value = CreateGroupError
            }
        }
    }
}

sealed class CreateGroupState
object CreateGroupSuccess : CreateGroupState()
object CreateGroupError : CreateGroupState()
object CreateGroupProcessing : CreateGroupState()
