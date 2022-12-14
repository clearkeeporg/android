package com.clearkeep.domain.usecase.group

import com.clearkeep.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupsByDomainUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    operator fun invoke(ownerDomain: String, ownerClientId: String) = groupRepository.getGroupsByDomain(ownerDomain, ownerClientId)
}

class GetAllGroupsByDomainUseCase @Inject constructor(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(ownerDomain: String, ownerClientId: String) = groupRepository.getAllGroupByDomain(ownerDomain, ownerClientId)
}