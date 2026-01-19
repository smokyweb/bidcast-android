package io.bidswipe.app.ui.sellerProfile

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.slider.RangeSlider
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.FramesAdapter
import io.bidswipe.app.databinding.ActivityClipEditBinding
import io.bidswipe.app.utils.bind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClipEditActivity : BaseActivity() {

    private val bind by bind(ActivityClipEditBinding::inflate)
    private val viewModel by viewModels<SellerViewModel>()

    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var isEdit: Boolean? = false

    private var videoDurationMs: Long = 0L
    private var trimStartMs: Long = 0L
    private var trimEndMs: Long = 0L
    private var isTrimSliderInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        bind.header.onBackClick { finishAfterTransition() }

        videoUrl = intent?.getStringExtra("videoUrl") ?: ""
        isEdit = intent?.getBooleanExtra("isEdit", false)

        bind.header.setHeaderText(
            if (isEdit == true) "Edit Clip" else "Show Clip"
        )

        if (isEdit == true) {
            bind.trimLayout.isVisible = true

            val adapter = FramesAdapter()
            bind.frameRecycler.adapter = adapter
            bind.frameRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

            lifecycleScope.launch(Dispatchers.IO) {
                val frames = extractFrames(videoUrl!!, videoDurationMs)
                withContext(Dispatchers.Main) {
                    adapter.submit(frames)
                }
            }
        }

        if (!videoUrl.isNullOrEmpty()) {
            initializePlayer()
        } else {
            errorToast("Video URL not found")
            finishAfterTransition()
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        videoUrl?.let { url ->
            bind.loader.isVisible = true

            exoPlayer = ExoPlayer.Builder(this).build()
            bind.playerView.player = exoPlayer
            bind.playerView.useController = true
            bind.playerView.setShowSubtitleButton(false)

            exoPlayer?.setMediaItem(MediaItem.fromUri(url))
            exoPlayer?.prepare()

            exoPlayer?.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        bind.loader.isVisible = false

                        videoDurationMs = exoPlayer?.duration ?: return
                        trimStartMs = 0L
                        trimEndMs = videoDurationMs

                        if (!isTrimSliderInitialized && isEdit == true) {
                            setupTrimSlider()
                            isTrimSliderInitialized = true
                        }

                        exoPlayer?.play()
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if ((exoPlayer?.currentPosition ?: 0L) >= trimEndMs) {
                        exoPlayer?.pause()
                        exoPlayer?.seekTo(trimStartMs)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    bind.loader.isVisible = false
                    errorToast("Failed to load video: ${error.message}")
                }
            })
        }
    }

    private fun setupTrimSlider() {

        videoDurationMs = exoPlayer?.duration ?: return

        bind.trimSlider.valueFrom = 0f
        bind.trimSlider.valueTo = videoDurationMs.toFloat()
        bind.trimSlider.values = listOf(0f, videoDurationMs.toFloat())
        bind.trimSlider.stepSize = 0f

        // Track changes while dragging
        bind.trimSlider.addOnChangeListener { slider, _, fromUser ->
            if (!fromUser) return@addOnChangeListener

            trimStartMs = slider.values[0].toLong()
            trimEndMs = slider.values[1].toLong()
        }

        // Seek only when user releases slider
        bind.trimSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {

            override fun onStartTrackingTouch(slider: RangeSlider) {}

            override fun onStopTrackingTouch(slider: RangeSlider) {
                trimStartMs = slider.values[0].toLong()
                trimEndMs = slider.values[1].toLong()

                exoPlayer?.seekTo(trimStartMs)
                exoPlayer?.play()
            }
        })

        // Prevent playback beyond trim end
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if ((exoPlayer?.currentPosition ?: 0) >= trimEndMs) {
                    exoPlayer?.pause()
                }
            }
        })
    }


    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun extractFrames(
        videoUrl: String,
        durationMs: Long
    ): List<Bitmap> {

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoUrl, HashMap())

        val frames = mutableListOf<Bitmap>()

        val frameCount = 12 // perfect for 1 min
        val interval = durationMs / frameCount

        for (i in 0 until frameCount) {
            val timeUs = (i * interval) * 1000
            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )?.let { frames.add(it) }
        }

        retriever.release()
        return frames
    }

}
