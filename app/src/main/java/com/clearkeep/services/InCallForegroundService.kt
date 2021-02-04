package com.clearkeep.services

/*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.opengl.EGLContext
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.clearkeep.R
import com.clearkeep.screen.repo.VideoCallRepository
import com.clearkeep.utilities.*
import computician.janusclientapi.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.MediaStream
import org.webrtc.PeerConnection.IceServer
import org.webrtc.VideoTrack
import java.math.BigInteger
import java.util.*
import javax.inject.Inject


enum class CallState {
    CALLING,
    RINGING,
    ANSWERED,
    BUSY,
    ENDED,
    CALL_NOT_READY
}

@AndroidEntryPoint
class InCallForegroundService : Service() {
    interface CallListener {
        fun onCallStateChanged(status: String, state: CallState)
        fun onLocalStream()
        fun onRemoteStreamAdd(remoteTrack: VideoTrack, remoteClientId: BigInteger)
        fun onStreamRemoved(remoteClientId: BigInteger)
    }

    inner class LocalBinder : Binder() {
        val service: InCallForegroundService
            get() = this@InCallForegroundService
    }

    @Inject
    lateinit var videoCallRepository: VideoCallRepository
    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private val mBinder: IBinder = LocalBinder()
    private var mIsInComingCall = false
    private lateinit var mGroupToken: String
    private var mAvatarInConversation: String? = null
    private var mUserNameInConversation: String? = null
    private var mIsSpeakerOn = false
    private var mIsMuting = false
    private var mIsMutingVideo = false
    private var mLastTimeConnectedCall: Long = -1

    private var mGroupId: Long = -1
    private lateinit var mOurClientId: String
    private lateinit var mCurrentCallStatus: String
    private lateinit var mCurrentCallState: CallState

    // janus
    private val remoteStreams: MutableMap<BigInteger, VideoTrack> = HashMap()
    private var janusServer: JanusServer? = null
    private val mListeners: ArrayList<CallListener> = ArrayList()
    private var localStream: MediaStream? = null
    private var localTrack: VideoTrack? = null
    private var handle: JanusPluginHandle? = null
    var lastTimeStampOfUpdateBitrate: Long = 0

    private val mEndCallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mCurrentCallState != CallState.ENDED) {
                mCurrentCallState == CallState.ENDED
                notifyListeners()
            }
            hangup()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        mCurrentCallStatus = getString(R.string.text_calling)
        mCurrentCallState = CallState.CALLING
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_END_CALL)
        registerReceiver(mEndCallReceiver, intentFilter)

        setSpeakerphoneOn(false)
        muteMic(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        scope.cancel()
        unregisterReceiver(mEndCallReceiver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        mGroupToken = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
        mIsInComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        mAvatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        mGroupId = intent.getLongExtra(EXTRA_GROUP_ID, -1)
        */
