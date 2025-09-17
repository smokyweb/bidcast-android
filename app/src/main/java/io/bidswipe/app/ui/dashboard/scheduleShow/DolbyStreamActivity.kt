package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import com.millicast.Core
import com.millicast.Media
import com.millicast.Media.audioSources
import com.millicast.Media.videoSources
import com.millicast.Publisher
import com.millicast.devices.source.audio.MicrophoneAudioSource
import com.millicast.devices.source.video.CameraVideoSource
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import com.millicast.publishers.Credential
import com.millicast.publishers.Option
import com.millicast.publishers.state.PublisherConnectionState
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityDolbyStreamBinding
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon

class DolbyStreamActivity : BaseActivity() {

	private val bind by bind(ActivityDolbyStreamBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()

	private lateinit var publisher: Publisher
	private lateinit var eglBase: EglBase

	private var audioSource: MicrophoneAudioSource? = null
	private var videoSource: CameraVideoSource? = null
	private var audioTrack: AudioTrack? = null
	private var videoTrack: VideoTrack? = null
	private var publisherStateJob: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		publisher = Core.createPublisher()

		initRenderer()

		bind.startBtn.setOnClickListener {
			connectPublisher()
		}

	}


	private fun initRenderer() {
		eglBase = EglBase.create()
		bind.remoteView.init(eglBase.eglBaseContext, null)
		bind.remoteView.setMirror(false)
		bind.remoteView.setEnableHardwareScaler(true)
		bind.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
	}

	private fun connectPublisher() {
		// Start preparing media sources (mic + camera)
		readyPublishingSources { aTrack, vTrack ->

			log("CONNECTING PUBLISHER")

			if (aTrack == null && vTrack == null) {
				log("Failed to initialize audio/video tracks.")
				return@readyPublishingSources
			}

			viewModel.viewModelScope.launch {
				try {
					log("CONNECTING PUBLISHER")
					aTrack?.let { publisher.addTrack(it) }
					vTrack?.let {
						publisher.addTrack(it)
						// Show local preview
						it.setVideoSink(bind.remoteView)
					}

					val credentials = Credential(
						streamName = Const.ACCOUNT_ID,
						token = Const.PUBLISHING_TOKEN,
						apiUrl = "https://director.millicast.com/api/director/publish"
					)

					publisher.setCredentials(credentials)

					publisher.connect()

					// Publish once connected
					publisherStateJob?.cancel()
					publisherStateJob = viewModel.viewModelScope.launch {
						publisher.state
							.map { it.connectionState }
							.distinctUntilChanged()
							.collect { state ->
								if (state == PublisherConnectionState.Connected) {
									val videoCodecs = Media.supportedVideoCodecs
									val audioCodecs = Media.supportedAudioCodecs
									val options = Option(
										videoCodec = videoCodecs.firstOrNull(),
										audioCodec = audioCodecs.firstOrNull(),
										dtx = true,
										stereo = true
									)
									publisher.publish(options)
								}
							}
					}
				} catch (e: Exception) {
					log("CONNECTING PUBLISHER ERROR : ${e.localizedMessage}")
					e.printStackTrace()
				}
			}
		}
	}

	private fun readyPublishingSources(callback: (AudioTrack?, VideoTrack?) -> Unit) {
		audioTrack = try {
			audioSource = audioSources<MicrophoneAudioSource>().firstOrNull()
			audioSource?.startCapture()
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}

		videoTrack = try {
			videoSource = videoSources<CameraVideoSource>().first()

			val capabilities = videoSource?.capabilities ?: emptyList()
			if (capabilities.isNotEmpty()) {
				// Prefer a reasonable preview size to avoid giant frames
				val preferred = capabilities.firstOrNull { it.width <= 1280 && it.height <= 720 } ?: capabilities.last()
				videoSource?.setCapability(preferred)
			}

			videoSource?.startCapture()
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}

		log("AUDIO TRACK : ${audioTrack?.name} || VIDEO TRACK : ${videoTrack?.name}")

		callback(audioTrack, videoTrack)
	}

	override fun onDestroy() {
		super.onDestroy()
		try { publisherStateJob?.cancel() } catch (_: Throwable) {}
		try { videoTrack?.removeVideoSink(bind.remoteView) } catch (_: Throwable) {}
		try { bind.remoteView.clearImage() } catch (_: Throwable) {}
		try { audioSource?.stopCapture(); audioSource?.release() } catch (_: Throwable) {}
		try { videoSource?.stopCapture(); videoSource?.release() } catch (_: Throwable) {}
		try { viewModel.viewModelScope.launch { publisher.unpublish(); publisher.disconnect() } } catch (_: Throwable) {}
		try { bind.remoteView.release() } catch (_: Throwable) {}
		try { eglBase.release() } catch (_: Throwable) {}
	}

}