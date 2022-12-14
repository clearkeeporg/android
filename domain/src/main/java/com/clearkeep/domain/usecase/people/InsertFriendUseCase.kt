package com.clearkeep.domain.usecase.people

import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.PeopleRepository
import javax.inject.Inject

class InsertFriendUseCase @Inject constructor(private val peopleRepository: PeopleRepository) {
    suspend operator fun invoke(friend: User, owner: Owner) = peopleRepository.insertFriend(friend, owner)
}