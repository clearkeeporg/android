package com.clearkeep.features.calls.presentation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog.Builder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.iterator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.Environment
import com.clearkeep.features.calls.R
import com.clearkeep.features.calls.databinding.ActivityInCallBinding
import com.clearkeep.features.calls.presentation.common.CallState
import com.clearkeep.features.calls.presentation.common.createVideoCapture
import com.clearkeep.features.calls.presentation.surfacegenerator.SurfacePosition
import com.clearkeep.features.calls.presentation.surfacegenerator.SurfacePositionFactory
import com.clearkeep.features.shared.createInCallNotification
import com.clearkeep.features.shared.dismissInCallNotification
import com.clearkeep.features.shared.presentation.AppCall
import com.clearkeep.features.shared.presentation.CallingStateData
import com.clearkeep.januswrapper.*
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.android.synthetic.main.toolbar_call_default.*
import kotlinx.android.synthetic.main.toolbar_call_default.view.*
import kotlinx.android.synthetic.main.view_control_call_audio.view.*
import kotlinx.android.synthetic.main.view_control_call_video.view.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*
import java.math.BigInteger
import javax.inject.Inject

@AndroidEntryPoint
class InCallActivity : BaseActivity(), JanusRTCInterface,
    PeerConnectionClient.PeerConnectionEvents {
    private val viewModel: InCallViewModel by viewModels()

    private val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    private val hideBottomButtonHandler: Handler = Handler(Looper.getMainLooper())

    private var mIsMute = false
    private var mIsMuteVideo = false
    private var mIsSpeaker = false

    private var mCurrentCallState: CallState = CallState.CALLING
    private var mTimeStarted: Long = 0

    private var mIsAudioMode: Boolean = false
    private var IsHeadPhone: Boolean = false
    private lateinit var mGroupId: String
    private lateinit var mGroupType: String
    private lateinit var mGroupName: String
    private lateinit var mOwnerClientId: String
    private lateinit var mOwnerDomain: String
    private lateinit var mUserNameInConversation: String
    private var mIsGroupCall: Boolean = false
    private lateinit var binding: ActivityInCallBinding

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    @Inject
    lateinit var environment: Environment

    // surface and render
    private lateinit var rootEglBase: EglBase
    private var peerConnectionClient: PeerConnectionClient? = null
    private var mWebSocketChannel: WebSocketChannel? = null
    private lateinit var mLocalSurfaceRenderer: SurfaceViewRenderer

    private val remoteRenders: MutableMap<BigInteger, RemoteInfo> = HashMap()

    private var endCallReceiver: BroadcastReceiver? = null

    private var switchVideoReceiver: BroadcastReceiver? = null

    private var broadcastReceiver: BroadcastReceiver? = null

    private var group: ChatGroup? = null

    // sound
    private var ringBackPlayer: MediaPlayer? = null
    private var busySignalPlayer: MediaPlayer? = null
    var isFromComingCall: Boolean = false
    var avatarInConversation = ""
    var groupName = ""
    var isShowDialogCamera = false

    @SuppressLint("ResourceType", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        printlnCK("InCallActivity onCreate()")
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
        if (environment.getServerCanNull() == null) {
            GlobalScope.launch(context = Dispatchers.Main) {
                val selectedServer = viewModel.getDefaultServer()
                selectedServer?.let { environment.setUpDomain(it) }
            }
        }
        mGroupId = intent.getStringExtra(EXTRA_GROUP_ID)!!
        mGroupName = intent.getStringExtra(EXTRA_GROUP_NAME)!!
        mGroupType = intent.getStringExtra(EXTRA_GROUP_TYPE)!!
        mOwnerDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN)!!
        mOwnerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT)!!
        mIsGroupCall = isGroup(mGroupType)
        mIsAudioMode = intent.getBooleanExtra(EXTRA_IS_AUDIO_MODE, false)
        isShowDialogCamera = !mIsAudioMode
        mUserNameInConversation = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
        isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)

        rootEglBase = EglBase.create()

        mLocalSurfaceRenderer = SurfaceViewRenderer(this)
        mLocalSurfaceRenderer.apply {
            init(rootEglBase.eglBaseContext, null)
            setZOrderMediaOverlay(true)
            setEnableHardwareScaler(true)
        }

        initViews()
        registerBroadcastReceiver()
        registerEndCallReceiver()

        if (mIsAudioMode) {
            configMedia(isSpeaker = false, isMuteVideo = true)
            registerSwitchVideoReceiver()
        } else {
            configMedia(isSpeaker = true, isMuteVideo = false)
        }

        requestCallPermissions()
        updateCallStatus(mCurrentCallState)
        dispatchCallStatus(true)

        createInCallNotification(this, InCallActivity::class.java)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configLandscapeLayout()
        }
    }

    private fun registerBroadcastReceiver() {
        Log.d("---", "registerBroadcastReceiver")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        0 -> {
                            IsHeadPhone = true
                            setSpeakerphoneOn(true)
                            Log.d("---", "Headset is unplugged - In Call")
                        }
                        1 -> {
                            IsHeadPhone = false
                            setSpeakerphoneOn(false)
                            Log.d("---", "Headset is plugged - In Call")
                        }
                    }
                }
            }
        }
        registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        )
    }

    private fun configMedia(isSpeaker: Boolean, isMuteVideo: Boolean) {
        mIsSpeaker = isSpeaker
        enableSpeaker(mIsSpeaker)

        mIsMuteVideo = isMuteVideo
        enableMuteVideo(mIsMuteVideo)
    }

    private fun initViews() {
        onAction()
        updateUIByStateAndMode()
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
        avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION) ?: ""
        tvUserName.text = groupName

        if (!TextUtils.isEmpty(groupName)) {
            includeToolbar.title.text = groupName
        }
        pipCallName.text = mGroupName

        runDelayToHideBottomButton()
        initWaitingCallView()
        initViewConnecting()
        updateRenders()
    }

    private fun initViewConnecting() {
        if (mIsAudioMode) viewConnecting.gone()
        if (isFromComingCall) {
            tvCallStateVideo.text = getString(R.string.call_connecting)
            tvCallState.text = getString(R.string.call_connecting)
            tvConnecting.visible()
        } else {
            tvCallStateVideo.text = getString(R.string.calling_group)
            tvCallState.text = getString(R.string.calling_group)
            tvConnecting.gone()
        }
        Glide.with(this)
            .load(avatarInConversation)
            .placeholder(R.drawable.ic_bg_gradient)
            .error(R.drawable.ic_bg_gradient)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 10)))
            .into(imageConnecting)
    }

    private fun runDelayToHideBottomButton() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        hideBottomButtonHandler.postDelayed(Runnable {
            if (!mIsAudioMode && mCurrentCallState == CallState.ANSWERED) {
                includeToolbar.gone()
                controlCallVideoView.gone()
            }
        }, 5 * 1000)
    }

    override fun onPermissionsAvailable() {
        isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)
        callScope.launch {
            withContext(Dispatchers.Main) {
                group = viewModel.getGroupById(
                    intent.getStringExtra(EXTRA_GROUP_ID)!!.toLong(),
                    mOwnerDomain,
                    mOwnerClientId
                )?.data
                if (isFromComingCall) {
                    val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
                    val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
                    val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
                    val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
                    val token = intent.getStringExtra(EXTRA_GROUP_TOKEN) ?: ""
                    val webRtcGroupId = intent.getStringExtra(EXTRA_WEB_RTC_GROUP_ID)!!.toInt()
                    val webRtcUrl = intent.getStringExtra(EXTRA_WEB_RTC_URL) ?: ""

                    startVideo(
                        webRtcGroupId,
                        webRtcUrl,
                        stunUrl,
                        turnUrl,
                        turnUserName,
                        turnPassword,
                        token
                    )
                } else {
                    val groupId = intent.getStringExtra(EXTRA_GROUP_ID)!!.toInt()
                    val result = viewModel.requestVideoCall(groupId, mIsAudioMode, getOwnerServer())

                    if (result != null) {
                        val turnConfig = result.turnServer
                        val stunConfig = result.stunServer
                        val turnUrl = turnConfig.server
                        val stunUrl = stunConfig.server
                        val token = result.groupRtcToken

                        val webRtcGroupId = result.groupRtcId
                        val webRtcUrl = result.groupRtcUrl
                        startVideo(
                            webRtcGroupId.toInt(),
                            webRtcUrl,
                            stunUrl,
                            turnUrl,
                            turnConfig.user,
                            turnConfig.pwd,
                            token
                        )
                    } else {
                        runOnUiThread {
                            updateCallStatus(CallState.CALL_NOT_READY)
                        }
                        return@withContext
                    }
                }
            }

            delay(CALL_WAIT_TIME_OUT)
            if (mCurrentCallState.name == CallState.CALLING.toString()) {
                runOnUiThread {
                    updateCallStatus(CallState.CALL_NOT_READY)
                }
            }
        }
    }

    private fun getOwnerServer(): Owner {
        return Owner(mOwnerDomain, mOwnerClientId)
    }

    override fun onPermissionsDenied() {
        finishAndReleaseResource()
    }

    override fun onPermissionsForeverDenied() {
        showAskPermissionDialog()
    }

    override fun onDestroy() {
        dismissInCallNotification(this)
        super.onDestroy()
        dispatchCallStatus(false)
        if (chronometerTimeCall.visibility == View.VISIBLE) {
            chronometerTimeCall.stop()
        }
        
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            includeToolbar.gone()
            controlCallVideoView.gone()
            controlCallAudioView.gone()
            imgEndWaiting.gone()
            tvEndButtonDescription.gone()
            pipInfo.visible()
            surfaceRootContainer.gone()
        } else {
            // Restore the full-screen UI.
            includeToolbar.visible()
            imgEndWaiting.visible()
            tvEndButtonDescription.visible()
            pipInfo.gone()
            updateRenders()
            surfaceRootContainer.visible()
            if (!mIsAudioMode) {
                controlCallVideoView.visible()
            } else {
                controlCallAudioView.visible()
            }
        }
    }

    override fun onBackPressed() {
        handleBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configLandscapeLayout()
        } else {
            configPortraitLayout()
        }
    }

    fun onAction() {
        imgEndWaiting.setOnClickListener {
            endCall()
        }
        includeToolbar.imgBack.setOnClickListener {
            handleBackPressed()
        }

        controlCallAudioView.toggleSpeaker.setOnClickListener {
            mIsSpeaker = !mIsSpeaker
            enableSpeaker(mIsSpeaker)
        }

        controlCallAudioView.toggleMute.setOnClickListener {
            mIsMute = !mIsMute
            enableMute(mIsMute)
            controlCallVideoView.bottomToggleMute.isChecked = mIsMute
            runDelayToHideBottomButton()
        }

        controlCallAudioView.toggleFaceTime.setOnClickListener {
            switchToVideoMode()
        }

        controlCallAudioView.imgEndWaiting.setOnClickListener {
            endCall()
        }

        controlCallVideoView.bottomToggleMute.setOnClickListener {
            mIsMute = !mIsMute
            enableMute(mIsMute)
            controlCallAudioView.toggleMute.isChecked = mIsMute
            runDelayToHideBottomButton()
        }

        controlCallVideoView.bottomToggleFaceTime.setOnClickListener {
            mIsMuteVideo = !mIsMuteVideo
            enableMuteVideo(mIsMuteVideo)
            runDelayToHideBottomButton()
        }

        controlCallVideoView.bottomToggleSwitchCamera.setOnClickListener {
            peerConnectionClient?.switchCamera()
        }

        controlCallVideoView.bottomImgEndCall.setOnClickListener {
            showDialogConfirmLeaveCall()
        }
        surfaceRootContainer.setOnClickListener {
            if (!mIsAudioMode && mCurrentCallState == CallState.ANSWERED)
                if (includeToolbar.visibility == View.VISIBLE) {
                    includeToolbar.gone()
                    controlCallVideoView.gone()
                } else {
                    includeToolbar.visible()
                    controlCallVideoView.visible()
                    runDelayToHideBottomButton()
                }
        }

        imgEndWaiting.setOnClickListener {
            showDialogConfirmLeaveCall()
        }
    }

    private fun showDialogConfirmLeaveCall() {
        Builder(this)
            .setTitle(getString(R.string.warning))
            .setMessage(getString(R.string.dialog_leave_call_title))
            .setPositiveButton(getString(R.string.leave)) { _, _ ->
                endCall()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->

            }
            .create()
            .show()
    }

    private fun handleBackPressed() {
        if (hasSupportPIP()) {
            enterPIPMode()
        } else {
            showDialogConfirmLeaveCall()
        }
    }

    private fun initWaitingCallView() {
        tvUserName2.text = mGroupName
        if (mIsAudioMode)
            controlCallVideoView.gone()
        else
            controlCallVideoView.visible()
        includeToolbar.gone()
        if (isFromComingCall) waitingCallVideoView.gone()
    }

    private fun startVideo(
        janusGroupId: Int, janusUrl: String,
        stunUrl: String,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        token: String
    ) {
        val ourClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        printlnCK("Janus URL: $janusUrl")
        printlnCK(
            "startVideo: janusUrl = $janusUrl, janusGroupId = $janusGroupId , stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass" +
                    ", token = $token"
        )
        mWebSocketChannel = WebSocketChannel(janusGroupId, ourClientId, token, janusUrl)
        mWebSocketChannel!!.initConnection()
        mWebSocketChannel!!.setDelegate(this)
        val peerConnectionParameters = PeerConnectionClient.PeerConnectionParameters(
            false, 360, 480, 20, "VP8",
            true, 0, "opus", false,
            false, false, false, false,
            turnUrl, turnUser, turnPass, stunUrl
        )
        peerConnectionClient = PeerConnectionClient()
        peerConnectionClient!!.createPeerConnectionFactory(this, peerConnectionParameters, this)
        peerConnectionClient!!.startVideoSource()
        enableMuteVideo(mIsMuteVideo)
    }

    private fun displayCountUpClockOfConversation() {
        mTimeStarted = SystemClock.elapsedRealtime()
        includeToolbar.chronometerTimeCall.apply {
            base = mTimeStarted
            start()
        }
        if (isFromComingCall) {
            Handler(mainLooper).postDelayed({
                viewConnecting.gone()
            }, 2000)
        }
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        mCurrentCallState = newState

        when (mCurrentCallState) {
            CallState.CALLING -> {
                if (!isFromComingCall)
                    playRingBackTone()
            }
            CallState.RINGING -> {
            }
            CallState.BUSY, CallState.CALL_NOT_READY -> {
                tvCallState.text = getString(R.string.text_busy)
                stopRingBackTone()
                playBusySignalSound()
                GlobalScope.launch {
                    delay(3 * 1000)
                    endCall()
                }
            }

            CallState.ENDED -> {
                tvCallState.text = getString(R.string.text_end)
                endCall()
            }
            CallState.ANSWERED -> {
                tvCallState.text = getString(R.string.text_started)
                stopRingBackTone()
                displayCountUpClockOfConversation()
                updateUIByStateAndMode()
                dispatchCallStatus(true)
            }
        }
    }

    private fun dispatchCallStatus(isStarted: Boolean) {
        AppCall.listenerCallingState.postValue(
            CallingStateData(
                isStarted,
                mUserNameInConversation,
                false,
                mTimeStarted
            )
        )
    }

    private fun switchToVideoMode() {
        mIsAudioMode = false
        configMedia(isSpeaker = true, isMuteVideo = false)
        updateUIByStateAndMode()
        callScope.launch {
            viewModel.switchAudioToVideoCall(mGroupId.toInt(), getOwnerServer())
        }
    }

    private fun showOpenCameraDialog() {
        isShowDialogCamera = true
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.call_request_video_dialog_title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                configMedia(isSpeaker = true, isMuteVideo = false)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun updateUIByStateAndMode() {
        if (mCurrentCallState == CallState.ANSWERED) {
            groupAudioWaiting.gone()
            waitingCallVideoView.gone()

            if (mIsAudioMode) {
                includeToolbar.visible()
                controlCallVideoView.gone()
                tvEndButtonDescription.text = getString(R.string.end_call)
            } else {
                waitingCallView.gone()
                includeToolbar.visible()
                controlCallVideoView.visible()
            }
        } else {
            if (!mIsAudioMode) {
                waitingCallView.gone()
                includeToolbar.gone()
                controlCallVideoView.visible()
                if (isFromComingCall) {
                    waitingCallVideoView.gone()
                } else {
                    waitingCallVideoView.visible()
                }
            } else {
                waitingCallView.visible()
                groupAudioWaiting.visible()
                waitingCallVideoView.gone()
            }
        }
    }

    private fun enableSpeaker(isEnable: Boolean) {
        controlCallAudioView.toggleSpeaker.isChecked = isEnable
        setSpeakerphoneOn(isEnable)
    }

    private fun enableMute(isMuting: Boolean) {
        controlCallAudioView.toggleMute.isChecked = isMuting
        controlCallVideoView.bottomToggleMute.isChecked = isMuting
        peerConnectionClient?.setAudioEnabled(!isMuting)
    }

    private fun enableMuteVideo(isMuteVideo: Boolean) {
        controlCallAudioView.toggleFaceTime.isChecked = isMuteVideo
        controlCallVideoView.bottomToggleFaceTime.isChecked = isMuteVideo
        peerConnectionClient?.setLocalVideoEnable(!isMuteVideo)
    }

    private fun setSpeakerphoneOn(isOn: Boolean) {
        printlnCK("setSpeakerphoneOn, isOn = $isOn")
        try {
            val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            if (!IsHeadPhone) {
                audioManager.isSpeakerphoneOn = isOn
            } else audioManager.isSpeakerphoneOn = true

        } catch (e: Exception) {
            printlnCK("setSpeakerphoneOn, exception!! $e")
        }
    }

    private fun hangup() {
        dismissInCallNotification(this)
        callScope.cancel()
        mWebSocketChannel?.close()
        peerConnectionClient?.close()
    }

    private fun finishAndReleaseResource() {
        hideBottomButtonHandler.removeCallbacksAndMessages(null)
        unRegisterEndCallReceiver()
        unRegisterSwitchVideoReceiver()
        unRegisterBroadcastReceiver()
        stopRingBackTone()
        stopBusySignalSound()
        if (!isFromComingCall && (mCurrentCallState == CallState.CALLING || mCurrentCallState == CallState.CALL_NOT_READY)) {
            cancelCallAPI()
        }
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun cancelCallAPI() {
        GlobalScope.launch {
            viewModel.cancelCall(mGroupId.toInt(), getOwnerServer())
        }
    }

    private fun offerPeerConnection(handleId: BigInteger) {
        peerConnectionClient?.createPeerConnection(
            rootEglBase.eglBaseContext,
            mLocalSurfaceRenderer,
            createVideoCapture(this),
            handleId
        )

        peerConnectionClient?.createOffer(handleId)
    }

    // interface JanusRTCInterface
    override fun onPublisherJoined(handleId: BigInteger) {
        offerPeerConnection(handleId)
        runOnUiThread {
            if (!isFromComingCall)
                Handler(mainLooper).postDelayed({
                    viewConnecting.gone()
                }, 1000)
        }
    }

    override fun onPublisherRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.setRemoteDescription(handleId, sessionDescription)
    }

    override fun subscriberHandleRemoteJsep(janusHandle: JanusHandle, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.subscriberHandleRemoteJsep(
            janusHandle.handleId,
            janusHandle.display,
            sessionDescription
        )
    }

    override fun onLeaving(handleId: BigInteger) {
        printlnCK("onLeaving: $handleId")
        removeRemoteRender(handleId)
    }

    override fun onLocalDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.publisherCreateOffer(handleId, sdp)
    }

    override fun onRemoteDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.subscriberCreateAnswer(handleId, sdp)
    }

    override fun onIceCandidate(candidate: IceCandidate?, handleId: BigInteger) {
        if (candidate != null) {
            mWebSocketChannel?.trickleCandidate(handleId, candidate)
        } else {
            mWebSocketChannel?.trickleCandidateComplete(handleId)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

    override fun onIceConnected() {
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {}

    override fun onPeerConnectionError(description: String) {
        printlnCK("onPeerConnectionError: $description")
    }

    override fun onRemoteRenderAdded(connection: JanusConnection) {
        printlnCK("onRemoteRenderAdded: ${connection.handleId}")
        runOnUiThread {
            if (mCurrentCallState != CallState.ANSWERED) {
                updateCallStatus(CallState.ANSWERED)
            }

            val remoteRender = SurfaceViewRenderer(this)
            remoteRender.init(rootEglBase.eglBaseContext, null)
            connection.videoTrack.addRenderer(VideoRenderer(remoteRender))
            remoteRenders[connection.handleId] = RemoteInfo(
                connection.display,
                remoteRender
            )

            updateRenders()
        }
    }

    override fun onRemoteRenderRemoved(connection: JanusConnection) {
        printlnCK("onRemoteRenderRemoved")
        removeRemoteRender(connection.handleId)
    }

    private fun removeRemoteRender(handleId: BigInteger) {
        runOnUiThread {
            val render = remoteRenders.remove(handleId)
            render?.surfaceViewRenderer?.release()

            if (remoteRenders.isEmpty() && !isGroup(mGroupType)) {
                updateCallStatus(CallState.ENDED)
                return@runOnUiThread
            } else {
                updateRenders()
            }
        }
    }

    private fun updateRenders() {
        printlnCK("updateRenders()")
        for (child in binding.surfaceRootContainer.iterator()) {
            (child as RelativeLayout).removeAllViews()
        }
        binding.surfaceRootContainer.removeAllViews()

        val renders = remoteRenders.values

        val me = group?.clientList?.find { it.userId == environment.getServer().profile.userId }
        val surfaceGenerator =
            SurfacePositionFactory().createSurfaceGenerator(
                this,
                renders.size + 1,
                screenWidth,
                screenHeight
            )

        val localSurfacePosition = surfaceGenerator.getLocalSurface()
        val cameraView =
            createRemoteView(mLocalSurfaceRenderer, me?.userName ?: "", localSurfacePosition)
        binding.surfaceRootContainer.addView(cameraView)

        // add remote streams
        val remoteSurfacePositions = surfaceGenerator.getRemoteSurfaces()
        remoteSurfacePositions.forEachIndexed { index, remoteSurfacePosition ->
            val remoteInfo = renders.elementAt(index)
            val user = group?.clientList?.find { it.userId == remoteInfo.clientId }
            printlnCK("updateRenders remoteSurfacePosition() user ${user?.userName} position remoteSurfacePosition $remoteSurfacePosition")
            val view = createRemoteView(
                remoteInfo.surfaceViewRenderer,
                user?.userName ?: "unknown",
                remoteSurfacePosition
            )
            binding.surfaceRootContainer.addView(view)
        }
    }

    private fun createRemoteView(
        renderer: SurfaceViewRenderer, remoteName: String,
        remoteSurfacePosition: SurfacePosition
    ): RelativeLayout {
        val params = RelativeLayout.LayoutParams(
            remoteSurfacePosition.width, remoteSurfacePosition.height
        ).apply {
            leftMargin = remoteSurfacePosition.marginStart
            topMargin = remoteSurfacePosition.marginTop
        }
        val relativeLayout = RelativeLayout(this)
        relativeLayout.layoutParams = params

        val tv = TextView(this)
        tv.text = remoteName
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setPadding(0, 0, 0, 24)
        tv.setTextColor(resources.getColor(R.color.grayscaleOffWhite))
        val nameLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        nameLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        /*val muteImage = ImageView(this)
        muteImage.setImageResource(R.drawable.ic_status_muted)
        val muteLayoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        muteLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)*/

        val rendererParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        relativeLayout.addView(renderer, rendererParams)
        relativeLayout.addView(tv, nameLayoutParams)
        /*relativeLayout.addView(muteImage, muteLayoutParams)*/
        return relativeLayout
    }

    private fun showAskPermissionDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.dialog_call_permission_denied_title))
            .setMessage(getString(R.string.dialog_call_permission_denied_text))
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                openSettingScreen()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finishAndReleaseResource()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun registerEndCallReceiver() {
        endCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)
                printlnCK("receive a end call event with group id = $groupId")
                if (mGroupId == groupId) {
                    endCall()
                }
            }
        }
        registerReceiver(
            endCallReceiver,
            IntentFilter(ACTION_CALL_CANCEL)
        )
    }

    private fun endCall() {
        hangup()
        finishAndReleaseResource()
    }

    private fun unRegisterEndCallReceiver() {
        if (endCallReceiver != null) {
            unregisterReceiver(endCallReceiver)
            endCallReceiver = null
        }
    }

    private fun unRegisterBroadcastReceiver() {
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerSwitchVideoReceiver() {
        printlnCK("registerSwitchVideoReceiver")
        switchVideoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupId = intent.getStringExtra(EXTRA_CALL_SWITCH_VIDEO).toString()
                if (mGroupId == groupId && mIsAudioMode) {
                    printlnCK("switch group $groupId to video mode")
                    if (mIsAudioMode && !isShowDialogCamera)
                        showOpenCameraDialog()
                    mIsAudioMode = false
                    configMedia(isSpeaker = mIsSpeaker, isMuteVideo = mIsMuteVideo)
                    updateUIByStateAndMode()
                }
            }
        }
        registerReceiver(
            switchVideoReceiver,
            IntentFilter(ACTION_CALL_SWITCH_VIDEO)
        )
    }

    private fun unRegisterSwitchVideoReceiver() {
        if (switchVideoReceiver != null) {
            unregisterReceiver(switchVideoReceiver)
            switchVideoReceiver = null
        }
    }

    private fun playRingBackTone() {
        ringBackPlayer = MediaPlayer.create(this, R.raw.sound_ringback_tone)
        ringBackPlayer?.isLooping = true
        ringBackPlayer?.start()
    }

    private fun stopRingBackTone() {
        ringBackPlayer?.stop()
        ringBackPlayer?.release()
        ringBackPlayer = null
    }

    private fun playBusySignalSound() {
        busySignalPlayer = MediaPlayer.create(this, R.raw.sound_busy_signal)
        busySignalPlayer?.start()
    }

    private fun stopBusySignalSound() {
        busySignalPlayer?.stop()
        busySignalPlayer?.release()
        busySignalPlayer = null
    }

    private fun configPortraitLayout() {
        controlCallAudioView.imgEndWaiting.gone()
        controlCallAudioView.tvEndButtonDescription.gone()
        tvEndButtonDescription.visible()
        imgEndWaiting.visible()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.TOP,
            R.id.parent,
            ConstraintSet.TOP
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(controlCallAudioView as ConstraintLayout)

        val layoutParams = controlCallAudioView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 0.532f
        controlCallAudioView.layoutParams = layoutParams
    }

    private fun configLandscapeLayout() {
        controlCallAudioView.imgEndWaiting.visible()
        controlCallAudioView.tvEndButtonDescription.visible()
        tvEndButtonDescription.gone()
        imgEndWaiting.gone()

        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(controlCallAudioView as ConstraintLayout)
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.END,
            R.id.parent,
            ConstraintSet.END
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.START,
            R.id.parent,
            ConstraintSet.START
        )
        mConstraintSet.connect(
            R.id.controlCallAudioView,
            ConstraintSet.BOTTOM,
            R.id.parent,
            ConstraintSet.BOTTOM
        )
        mConstraintSet.applyTo(controlCallAudioView as ConstraintLayout)

        val layoutParams = controlCallAudioView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = 1f
        controlCallAudioView.layoutParams = layoutParams
    }

    data class RemoteInfo(
        val clientId: String,
        val surfaceViewRenderer: SurfaceViewRenderer
    )

    companion object {
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
        const val DOMAIN = "domain"
        const val CLIENT_ID = "client_id"
    }
}