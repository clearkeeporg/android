package com.clearkeep.screen.chat.main.people

import androidx.lifecycle.*
import com.clearkeep.db.model.People
import com.clearkeep.repository.utils.Resource
import com.clearkeep.utilities.UserManager
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class PeopleViewModel @Inject constructor(
        private val peopleRepository: PeopleRepository,
        private val userManager: UserManager
): ViewModel() {
    fun getClientId() = userManager.getClientId()

    val friends: LiveData<Resource<List<People>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emitSource(peopleRepository.getFriends(getClientId()))
    }
}
