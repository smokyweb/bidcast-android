package io.bidswipe.app.utils

import android.content.Context
import android.view.SurfaceView
import android.widget.FrameLayout
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoDenoiserOptions
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.bidswipe.app.utils.Alerts.log

class AgoraManager(
	val mCtx: Context,
	val appID: String
) {

	var mRtcEngine: RtcEngine? = null
	private var isSwitched: Boolean = false
	var isMuted: Boolean = false
	private var isInitialized: Boolean = false
	private val readyCallbacks = mutableListOf<() -> Unit>()

	companion object {
		const val TAG = "AGORA-MANAGER"
	}

	var onUserJoin: ((Int, Int) -> Unit)? = null
	var onUserLeave: ((Int, Int) -> Unit)? = null
	var onEngineError: ((Int) -> Unit)? = null

	private val mRtcEventHandler = object : IRtcEngineEventHandler() {
		override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
			super.onJoinChannelSuccess(channel, uid, elapsed)
			log(TAG, "Joined channel $channel")
		}

		override fun onUserJoined(uid: Int, elapsed: Int) {
			log(TAG, "User joined: $uid")
			onUserJoin?.invoke(uid, elapsed)
		}

		override fun onUserOffline(uid: Int, reason: Int) {
			super.onUserOffline(uid, reason)
			log(TAG, "User offline: $uid")
			onUserLeave?.invoke(uid, reason)
		}

		override fun onError(err: Int) {
			super.onError(err)
			log(TAG, "Error: $err")
			onEngineError?.invoke(err)
		}
	}

	fun initializeAgoraSDK(role:Int) {
		runSafe {
			if (isInitialized) {
				log(TAG, "initializeAgoraSDK called while engine already active – restarting engine")
				destroyEngine()
			}

			val config = RtcEngineConfig().also {
				it.mContext = mCtx
				it.mAppId = appID
				it.mEventHandler = mRtcEventHandler
			}

			val engine = RtcEngine.create(config)
			engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
			engine.setVideoDenoiserOptions(true, VideoDenoiserOptions())
			engine.setClientRole(role)
			engine.setVideoEncoderConfiguration(videoConfig())
			engine.setVideoQualityParameters(false)
			engine.enableVideo()
			engine.startPreview()

			mRtcEngine = engine
			isInitialized = true
			log(TAG, "AGORA MANAGER INITIALIZED")
			if (readyCallbacks.isNotEmpty()) {
				readyCallbacks.toList().forEach { it.invoke() }
				readyCallbacks.clear()
			}
		}
	}

	/*fun onReady(action: () -> Unit) {
		if (isInitialized && mRtcEngine != null) {
			action.invoke()
		} else {
			readyCallbacks.add(action)
		}
	}*/

	fun isReady(): Boolean = isInitialized && mRtcEngine != null

	fun setupPublisherView(mView: FrameLayout) {
		val surfaceView = SurfaceView(mCtx)
		val videoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
		videoCanvas.mirrorMode = Constants.VIDEO_MIRROR_MODE_AUTO
		mView.addView(surfaceView)
		mRtcEngine?.setupLocalVideo(videoCanvas)
	}

	fun joinChannel( token : String, channelName : String) {
		val options = ChannelMediaOptions().also {
			it.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
			it.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
			it.autoSubscribeAudio = false
			it.autoSubscribeVideo = false
			it.publishMicrophoneTrack = true
			it.publishCameraTrack = true
		}

		mRtcEngine?.joinChannel(token, channelName, 0, options)
	}

	fun joinSubscriberChannel( token : String, channelName : String) {
		val options = ChannelMediaOptions().also {
			it.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
			it.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
			it.autoSubscribeAudio = true
			it.autoSubscribeVideo = true
			it.publishMicrophoneTrack = false
			it.publishCameraTrack = false
		}
		mRtcEngine?.joinChannel(token, channelName, 0, options)
	}

	fun zoomCamera(zoomLevel : Float, callback: (zoomLevel : Float) -> Unit){
		mRtcEngine?.setCameraZoomFactor(zoomLevel)
		callback.invoke(zoomLevel)
	}

/*	fun turnOffCamera(callback: (isOff: Boolean) -> Unit) {
		isCameraOff = !isCameraOff
		mRtcEngine?.muteLocalVideoStream(isCameraOff)
	}*/

	fun switchCamera(callback: (isSwitched: Boolean) -> Unit) {
		isSwitched = !isSwitched
		mRtcEngine?.switchCamera()
		callback.invoke(isSwitched)
	}

	fun muteAudio(callback: (isMuted: Boolean) -> Unit) {
		isMuted = !isMuted
		mRtcEngine?.muteLocalAudioStream(isMuted)
		callback(isMuted)
	}

	fun leaveChannel() {
		mRtcEngine?.leaveChannel()
	}

	fun destroyEngine() {
		log(TAG, "AGORA MANAGER DESTROYED")
		readyCallbacks.clear()
		if (mRtcEngine != null) {
			mRtcEngine?.stopPreview()
			mRtcEngine?.leaveChannel()
			mRtcEngine = null
			RtcEngine.destroy()
		}
		isInitialized = false
	}

	private fun videoConfig() = VideoEncoderConfiguration().also {
		it.advanceOptions?.compressionPreference = VideoEncoderConfiguration.COMPRESSION_PREFERENCE.PREFER_LOW_LATENCY
		it.advanceOptions?.encodingPreference = VideoEncoderConfiguration.ENCODING_PREFERENCE.PREFER_AUTO
		it.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
		it.degradationPrefer = VideoEncoderConfiguration.DEGRADATION_PREFERENCE.MAINTAIN_BALANCED
		it.mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_AUTO
		it.frameRate = VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24.value
		it.bitrate = VideoEncoderConfiguration.DEFAULT_MIN_BITRATE_EQUAL_TO_TARGET_BITRATE
		it.dimensions = VideoEncoderConfiguration.VD_640x480
	}

}