/*mGroupId = java.lang.Long.valueOf(1234)*//*

        mOurClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID) ?: ""
        notifyStartForeground(getString(R.string.text_notification_calling), mCurrentCallStatus)
        return START_NOT_STICKY
    }

    fun startCall(con: EGLContext) {
        if (mIsInComingCall) {
            answer(con)
        } else {
            scope.launch {
                makeCall(con)

                delay(CALL_WAIT_TIME_OUT)
                if (remoteStreams.isEmpty()) {
                    updateCallingState(CallState.CALL_NOT_READY)
                }
            }
        }
    }

    private suspend fun makeCall(con: EGLContext) {
        Log.i(TAG, "makeCall, groupID = $mGroupId, our client id = $mOurClientId, token = $mGroupToken")
        if (janusServer != null) {
            return
        }
        val success = videoCallRepository.requestVideoCall(mGroupId)
        if (success) {
            janusServer = JanusServer(JanusGatewayCallbacksImpl())
            janusServer!!.initializeMediaContext(this, true, true, true, con)
            janusServer!!.Connect()
        } else {
            delay(5 * 1000)
            updateCallingState(CallState.CALL_NOT_READY)
        }
    }

    private fun answer(con: EGLContext) {
        Log.i(TAG, "answer, groupID = $mGroupId, our client id = $mOurClientId, token = $mGroupToken")
        if (janusServer != null) {
            return
        }
        janusServer = JanusServer(JanusGatewayCallbacksImpl())
        janusServer!!.initializeMediaContext(this, true, true, true, con)
        janusServer!!.Connect()
    }

    fun setSpeakerphoneOn(isOn: Boolean) {
        Log.i(TAG, "setSpeakerphoneOn, isOn = $isOn")
        mIsSpeakerOn = isOn
        try {
            val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = isOn
        } catch (e: Exception) {}
    }

    fun muteMic(isMute: Boolean) {
        Log.i(TAG, "mute, isMute = $isMute")
        mIsMuting = isMute
        try {
            localStream?.audioTracks?.get(0)?.setEnabled(!mIsMuting)
        } catch (e: Exception) {}
    }

    fun muteVideo(isMute: Boolean) {
        Log.i(TAG, "muteVideo, isMute = $isMute")
        mIsMutingVideo = isMute
        try {
            localStream?.videoTracks?.get(0)?.setEnabled(!mIsMutingVideo)
        } catch (e: Exception) {}
    }

    fun switchCamera() {
        Log.i(TAG, "switchCamera")
        try {
            handle?.switchCamera()
        } catch (e: Exception) {}
    }

    fun hangup() {
        Log.i(TAG, "hangup")
        sendEndServiceBroadcast()
        janusServer?.Destroy()
        stopSelf()
    }

    fun registerCallListener(listener: CallListener) {
        mListeners.add(listener)
        notifyListeners()
    }

    fun unregisterCallListener(listener: CallListener) {
        mListeners.remove(listener)
    }

    private fun notifyListeners() {
        if (mListeners.isNotEmpty()) {
            for (callListener in mListeners) {
                callListener.onCallStateChanged(mCurrentCallStatus, mCurrentCallState)
            }
        }
    }

    fun isSpeakerOn(): Boolean {
        return mIsSpeakerOn
    }

    fun isMicMuting(): Boolean {
        return mIsMuting
    }

    fun isVideoMuting(): Boolean {
        return mIsMuting
    }

    fun getCurrentState(): CallState {
        return mCurrentCallState
    }

    fun getLastTimeConnectedCall(): Long {
        return mLastTimeConnectedCall
    }

    fun getRemoteStreams() : Map<BigInteger, VideoTrack> {
        return remoteStreams
    }

    fun getLocalTrack() : VideoTrack? {
        return localTrack
    }

    private fun sendEndServiceBroadcast() {
        val intent = Intent(ACTION_CALL_SERVICE_AVAILABLE_STATE_CHANGED)
        intent.putExtra(EXTRA_SERVICE_IS_AVAILABLE, false)
        sendBroadcast(intent)
    }

    private fun updateCallingState(signalingState: CallState) {
        mCurrentCallState = signalingState
        mCurrentCallStatus = getStatusFromState(mCurrentCallState)
        Log.d(TAG, "updateCallingState: status=" + mCurrentCallStatus + ", state: " + mCurrentCallState.name)

        if (mCurrentCallState == CallState.ANSWERED) {
            mLastTimeConnectedCall = SystemClock.elapsedRealtime()
        }

        notifyStartForeground(getString(R.string.text_notification_calling), mCurrentCallStatus)
        notifyListeners()
        if (mCurrentCallState == CallState.BUSY
            || mCurrentCallState == CallState.ENDED
            ||  mCurrentCallState == CallState.CALL_NOT_READY
        ) {
            hangup()
        }
    }

    private fun getStatusFromState(mCurrentCallState: CallState): String {
        var ret = ""
        when (mCurrentCallState) {
            CallState.CALLING -> {
                ret = getString(R.string.text_calling)
            }
            CallState.RINGING -> {
                ret = getString(R.string.text_ringing)
            }
            CallState.ANSWERED -> {
                ret = getString(R.string.text_started)
            }
            CallState.BUSY -> {
                ret = getString(R.string.text_busy)
            }
            CallState.ENDED -> {
                ret = getString(R.string.text_end)
            }
            CallState.CALL_NOT_READY -> {
                ret = getString(R.string.text_end)
            }
        }
        return ret
    }

    private fun notifyStartForeground(title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    CHANNEL_ID,
                    "channel_voice", NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent()
        notificationIntent.putExtra("is_from_notification", true)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        */
