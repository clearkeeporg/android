package com.clearkeep.data.remote.service

import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.domain.model.Server
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import signal.Signal
import javax.inject.Inject

class SignalKeyDistributionService @Inject constructor(
    private val paramAPIProvider: ParamAPIProvider,
) {
    suspend fun groupRegisterClientKey(
        server: Server, deviceId: Int, groupId: Long, clientKeyDistribution: SenderKeyDistributionMessage, encryptedGroupPrivateKey: ByteArray?,
        key: ECKeyPair?, senderKeyId: Int?, senderKey: ByteArray?
    ) = withContext(Dispatchers.IO) {
        val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)

        val request = Signal.GroupRegisterClientKeyRequest.newBuilder()
            .setDeviceId(deviceId)
            .setGroupId(groupId)
            .setClientKeyDistribution(ByteString.copyFrom(clientKeyDistribution.serialize()))
            .setPrivateKey(DecryptsPBKDF2.toHex(encryptedGroupPrivateKey!!))
            .setPublicKey(ByteString.copyFrom(key?.publicKey?.serialize()))
            .setSenderKeyId(senderKeyId?.toLong() ?: 0L)
            .setSenderKey(ByteString.copyFrom(senderKey))
            .build()

        return@withContext paramAPIProvider.provideSignalKeyDistributionBlockingStub(paramAPI)
            .groupRegisterClientKey(request)
    }

    suspend fun getPeerClientKey(server: Server, clientId: String, domain: String): Signal.PeerGetClientKeyResponse = withContext(Dispatchers.IO) {
        val requestUser = Signal.PeerGetClientKeyRequest.newBuilder()
            .setClientId(clientId)
            .setWorkspaceDomain(domain)
            .build()

        return@withContext paramAPIProvider.provideSignalKeyDistributionBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        ).peerGetClientKey(requestUser)
    }

    suspend fun getGroupClientKey(server: Server, groupId: Long, clientId: String): Signal.GroupGetClientKeyResponse = withContext(Dispatchers.IO) {
        val signalGrpc = paramAPIProvider.provideSignalKeyDistributionBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        val request = Signal.GroupGetClientKeyRequest.newBuilder()
            .setGroupId(groupId)
            .setClientId(clientId)
            .build()
        return@withContext signalGrpc.groupGetClientKey(request)
    }

    suspend fun updateGroupSenderKey(server: Server, arrayList: ArrayList<Pair<Long, ByteArray>>): Signal.BaseResponse = withContext(Dispatchers.IO) {
        val signalGrpc = paramAPIProvider.provideSignalKeyDistributionBlockingStub(
            ParamAPI(
                server.serverDomain,
                server.accessKey,
                server.hashKey
            )
        )
        val listGroupClientKey = arrayListOf<Signal.GroupRegisterClientKeyRequest>()
        arrayList.forEach {
            val itemRequest = Signal.GroupRegisterClientKeyRequest.newBuilder()
                .setGroupId(it.first)
                .setSenderKey(ByteString.copyFrom(it.second))
                .build()
            listGroupClientKey.add(itemRequest)
        }
        val request = Signal.GroupUpdateClientKeyRequest
            .newBuilder()
            .addAllListGroupClientKey(listGroupClientKey)
            .build()
        return@withContext signalGrpc.groupUpdateClientKey(request)

    }
}