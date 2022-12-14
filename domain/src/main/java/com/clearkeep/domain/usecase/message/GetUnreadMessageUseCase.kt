package com.clearkeep.domain.usecase.message

import com.clearkeep.domain.model.Message
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetUnreadMessageUseCase @Inject constructor(private val messageRepository: MessageRepository, private val groupRepository: GroupRepository, private val serverRepository: ServerRepository) {
    suspend operator fun invoke(
        groupId: Long,
        domain: String,
        ourClientId: String
    ): List<Message> = withContext(Dispatchers.IO) {
        val server = serverRepository.getServer(domain, ourClientId)
        val group = groupRepository.getGroupByID(groupId, domain, ourClientId, server, false)
        return@withContext messageRepository.getUnreadMessage(
            groupId,
            group.data?.lastMessageSyncTimestamp ?: 0L,
            domain,
            ourClientId
        ).dropWhile { it.senderId == ourClientId }
    }
}