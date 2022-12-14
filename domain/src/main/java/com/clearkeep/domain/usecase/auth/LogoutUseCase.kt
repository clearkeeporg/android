package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.*
import com.clearkeep.common.utilities.RECEIVER_DEVICE_ID
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val environment: Environment,
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val signalKeyRepository: SignalKeyRepository
) {
    suspend operator fun invoke(): Boolean {
        val server = serverRepository.getDefaultServer()
        server?.let {
            val server = environment.getServer()
            val owner = Owner(server.serverDomain, server.ownerClientId)
            val groups = groupRepository.getAllRooms()
            val profile = serverRepository.getDefaultServer()?.profile
            val groupsInServer = groups.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == profile?.userId }?.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value
            }

            deleteKey(owner, server, groupsInServer)

            server.id?.let {
                val removeResult = serverRepository.deleteServer(it)
                groupRepository.deleteGroup(
                    server.serverDomain,
                    server.ownerClientId
                )
                messageRepository.deleteMessageByDomain(
                    server.serverDomain,
                    server.ownerClientId
                )
                if (removeResult > 0) {
                     if (serverRepository.getServers().isNotEmpty()) {
                        val firstServer = serverRepository.getServers()[0]
                        serverRepository.setActiveServer(firstServer)
                         return true
                    }
                }
            }
        }
        return true
    }

    private suspend fun deleteKey(owner: Owner, server: com.clearkeep.domain.model.Server, chatGroups: List<com.clearkeep.domain.model.ChatGroup>?) {
        val (domain, clientId) = owner

        signalKeyRepository.deleteIdentityKeyByOwnerDomain(domain = domain, clientId = clientId)
        signalKeyRepository.deleteSenderPreKey(domain = server.serverDomain, clientId = server.ownerClientId)

        chatGroups?.forEach { group ->
            val receiverAddress = CKSignalProtocolAddress(
                Owner(server.serverDomain, server.ownerClientId),
                group.groupId,
                RECEIVER_DEVICE_ID
            )
            signalKeyRepository.deleteGroupSenderKey(receiverAddress)
            group.clientList.forEach {
                val senderAddress = CKSignalProtocolAddress(
                    Owner(
                        it.domain,
                        it.userId
                    ), group.groupId,SENDER_DEVICE_ID
                )
                signalKeyRepository.deleteGroupSenderKey(group.groupId.toString(), senderAddress.name)
            }
        }
    }
}