/*notificationIntent.putExtra(EXTRA_FROM_IN_COMING_CALL, mIsInComingCall)
        notificationIntent.putExtra(EXTRA_USER_NAME, mUserNameInConversation)*//*

        notificationIntent.putExtra(
                EXTRA_AVATAR_USER_IN_CONVERSATION,
                mAvatarInConversation
        )
        notificationIntent.putExtra(EXTRA_GROUP_ID, mGroupId)
        notificationIntent.putExtra(EXTRA_OUR_CLIENT_ID, mOurClientId)
        notificationIntent.setClassName(packageName, com.clearkeep.screen.videojanus.InCallActivity::class.java.name)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val pi = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_END_CALL), 0
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notify_small)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_close_call, "End Call", pi)
                .setShowWhen(false)
                .setAutoCancel(false)
        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun handleOnRemoteStreamAdded(remoteClientId: BigInteger, remoteStream: MediaStream) {
        Log.i(TAG, "handleOnRemoteStreamAdded, remoteClientId = $remoteClientId")
        remoteStreams[remoteClientId] = remoteStream.videoTracks[0]

        if (mCurrentCallState != CallState.ANSWERED) {
            updateCallingState(CallState.ANSWERED)
        }
        if (mListeners.isNotEmpty()) {
            for (callListener in mListeners) {
                callListener.onRemoteStreamAdd(remoteStream.videoTracks[0], remoteClientId)
            }
        }
    }

    private fun handleOnRemoteStreamRemoved(remoteClientId: BigInteger) {
        Log.i(TAG, "handleOnRemoteStreamRemoved, remoteClientId = $remoteClientId")
        remoteStreams.remove(remoteClientId)

        if (remoteStreams.isEmpty()) {
            updateCallingState(CallState.ENDED)
        }
        if (mListeners.isNotEmpty()) {
            for (callListener in mListeners) {
                callListener.onStreamRemoved(remoteClientId)
            }
        }
    }

    private fun handleOnLocalStreamConnected(localStream: MediaStream) {
        this.localStream = localStream
        localTrack = localStream.videoTracks[0]
        if (mListeners.isNotEmpty()) {
            for (callListener in mListeners) {
                callListener.onLocalStream()
            }
        }
    }

    internal inner class ListenerAttachCallbacks(
            private val groupId: Long?,
            private val mRemoteClientId: BigInteger
    ) : IJanusPluginCallbacks {
        private var listenerHandle
        : JanusPluginHandle? = null
        override fun success(handle: JanusPluginHandle) {
            listenerHandle = handle
            try {
                val body = JSONObject()
                val msg = JSONObject()
                body.put(REQUEST, "join")
                body.put("room", groupId)
                body.put("ptype", "subscriber")
                body.put("feed", mRemoteClientId)
                msg.put(MESSAGE, body)
                handle.sendMessage(PluginHandleSendMessageCallbacks(msg))
            } catch (ex: Exception) {
                Log.e(TAG, ex.toString())
            }
        }

        override fun onMessage(msg: JSONObject, jsep: JSONObject?) {
            try {
                val event = msg.getString("videoroom")
                Log.i(TAG, "ListenerAttachCallbacks#event = $event, message = $msg")
                if (event == "attached" && jsep != null) {
                    listenerHandle?.createAnswer(object : IPluginHandleWebRTCCallbacks {
                        override fun onSuccess(obj: JSONObject) {
                            try {
                                val mymsg = JSONObject()
                                val body = JSONObject()
                                body.put(REQUEST, "start")
                                body.put("room", groupId)
                                mymsg.put(MESSAGE, body)
                                mymsg.put("jsep", obj)
                                listenerHandle?.sendMessage(PluginHandleSendMessageCallbacks(mymsg))
                            } catch (ex: Exception) {
                            }
                        }

                        override fun getJsep(): JSONObject {
                            return jsep
                        }

                        override fun getMedia(): JanusMediaConstraints {
                            val cons = JanusMediaConstraints()
                            cons.video = null
                            cons.recvAudio = true
                            cons.recvVideo = true
                            cons.sendAudio = false
                            return cons
                        }

                        override fun getTrickle(): Boolean {
                            return true
                        }

                        override fun onCallbackError(error: String) {}
                    })
                }
            } catch (ex: Exception) {
                Log.e(TAG, ex.toString())
            }
        }

        override fun onLocalStream(stream: MediaStream) {}

        override fun onRemoteStream(stream: MediaStream) {
            handleOnRemoteStreamAdded(mRemoteClientId, stream)
        }

        override fun onDataOpen(data: Any) {}
        override fun onData(data: Any) {}
        override fun onCleanup() {}
        override fun onDetached() {}
        override fun getPlugin(): JanusSupportedPluginPackages {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM
        }

        override fun onCallbackError(error: String) {}
    }

    inner class JanusPublisherPluginCallbacks : IJanusPluginCallbacks {
        override fun success(janusPluginHandle: JanusPluginHandle) {
            Log.i(TAG, "JanusPublisherPluginCallbacks, success")
            handle = janusPluginHandle
            joinRoomAsPublisher()
        }

        override fun onMessage(msg: JSONObject, jsep: JSONObject?) {
            Log.i(TAG, "JanusPublisherPluginCallbacks, onMessage: $msg")
            try {
                val event = msg.getString("videoroom")
                if (event == "joined") {
                    createOffer()
                    if (msg.has(PUBLISHERS)) {
                        val pubs = msg.getJSONArray(PUBLISHERS)
                        for (i in 0 until pubs.length()) {
                            val pub = pubs.getJSONObject(i)
                            val remoteId = BigInteger(pub.getString("id"))
                            attachRemoteClient(remoteId)
                        }
                    }
                } else if (event == "hangup") {
                    Log.e(TAG, "JanusPublisherPluginCallbacks, onMessage: hangup")
                    hangup()
                } else if (event == "event") {
                    when {
                        msg.has(PUBLISHERS) -> {
                            val pubs = msg.getJSONArray(PUBLISHERS)
                            for (i in 0 until pubs.length()) {
                                val pub = pubs.getJSONObject(i)
                                attachRemoteClient(BigInteger(pub.getString("id")))
                            }
                        }
                        msg.has("leaving") -> {
                            val id = BigInteger(msg.getString("leaving"))
                            handleOnRemoteStreamRemoved(id)
                        }
                        msg.has("unpublished") -> {
                            val id = BigInteger(msg.getString("unpublished"))
                            handleOnRemoteStreamRemoved(id)
                        }
                        else -> {
                            //todo error
                        }
                    }
                } else if (event == "slow_link" && msg.has("current-bitrate")) {
                    val currentBitrate: Int = Integer.parseInt(msg.getString("current-bitrate"))
                    */
