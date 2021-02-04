package com.clearkeep.screen.videojanus

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.TypedArray
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.Rational
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.clearkeep.R
import com.clearkeep.databinding.ActivityInCallBinding
import com.clearkeep.januswrapper.JanusConnection
import com.clearkeep.januswrapper.JanusRTCInterface
import com.clearkeep.januswrapper.PeerConnectionClient
import com.clearkeep.januswrapper.WebSocketChannel
import com.clearkeep.screen.repo.VideoCallRepository
import com.clearkeep.screen.videojanus.common.AvatarImageTask
import com.clearkeep.screen.videojanus.common.createVideoCapture
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_in_call.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*
import java.math.BigInteger
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class InCallActivity : AppCompatActivity(), View.OnClickListener, JanusRTCInterface, PeerConnectionClient.PeerConnectionEvents {
    enum class CallState {
        CALLING,
        RINGING,
        ANSWERED,
        BUSY,
        ENDED,
        CALL_NOT_READY
    }

    val callScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var mIsMute = false
    private var mIsMuteVideo = false
    private var mIsSpeaker = false

    private var mCurrentCallState: CallState = CallState.CALLING

    private lateinit var binding: ActivityInCallBinding

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    // surface and render
    private lateinit var rootEglBase: EglBase
    private var peerConnectionClient: PeerConnectionClient? = null
    private var mWebSocketChannel: WebSocketChannel? = null

    private val remoteRenders: MutableMap<BigInteger, SurfaceViewRenderer> = HashMap()

    // resource id
    private var mResIdSpeakerOn = 0
    private var mResIdSpeakerOff = 0
    private var mResIdMuteOn = 0
    private var mResIdMuteOff = 0
    private var mResIdMuteVideoOn = 0
    private var mResIdMuteVideoOff = 0

    // input

    @SuppressLint("ResourceType", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("java.net.preferIPv6Addresses", "false")
        System.setProperty("java.net.preferIPv4Stack", "true")
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val a: TypedArray = theme.obtainStyledAttributes(
            intArrayOf(
                R.attr.buttonSpeakerOn, R.attr.buttonSpeakerOff,
                R.attr.buttonMuteOn, R.attr.buttonMuteOff,
                R.attr.buttonMuteVideoOn, R.attr.buttonMuteVideoOff
            )
        )
        try {
            mResIdSpeakerOn = a.getResourceId(0, R.drawable.ic_string_ee_sound_on)
            mResIdSpeakerOff = a.getResourceId(1, R.drawable.ic_string_ee_sound_off)
            mResIdMuteOn = a.getResourceId(2, R.drawable.ic_string_ee_mute_on)
            mResIdMuteOff = a.getResourceId(3, R.drawable.ic_string_ee_mute_off)
            mResIdMuteVideoOn = a.getResourceId(4, R.drawable.baseline_videocam_white_18)
            mResIdMuteVideoOff = a.getResourceId(5, R.drawable.baseline_videocam_off_white_18)
        } finally {
            a.recycle()
        }

        rootEglBase = EglBase.create()
        binding.localSurfaceView.init(rootEglBase.eglBaseContext, null)
        binding.localSurfaceView.setEnableHardwareScaler(true)

        initViews()

        requestCallPermissions()
        updateCallStatus(mCurrentCallState)
    }

    private fun initViews() {
        binding.imgBack.setOnClickListener(this)
        binding.imgSpeaker.setOnClickListener(this)
        binding.imgMute.setOnClickListener(this)
        binding.imgEnd.setOnClickListener(this)
        binding.imgSwitchCamera.setOnClickListener(this)
        binding.imgVideoMute.setOnClickListener(this)

        val userNameInConversation = intent.getStringExtra(EXTRA_USER_NAME)
        val avatarInConversation = intent.getStringExtra(EXTRA_AVATAR_USER_IN_CONVERSATION)
        if (!TextUtils.isEmpty(userNameInConversation)) {
            binding.tvUserName.text = userNameInConversation
        }
        if (!TextUtils.isEmpty(avatarInConversation)) {
            AvatarImageTask(binding.imgThumb).execute(avatarInConversation)
        }
    }

    fun requestCallPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                )
                            != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
                            != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                    ),
                    REQUEST_PERMISSIONS
                )
                return
            }
        }
        onCallPermissionsAvailable()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS &&
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCallPermissionsAvailable()
        } else {
            finishAndRemoveFromTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rootEglBase.release()
        val localRender = binding.localSurfaceView
        localRender.release()
        val renderList = remoteRenders.values
        if (renderList.isNotEmpty()) {
            for (render in renderList) {
                render.release()
            }
        }
        if (binding.chronometer.visibility == View.VISIBLE) {
            binding.chronometer.stop()
        }
    }

    private fun onCallPermissionsAvailable() {
        val isFromComingCall = intent.getBooleanExtra(EXTRA_FROM_IN_COMING_CALL, false)
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)!!.toInt()
        if (isFromComingCall) {
            val turnUserName = intent.getStringExtra(EXTRA_TURN_USER_NAME) ?: ""
            val turnPassword = intent.getStringExtra(EXTRA_TURN_PASS) ?: ""
            val turnUrl = intent.getStringExtra(EXTRA_TURN_URL) ?: ""
            val stunUrl = intent.getStringExtra(EXTRA_STUN_URL) ?: ""
            startVideo(groupId, stunUrl, turnUrl, turnUserName, turnPassword)
        } else {
            callScope.launch {
                val result = videoCallRepository.requestVideoCall(groupId)
                if (result != null) {
                    val turnConfig = result.turnServer
                    val stunConfig = result.stunServer
                    val turnUrl = turnConfig.server
                    val stunUrl = stunConfig.server
                    startVideo(groupId, stunUrl, turnUrl, turnConfig.user, turnConfig.pwd)
                } else {
                    runOnUiThread {
                        updateCallStatus(CallState.CALL_NOT_READY)
                    }
                }
                delay(CALL_WAIT_TIME_OUT)
                if (remoteRenders.isEmpty()) {
                    runOnUiThread {
                        updateCallStatus(CallState.CALL_NOT_READY)
                    }
                }
            }
        }
    }

    private fun startVideo(groupId: Int, stunUrl: String, turnUrl: String, turnUser: String, turnPass: String) {
        val ourClientId = intent.getStringExtra(EXTRA_OUR_CLIENT_ID) ?: ""
        val token = intent.getStringExtra(EXTRA_GROUP_TOKEN)
        printlnCK("startVideo: stun = $stunUrl, turn = $turnUrl, username = $turnUser, pwd = $turnPass, group = $groupId, token = $token")
        mWebSocketChannel = WebSocketChannel(groupId, ourClientId, token, JANUS_URI)
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
    }

    private fun displayCountUpClockOfConversation() {
        if (binding.chronometer.isVisible) {
            return
        }
        binding.chronometer.visibility = View.VISIBLE
        binding.tvCallState.visibility = View.GONE

        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()
    }

    private fun updateCallStatus(newState: CallState) {
        printlnCK("update call state: $newState")
        mCurrentCallState = newState
        when (mCurrentCallState) {
            CallState.CALLING ->
                binding.tvCallState.text = getString(R.string.text_calling)
            CallState.RINGING ->
                binding.tvCallState.text = getString(R.string.text_ringing)
            CallState.BUSY, CallState.CALL_NOT_READY -> {
                binding.tvCallState.text = getString(R.string.text_busy)
                hangup()
                finishAndRemoveFromTask()
            }
            CallState.ENDED -> {
                binding.tvCallState.text = getString(R.string.text_end)
                hangup()
                finishAndRemoveFromTask()
            }
            CallState.ANSWERED -> {

                binding.tvCallState.text = getString(R.string.text_started)
                displayCountUpClockOfConversation()
                showOrHideAvatar(false)
            }
        }
    }

    private fun showOrHideAvatar(isShowAvatar: Boolean) {
        runOnUiThread {
            if (isShowAvatar) {
                binding.containerUserInfo.visibility = View.VISIBLE
            } else {
                binding.containerUserInfo.visibility = View.GONE
            }
        }
    }

    override fun onUserLeaveHint() {
        if (hasSupportPIP()) {
            enterPictureInPictureMode()
        }
    }

    private fun hasSupportPIP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            binding.imgBack.visibility = View.GONE
            binding.imgSpeaker.visibility = View.GONE
            binding.imgMute.visibility = View.GONE
            binding.imgEnd.visibility = View.GONE
            binding.imgSwitchCamera.visibility = View.GONE
        } else {
            // Restore the full-screen UI.
            binding.imgBack.visibility = View.VISIBLE
            binding.imgSpeaker.visibility = View.VISIBLE
            binding.imgMute.visibility = View.VISIBLE
            binding.imgEnd.visibility = View.VISIBLE
            binding.imgSwitchCamera.visibility = View.VISIBLE
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgEnd -> {
                hangup()
                finishAndRemoveFromTask()
            }
            R.id.imgBack -> {
                if (hasSupportPIP()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val aspectRatio = Rational(3, 4)
                        val params =
                            PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                        enterPictureInPictureMode(params)
                    } else {
                        enterPictureInPictureMode()
                    }
                } else {
                    hangup()
                    finishAndRemoveFromTask()
                }
            }
            R.id.imgSpeaker -> {
                mIsSpeaker = !mIsSpeaker
                enableSpeaker(mIsSpeaker)
                setSpeakerphoneOn(mIsSpeaker)
            }
            R.id.imgMute -> {
                mIsMute = !mIsMute
                enableMute(mIsMute)
                peerConnectionClient?.setAudioEnabled(!mIsMute)
            }
            R.id.imgSwitchCamera -> {
                peerConnectionClient?.switchCamera()
            }
            R.id.imgVideoMute -> {
                mIsMuteVideo = !mIsMuteVideo
                enableMuteVideo(mIsMuteVideo)
                peerConnectionClient?.setVideoEnabled(!mIsMuteVideo)
            }
        }
    }

    private fun enableSpeaker(isEnable: Boolean) {
        binding.imgSpeaker.isClickable = true
        if (isEnable) {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOn)
        } else {
            binding.imgSpeaker.setImageResource(mResIdSpeakerOff)
        }
    }

    private fun enableMute(isMuting: Boolean) {
        binding.imgMute.isClickable = true
        if (isMuting) {
            binding.imgMute.setImageResource(mResIdMuteOn)
        } else {
            binding.imgMute.setImageResource(mResIdMuteOff)
        }
    }

    private fun enableMuteVideo(isMuteVideo: Boolean) {
        binding.imgVideoMute.isClickable = true
        if (isMuteVideo) {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOff)
        } else {
            binding.imgVideoMute.setImageResource(mResIdMuteVideoOn)
        }
    }

    private fun setSpeakerphoneOn(isOn: Boolean) {
        printlnCK("setSpeakerphoneOn, isOn = $isOn")
        try {
            val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = isOn
        } catch (e: Exception) {
            printlnCK("setSpeakerphoneOn, $e")
        }
    }

    private fun hangup() {
        callScope.cancel()
        mWebSocketChannel?.close()
        peerConnectionClient?.close()
    }

    private fun finishAndRemoveFromTask() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
        private const val CALL_WAIT_TIME_OUT: Long = 60 * 1000
    }

    private fun offerPeerConnection(handleId: BigInteger) {
        peerConnectionClient?.createPeerConnection(
            rootEglBase.eglBaseContext,
            binding.localSurfaceView,
            createVideoCapture(this),
            handleId
        )
        peerConnectionClient?.createOffer(handleId)
    }

    // interface JanusRTCInterface
    override fun onPublisherJoined(handleId: BigInteger) {
        offerPeerConnection(handleId)
    }

    override fun onPublisherRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.setRemoteDescription(handleId, sessionDescription)
    }

    override fun subscriberHandleRemoteJsep(handleId: BigInteger, jsep: JSONObject) {
        val type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"))
        val sdp = jsep.optString("sdp")
        val sessionDescription = SessionDescription(type, sdp)
        peerConnectionClient?.subscriberHandleRemoteJsep(handleId, sessionDescription)
    }

    override fun onLeaving(handleId: BigInteger) {
        printlnCK("onLeaving: $handleId")
        removeRemoteRender(handleId)
    }

    // interface PeerConnectionClient.PeerConnectionEvents
    override fun onLocalDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.publisherCreateOffer(handleId, sdp)
    }

    override fun onRemoteDescription(sdp: SessionDescription, handleId: BigInteger) {
        printlnCK(sdp.type.toString())
        mWebSocketChannel?.subscriberCreateAnswer(handleId, sdp)
    }

    override fun onIceCandidate(candidate: IceCandidate?, handleId: BigInteger) {
        printlnCK("=========onIceCandidate========")
        if (candidate != null) {
            mWebSocketChannel?.trickleCandidate(handleId, candidate)
        } else {
            mWebSocketChannel?.trickleCandidateComplete(handleId)
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

    override fun onIceConnected() {
        printlnCK("onIceConnected")
    }

    override fun onIceDisconnected() {
        printlnCK("onIceDisconnected")
    }

    override fun onPeerConnectionClosed() {
        printlnCK("onPeerConnectionClosed")
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
            remoteRenders[connection.handleId] = remoteRender

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
            render?.release()

            if (remoteRenders.isEmpty()) {
                updateCallStatus(CallState.ENDED)
                return@runOnUiThread
            } else {
                updateRenders()
            }
        }
    }

    private fun updateRenders() {
        binding.remoteRoot.removeAllViews()
        val renders = remoteRenders.values
        printlnCK("updateRenders: length = ${renders.size}")
        if (renders.isNotEmpty()) {
            val params = RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            binding.remoteRoot.addView(renders.first(), params)
        }

        val localRender = binding.localSurfaceView
        binding.localRoot.removeAllViews()
        val localParams = LinearLayout.LayoutParams(200, 300)
        localParams.setMargins(20, 20, 0, 0)
        binding.localRoot.addView(localRender, localParams)
    }
}