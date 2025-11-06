/*
package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import com.millicast.Core
import com.millicast.Media
import com.millicast.Subscriber
import com.millicast.clients.ConnectionOptions
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.SubscriberConnectionState
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityDolbySubscribeBinding
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon

class DolbySubscribeActivity : BaseActivity() {

    companion object {
        const val EXTRA_STREAM_NAME = "extra_stream_name"
    }

    private val bind by bind(ActivityDolbySubscribeBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private lateinit var subscriber: Subscriber
    private lateinit var eglBase: EglBase
    private var subscriberStateJob: Job? = null
    val sourceVideoTracks: ArrayList<RemoteVideoTrack> = arrayListOf()
    var audioTrack: RemoteAudioTrack? = null
    private var streamName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        log("Subscriber Activity created")

        subscriber = Core.createSubscriber()
        log("Subscriber instance created")

        initRenderer()

        bind.startBtn.setOnClickListener {
            startSubscription()
        }

        // Resolve the stream name once
        streamName = intent?.getStringExtra(EXTRA_STREAM_NAME)?.takeIf { !it.isNullOrBlank() }
            ?: streamName
        log("Using streamName='${streamName}' (empty means fallback in credentials)")
    }

    private fun initRenderer() {
        // Prefer SDK-provided EGL context for subscriber per docs
        eglBase = EglBase.create()
        bind.remoteView.init(Media.eglBaseContext, null)
        bind.remoteView.setMirror(false)
//		bind.remoteView.setEnableHardwareScaler(true)
        bind.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        log("Renderer initialized")
    }

    private fun startSubscription() {
        viewModel.viewModelScope.launch {
            try {
                log("Starting subscription flow...")
                val credentials = Credential(
                    streamName = streamName.ifBlank { Const.ACCOUNT_ID },
                    accountId = Const.ACCOUNT_ID,
                    apiUrl = "https://director.millicast.com/api/director/subscribe"
                )

                subscriber.setCredentials(credentials)
                log("Credentials set. Connecting (autoReconnect=true)...")
                subscriber.connect(ConnectionOptions(autoReconnect = true))

                subscriberStateJob?.cancel()
                subscriberStateJob = viewModel.viewModelScope.launch {
                    // Connection state
                    subscriber.state
                        .map { it.connectionState }
                        .distinctUntilChanged()
                        .collect { state ->
                            log("Subscriber state: $state")
                            when (state) {
                                SubscriberConnectionState.Connected -> {
                                    log("Connected. Subscribing now...")
                                    subscriber.subscribe(Option())
                                    log("Subscribe invoked; awaiting remote tracks...")
                                }

                                SubscriberConnectionState.Subscribed -> {
                                    log("Subscriber state: Subscribed (media should start)")
                                }

                                else -> {}
                            }
                        }
                }

                // Log websocket and peer connection states
                viewModel.viewModelScope.launch {
                    subscriber.state.map { it.websocketConnectionState }.distinctUntilChanged()
                        .collect { ws ->
                            log("WebSocket state: $ws")
                        }
                }
                viewModel.viewModelScope.launch {
                    subscriber.state.map { it.peerConnectionState }.distinctUntilChanged()
                        .collect { pc ->
                            log("PeerConnection state: $pc")
                        }
                }
                // Log signaling errors if any
                viewModel.viewModelScope.launch {
                    subscriber.signalingError.collect { err ->
                        log("Signaling error: $err")
                    }
                }

                // Collect remote video/audio tracks
                viewModel.viewModelScope.launch {
                    subscriber.onRemoteTrack.collect { holder ->
                        when (holder) {
                            is RemoteVideoTrack -> {
                                log("RemoteVideoTrack: sourceId=${holder.sourceId}")
                                sourceVideoTracks.add(holder)
                                holder.enableAsync(videoSink = bind.remoteView)
                                viewModel.viewModelScope.launch {
                                    holder.onState.collect { trackState ->
                                        log("VideoTrack state mid=${trackState.mid} active=${trackState.isActive}")
                                        if (!trackState.isActive) holder.disableAsync() else holder.enableAsync(
                                            videoSink = bind.remoteView
                                        )
                                    }
                                }
                                // Add a tiny post-frame confirmation
                                bind.remoteView.postDelayed({
                                    log("Renderer (TextureViewRenderer) ready; awaiting frames...")
                                }, 300)
                            }

                            is RemoteAudioTrack -> {
                                log("RemoteAudioTrack: sourceId=${holder.sourceId}")
                                audioTrack = holder
                                holder.enableAsync()
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                log("SUBSCRIBE ERROR: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Subscriber cleanup starting")
        try {
            subscriberStateJob?.cancel()
        } catch (_: Throwable) {
        }
        try {
            sourceVideoTracks.forEach { it.disableAsync() }
        } catch (_: Throwable) {
        }
        try {
            audioTrack?.disableAsync()
        } catch (_: Throwable) {
        }
//		try {
//			bind.remoteView.clearImage()
//		} catch (_: Throwable) {
//		}
        try {
            viewModel.viewModelScope.launch { subscriber.unsubscribe(); subscriber.disconnect() }
        } catch (_: Throwable) {
        }
        try {
            bind.remoteView.release()
        } catch (_: Throwable) {
        }
        try {
            eglBase.release()
        } catch (_: Throwable) {
        }
        log("Subscriber cleanup finished")
    }
}


*/