/*configBitrate(currentBitrate + BITRATE_STEP)*//*

                }
                if (jsep != null) {
                    handle?.handleRemoteJsep(PluginHandleWebRTCCallbacks(null, jsep, false))
                }
            } catch (ex: Exception) {
                Log.e(TAG, "JanusPublisherPluginCallbacks, onMessage: error =$ex")
            }
        }

        override fun onLocalStream(stream: MediaStream) {
            Log.i(TAG, "JanusPublisherPluginCallbacks, onLocalStream")
            handleOnLocalStreamConnected(stream)
        }

        override fun onRemoteStream(stream: MediaStream) {
            Log.i(TAG, "JanusPublisherPluginCallbacks, onRemoteStream")
        }

        override fun onDataOpen(data: Any) {}
        override fun onData(data: Any) {}
        override fun onCleanup() {
            Log.i(TAG, "JanusPublisherPluginCallbacks, onCleanup")
        }

        override fun onDetached() {
            Log.i(TAG, "JanusPublisherPluginCallbacks, onDetached")
        }

        override fun getPlugin(): JanusSupportedPluginPackages {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM
        }

        override fun onCallbackError(error: String) {
            Log.w(TAG, "JanusPublisherPluginCallbacks, onCallbackError: $error")
        }

        private fun createOffer() {
            handle?.createOffer(object : IPluginHandleWebRTCCallbacks {
                override fun onSuccess(obj: JSONObject) {
                    try {
                        val msg = JSONObject()
                        val body = JSONObject()
                        body.put(REQUEST, "configure")
                        body.put("audio", true)
                        body.put("video", true)
                        body.put("bitrate", 800 * 1000)
                        msg.put(MESSAGE, body)
                        msg.put("jsep", obj)
                        handle?.sendMessage(PluginHandleSendMessageCallbacks(msg))
                    } catch (ex: Exception) {
                        Log.e(TAG, "createOffer: $ex")
                    }
                }

                override fun getJsep(): JSONObject? {
                    return null
                }

                override fun getMedia(): JanusMediaConstraints {
                    val cons = JanusMediaConstraints()
                    cons.recvAudio = false
                    cons.recvVideo = false
                    cons.sendAudio = true
                    return cons
                }

                override fun getTrickle(): Boolean {
                    return true
                }

                override fun onCallbackError(error: String) {}
            })
        }

        private fun joinRoomAsPublisher() {
            val obj = JSONObject()
            val msg = JSONObject()
            try {
                obj.put(REQUEST, "join")
                obj.put("room", mGroupId)
                obj.put("ptype", "publisher")
                obj.put("display", mOurClientId)
                msg.put(MESSAGE, obj)
            } catch (ex: Exception) {
                Log.e(TAG, "joinRoomAsPublisher: $ex")
            }
            handle?.sendMessage(PluginHandleSendMessageCallbacks(msg))
        }

        private fun configBitrate(newBitrate: Int) {
            val now = System.currentTimeMillis()
            if (now - lastTimeStampOfUpdateBitrate < MIN_DURATION_UPDATE_BITRATE) {
                Log.w(TAG, "configBitrate: config just requested before")
                return
            }
            if (newBitrate > MAX_BITRATE) {
                Log.w(TAG, "configBitrate: bitrate is maximum")
                return
            }
            lastTimeStampOfUpdateBitrate = now
            try {
                val msg = JSONObject()
                val body = JSONObject()
                body.put(REQUEST, "configure")
                body.put("bitrate", newBitrate)
                msg.put(MESSAGE, body)
                handle?.sendMessage(PluginHandleSendMessageCallbacks(msg))
            } catch (ex: java.lang.Exception) {
                Log.e(TAG, "configBitrate: $ex")
            }
        }

        private fun attachRemoteClient(remoteId: BigInteger) {
            janusServer!!.Attach(ListenerAttachCallbacks(mGroupId, remoteId))
        }
    }

    inner class JanusGatewayCallbacksImpl : IJanusGatewayCallbacks {
        override fun onSuccess() {
            janusServer!!.Attach(JanusPublisherPluginCallbacks())
        }

        override fun onDestroy() {}
        override fun getServerUri(): String {
            return JANUS_URI
        }

        override fun getToken(): String {
            return mGroupToken
            */
/*return "a1b2c3d4"*//*

        }

        override fun getIceServers(): List<IceServer> {
            val iceServers = ArrayList<IceServer>()
            */
/*val server = IceServer("stun:stun.l.google.com:19302")*//*

            val iceServer1 = IceServer(TURN_SERVER_URL, TURN_USER_NAME, TURN_PASSWORD)
            val iceServer2 = IceServer("stun:global.stun.twilio.com:3478")
            iceServers.add(iceServer1)
            iceServers.add(iceServer2)
            return iceServers
        }

        override fun getIpv6Support(): Boolean {
            return java.lang.Boolean.FALSE
        }

        override fun getMaxPollEvents(): Int {
            return 0
        }

        override fun onCallbackError(error: String) {}
    }

    companion object {
        private const val TAG = "InCallForegroundService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "ck_call_01"
        private const val ACTION_END_CALL = "ck.action.end.call"
        private const val MIN_DURATION_UPDATE_BITRATE = 5 * 1000 // Second
        private const val MAX_BITRATE = 1024 * 1000
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
        private const val BITRATE_STEP = 128 * 1000
    }
}*/