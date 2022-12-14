package com.clearkeep.data.remote.dynamicapi

import auth.AuthGrpc
import group.GroupGrpc
import message.MessageGrpc
import note.NoteGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import workspace.WorkspaceGrpc

interface ParamAPIProvider {
    fun provideSignalKeyDistributionGrpc(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionStub

    fun provideSignalKeyDistributionBlockingStub(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub

    fun provideNotifyStub(paramAPI: ParamAPI): NotifyGrpc.NotifyStub

    fun provideNotifyBlockingStub(paramAPI: ParamAPI): NotifyGrpc.NotifyBlockingStub

    fun provideAuthBlockingStub(paramAPI: ParamAPI): AuthGrpc.AuthBlockingStub

    fun provideUserBlockingStub(paramAPI: ParamAPI): UserGrpc.UserBlockingStub

    fun provideGroupBlockingStub(paramAPI: ParamAPI): GroupGrpc.GroupBlockingStub

    fun provideMessageBlockingStub(paramAPI: ParamAPI): MessageGrpc.MessageBlockingStub

    fun provideNotesBlockingStub(paramAPI: ParamAPI): NoteGrpc.NoteBlockingStub

    fun provideMessageStub(paramAPI: ParamAPI): MessageGrpc.MessageStub

    fun provideNotifyPushBlockingStub(paramAPI: ParamAPI): NotifyPushGrpc.NotifyPushBlockingStub

    fun provideVideoCallBlockingStub(paramAPI: ParamAPI): VideoCallGrpc.VideoCallBlockingStub

    fun provideWorkspaceBlockingStub(paramAPI: ParamAPI): WorkspaceGrpc.WorkspaceBlockingStub
}

class ParamAPI(
    val serverDomain: String,
    val accessKey: String? = null,
    val hashKey: String? = null,
)