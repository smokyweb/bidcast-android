package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductImageItemBinding
import io.bidswipe.app.utils.loadUrl

class ProductImageAdapter(
    mList: MutableList<String?>
) : BaseAdapter<String, ProductImageItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductImageItemBinding.inflate(inflater, parent, false)


    private val playerInstances = mutableMapOf<Int, ExoPlayer>()
    override fun onBind(
        holder: BaseViewHolder<ProductImageItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {
            val isVideo = isVideoFile(item ?: "")
            val newPlayer = ExoPlayer.Builder(mCtx).build()
            if (isVideo) {
                bind.imageView.isVisible = false
                bind.videoView.isVisible = true

                // Release any existing player for this position
                playerInstances[position]?.let { existingPlayer ->
                    existingPlayer.stop()
                    existingPlayer.release()
                }


                playerInstances[position] = newPlayer
                bind.videoView.player = newPlayer
                val newMediaItem = MediaItem.fromUri(item.toString())

                newPlayer.setMediaItem(newMediaItem)
                newPlayer.prepare()
                newPlayer.playWhenReady = false

                bind.videoView.useController = false
                newPlayer.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.height > 0) {
                            bind.videoView.post {
                                adjustPlayerViewSize(videoSize.width, videoSize.height, holder)
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_ENDED -> {
                                newPlayer.seekTo(0) // Loop the video
                                newPlayer.playWhenReady = true
                            }

                            Player.STATE_BUFFERING -> {

                                bind.videoView.useController = false
                            }

                            Player.STATE_READY -> {
                                bind.videoView.useController = true
                            }

                            Player.STATE_IDLE -> {
                            }
                        }
                    }
                })

            } else {
                newPlayer.pause()
                bind.imageView.isVisible = true
                bind.videoView.isVisible = false
                bind.imageView.loadUrl(mCtx, item ?: "")
            }

        }
    }

    private fun isVideoFile(path: String): Boolean {
        val videoExtensions = listOf(".mp4", ".mov", ".avi", ".mkv", ".3gp", ".webm")
        val lowerPath = path.lowercase()
        return videoExtensions.any { lowerPath.endsWith(it) }
    }


    private fun adjustPlayerViewSize(
        videoWidth: Int,
        videoHeight: Int,
        holder: BaseViewHolder<ProductImageItemBinding>
    ) {
        if (videoWidth == 0 || videoHeight == 0) return

        val container = holder.bind.videoView.parent as View
        val containerWidth = container.width
        if (containerWidth == 0) return // wait until container is measured

        val layoutParams = holder.bind.videoView.layoutParams

        val calculatedHeight =
            (videoHeight.toFloat() / videoWidth.toFloat() * containerWidth).toInt()

        layoutParams.width = containerWidth
        layoutParams.height = calculatedHeight

        holder.bind.videoView.layoutParams = layoutParams
    }

    // Lifecycle methods for ExoPlayer management
    fun onPause() {
        playerInstances.values.forEach { player ->
            player.pause()
        }
    }

    fun onResume() {
        playerInstances.values.forEach { player ->
            if (player.playbackState != Player.STATE_ENDED) {
                player.playWhenReady = true
            }
        }
    }

    fun onDestroy() {
        playerInstances.values.forEach { player ->
            player.stop()
            player.release()
        }
        playerInstances.clear()
    }


}