package com.clearkeep.domain.usecase.chat

import android.text.TextUtils
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.utilities.SENDER_DEVICE_ID
import com.clearkeep.utilities.printlnCK
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.lang.Exception
import javax.inject.Inject

class ProcessPeerKeyUseCase @Inject constructor(
    private val serverRepository: ServerRepository,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val signalKeyDistributionService: SignalKeyDistributionService
) {
    suspend operator fun invoke(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ): Boolean {
        val signalProtocolAddress =
            CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), SENDER_DEVICE_ID)
        val signalProtocolAddress2 =
            CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
        initSessionUserPeer(
            signalProtocolAddress2,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
        return initSessionUserPeer(
            signalProtocolAddress,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
    }

    private suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        signalProtocolStore: InMemorySignalProtocolStore,
        owner: Owner
    ): Boolean = withContext(Dispatchers.IO) {
        val remoteClientId = signalProtocolAddress.owner.clientId
        printlnCK("initSessionUserPeer with $remoteClientId, domain = ${signalProtocolAddress.owner.domain}")
        if (TextUtils.isEmpty(remoteClientId)) {
            return@withContext false
        }
        try {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserPeer: server must be not null")
                return@withContext false
            }

            val remoteKeyBundle = signalKeyDistributionService.getPeerClientKey(
                server,
                remoteClientId,
                signalProtocolAddress.owner.domain
            )

            val preKey = PreKeyRecord(remoteKeyBundle.preKey.toByteArray())
            val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey.toByteArray())
            val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic.toByteArray(), 0)

            val retrievedPreKey = PreKeyBundle(
                remoteKeyBundle.registrationId,
                signalProtocolAddress.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                remoteKeyBundle.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)
            printlnCK("initSessionUserPeer: success")
            return@withContext true
        } catch (e: StatusRuntimeException) {
            printlnCK("initSessionUserPeer: $e")
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("initSessionUserPeer: $e")
        }

        return@withContext false
    }
}