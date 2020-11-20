package com.clearkeep.screen.chat.repositories

import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.initSessionUserInGroup
import com.clearkeep.screen.chat.utils.initSessionUserPeer
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.repository.ProfileRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import signal.Signal
import signal.SignalKeyDistributionGrpc
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
        private val client: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
        private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,

        private val senderKeyStore: InMemorySenderKeyStore,
        private val signalProtocolStore: InMemorySignalProtocolStore,

        private val messageRepository: MessageRepository,
        private val roomRepository: GroupRepository,
        private val userRepository: ProfileRepository
) {
    init {
        subscribe()
    }

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    fun getClientId() = userRepository.getClientId()

    fun getMessagesFromRoom(groupId: String) = messageRepository.getMessages(groupId = groupId)

    suspend fun sendMessageInPeer(receiverId: String, groupId: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageInPeer: $receiverId")
        val senderId = getClientId()
        try {
            val signalProtocolAddress = SignalProtocolAddress(receiverId, 111)

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(receiverId, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return@withContext false
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                    sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))

            val request = Signal.PublishRequest.newBuilder()
                    .setClientId(receiverId)
                    .setFromClientId(senderId)
                    .setGroupId(groupId)
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            clientBlocking.publish(request)
            insertNewMessage(groupId, senderId, msg)

            printlnCK("send message success: $msg")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun sendMessageToGroup(groupId: String, msg: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageToGroup: $groupId")
        val senderId = getClientId()
        try {
            val senderAddress = SignalProtocolAddress(senderId, 111)
            val groupSender  =  SenderKeyName(groupId, senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(msg.toByteArray(charset("UTF-8")))
            val messageAfterEncrypted = String(ciphertextFromAlice, StandardCharsets.UTF_8)

            val request = Signal.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.name)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            clientBlocking.publish(request)
            insertNewMessage(groupId, senderId, msg)

            printlnCK("send message success: $msg, encrypted: $messageAfterEncrypted")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

    private fun subscribe() {
        val request = Signal.SubscribeAndListenRequest.newBuilder()
                .setClientId(getClientId())
                .build()

        client.subscribe(request, object : StreamObserver<Signal.BaseResponse> {
            override fun onNext(response: Signal.BaseResponse?) {
                printlnCK("subscribe onNext ${response?.message}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
                printlnCK("subscribe onCompleted")
                listen()
            }
        })
    }

    private fun listen() {
        val request = Signal.SubscribeAndListenRequest.newBuilder()
            .setClientId(getClientId())
            .build()
        client.listen(request, object : StreamObserver<Signal.Publication> {
            override fun onNext(value: Signal.Publication) {
                printlnCK("Receive a message from : ${value.fromClientId}" +
                        ", groupId = ${value.groupId} groupType = ${value.groupType}")
                scope.launch {
                    // TODO
                    if (!isGroup(value.groupType)) {
                        decryptMessageFromPeer(value)
                    } else {
                        decryptMessageFromGroup(value)
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen message error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    private suspend fun decryptMessageFromPeer(value: Signal.Publication) {
        try {
            val senderId = value.fromClientId
            val signalProtocolAddress = SignalProtocolAddress(senderId, 111)
            val preKeyMessage = PreKeySignalMessage(value.message.toByteArray())

            if (!signalProtocolStore.containsSession(signalProtocolAddress)) {
                val initSuccess = initSessionUserPeer(senderId, signalProtocolAddress, clientBlocking, signalProtocolStore)
                if (!initSuccess) {
                    return
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message = sessionCipher.decrypt(preKeyMessage)
            val result = String(message, StandardCharsets.UTF_8)
            printlnCK("decryptMessageFromPeer: $result")

            insertNewMessage(value.groupId, senderId, result)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromPeer error : $e")
        }
    }

    // Group
    private suspend fun decryptMessageFromGroup(value: Signal.Publication) {
        try {
            val senderAddress = SignalProtocolAddress(value.fromClientId, 111)
            val groupSender = SenderKeyName(value.groupId, senderAddress)
            val bobGroupCipher = GroupCipher(senderKeyStore, groupSender)

            val initSession = initSessionUserInGroup(value, groupSender, clientBlocking, senderKeyStore)
            if (!initSession) {
                return
            }

            val plaintextFromAlice = bobGroupCipher.decrypt(value.message.toByteArray())
            val result = String(plaintextFromAlice, StandardCharsets.UTF_8)
            printlnCK("decryptMessageFromGroup: $result")

            insertNewMessage(value.groupId, value.fromClientId, result)
        } catch (e: Exception) {
            printlnCK("decryptMessageFromGroup error : $e")
        }
    }

    private suspend fun insertNewMessage(groupId: String, senderId: String, message: String) {
        var room: ChatGroup? = roomRepository.getGroupByID(groupId)
        if (room == null) {
            room = roomRepository.getGroupFromAPI(groupId)
            if (room != null) {
                roomRepository.insertGroup(room)
            }
        }

        if (room == null) {
            return
        }

        val createTime = getCurrentDateTime().time
        messageRepository.insert(Message(senderId, message, room.id, createTime))

        // update last message in room
        val updateRoom = ChatGroup(
                id = room.id,
                groupName = room.groupName,
                groupAvatar = room.groupAvatar,
                groupType = room.groupType,
                createBy = room.createBy,
                createdAt = room.createdAt,
                updateBy = room.updateBy,
                updateAt = room.updateAt,
                clientList = room.clientList,

                // update
                isJoined = true,

                lastClient = senderId,
                lastMessage = message,
                lastUpdatedTime = createTime
        )
        roomRepository.updateRoom(updateRoom)
    }
}