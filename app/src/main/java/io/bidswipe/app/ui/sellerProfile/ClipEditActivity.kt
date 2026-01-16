package io.bidswipe.app.ui.sellerProfile

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityClipEditBinding
import io.bidswipe.app.utils.bind

class ClipEditActivity : BaseActivity() {

    private val bind by bind(ActivityClipEditBinding::inflate)
    private val viewModel by viewModels<SellerViewModel>()

    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var isEdit: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContentView(bind.root)

        videoUrl = intent?.getStringExtra("videoUrl") ?: ""
        log("URL $videoUrl")
        isEdit = intent?.getBooleanExtra("isEdit", false)

        if (isEdit == true) {
            bind.header.setHeaderText("Edit Clip")
        } else {
            bind.header.setHeaderText("Show Clip")
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

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()

            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()

            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        bind.loader.isVisible = false
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    bind.loader.isVisible = false
                    errorToast("Failed to load video: ${error.message}")
                }
            })

            exoPlayer?.play()
        }
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

}