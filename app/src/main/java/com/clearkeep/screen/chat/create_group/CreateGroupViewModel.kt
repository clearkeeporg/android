package com.clearkeep.screen.chat.create_group

import androidx.lifecycle.*
import com.clearkeep.db.model.People
import com.clearkeep.repository.utils.Resource
import com.clearkeep.screen.chat.main.people.PeopleRepository
import com.clearkeep.screen.chat.repositories.GroupRepository
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateGroupViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val userManager: UserManager,
    private val groupRepository: GroupRepository
): ViewModel() {
        private val invitedFriends: MutableList<String> = mutableListOf()

        private val _createGroupState = MutableLiveData<CreateGroupState>()

        val createGroupState: LiveData<CreateGroupState>
                get() = _createGroupState

        val selectedFriends: List<People> = emptyList()

        fun getClientId() = userManager.getClientId()

        val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emitSource(peopleRepository.getFriends(getClientId()))
        }

        fun setFriendsList(friends: List<People>) {
                invitedFriends.clear()
                invitedFriends.add(getClientId())
                invitedFriends.addAll(friends.map { it.id })
        }

        fun createGroup(groupName: String) {
                viewModelScope.launch {
                        _createGroupState.value = CreateGroupProcessing
                        val invitedFriendsAsString = invitedFriends
                        if (groupRepository.createGroupFromAPI(getClientId(), groupName, invitedFriendsAsString, true) != null) {
                                _createGroupState.value = CreateGroupSuccess
                        } else {
                                _createGroupState.value = CreateGroupError
                        }
                }
        }
}

sealed class CreateGroupState
object CreateGroupSuccess : CreateGroupState()
object CreateGroupError : CreateGroupState()
object CreateGroupProcessing : CreateGroupState()
