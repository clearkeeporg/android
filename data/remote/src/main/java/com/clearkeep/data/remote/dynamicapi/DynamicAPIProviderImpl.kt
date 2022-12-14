package com.clearkeep.data.remote.dynamicapi

import auth.AuthGrpc
import com.clearkeep.data.remote.dynamicapi.channel.ChannelSelector
import com.clearkeep.common.utilities.printlnCK
import group.GroupGrpc
import message.MessageGrpc
import note.NoteGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import upload_file.UploadFileGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import workspace.WorkspaceGrpc
import java.lang.IllegalArgumentException
import javax.inject.Inject

/*
* active server
* */
class DynamicAPIProviderImpl @Inject constructor(
    private val channelSelector: ChannelSelector,
) : DynamicAPIProvider {

    private var server: com.clearkeep.domain.model.Server? = null

    override fun setUpDomain(server: com.clearkeep.domain.model.Server) {
        printlnCK("setUpDomain, domain = ${server.serverDomain}")
        this.server = server
    }

    override fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return SignalKeyDistributionGrpc.newStub(managedChannel)
    }

    override fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return SignalKeyDistributionGrpc.newBlockingStub(managedChannel).withCallCredentials(
            CallCredentials(
                server!!.accessKey,
                server!!.hashKey
            )
        )
    }

    override fun provideNotifyStub(): NotifyGrpc.NotifyStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyGrpc.newStub(managedChannel)
    }

    override fun provideNotifyBlockingStub(): NotifyGrpc.NotifyBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyGrpc.newBlockingStub(managedChannel)
    }

    override fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return AuthGrpc.newBlockingStub(managedChannel)
    }

    override fun provideUserBlockingStub(): UserGrpc.UserBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UserGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideGroupBlockingStub(): GroupGrpc.GroupBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return GroupGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideMessageBlockingStub(): MessageGrpc.MessageBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        printlnCK("provideMessageBlockingStub: ${managedChannel.authority()}")
        return MessageGrpc.newBlockingStub(managedChannel)
    }

    override fun provideMessageStub(): MessageGrpc.MessageStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return MessageGrpc.newStub(managedChannel)
    }

    override fun provideNoteBlockingStub(): NoteGrpc.NoteBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        printlnCK("provideNoteBlockingStub: ${managedChannel.authority()}")
        return NoteGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideNotifyPushBlockingStub(): NotifyPushGrpc.NotifyPushBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyPushGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideVideoCallBlockingStub(): VideoCallGrpc.VideoCallBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return VideoCallGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideUploadFileBlockingStub(): UploadFileGrpc.UploadFileBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UploadFileGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideUploadFileStub(): UploadFileGrpc.UploadFileStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UploadFileGrpc.newStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            )
    }

    override fun provideWorkSpaceBlockingStub(): WorkspaceGrpc.WorkspaceBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return WorkspaceGrpc.newBlockingStub(managedChannel).withCallCredentials(
            CallCredentials(
                server!!.accessKey,
                server!!.hashKey
            )
        )
    }
}