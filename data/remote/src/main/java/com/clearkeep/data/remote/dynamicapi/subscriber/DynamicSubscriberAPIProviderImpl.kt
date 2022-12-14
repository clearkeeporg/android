package com.clearkeep.data.remote.dynamicapi.subscriber

import com.clearkeep.data.remote.dynamicapi.channel.ManagedChannelImpl
import io.grpc.ManagedChannel
import message.MessageGrpc
import notification.NotifyGrpc
import java.lang.IllegalArgumentException
import java.util.HashMap
import javax.inject.Inject

class DynamicSubscriberAPIProviderImpl @Inject constructor() : DynamicSubscriberAPIProvider {
    private val mChannelMap = HashMap<String, ManagedChannel>()

    override fun provideNotifyStub(domain: String): NotifyGrpc.NotifyStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }

        val managedChannel = createNewChannel(domain)

        return NotifyGrpc.newStub(managedChannel)
    }

    override fun provideNotifyBlockingStub(domain: String): NotifyGrpc.NotifyBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = createNewChannel(domain)
        return NotifyGrpc.newBlockingStub(managedChannel)
    }

    override fun provideMessageBlockingStub(domain: String): MessageGrpc.MessageBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }

        val managedChannel = createNewChannel(domain)
        return MessageGrpc.newBlockingStub(managedChannel)
    }

    override fun provideMessageStub(domain: String): MessageGrpc.MessageStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }

        val managedChannel = createNewChannel(domain)
        return MessageGrpc.newStub(managedChannel)
    }

    override fun shutDownAll() {
        mChannelMap.values.forEach { u -> u.shutdownNow() }
        mChannelMap.clear()
    }

    private fun createNewChannel(domain: String): ManagedChannel {
        return if (mChannelMap[domain] != null)
            mChannelMap[domain]!!
        else {
            val newChannel = ManagedChannelImpl().createManagedChannel(domain)
            mChannelMap[domain] = newChannel
            newChannel
        }
    }
}