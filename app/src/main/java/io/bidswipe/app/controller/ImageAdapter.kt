package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ImageAdapter(
    var mList: MutableList<String?>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, UploadImageItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        UploadImageItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<UploadImageItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            if (item != null) {
                val isVideo = isVideoFile(item)

                if (item.contains(Const.BASE_URL)) {
	                bind.image.isVisible = true
	                bind.videoView.isVisible = false
                    bind.image.loadUrl(mCtx, item)
                } else {
                    if (isVideo) {
                        bind.image.isVisible = false
                        val exoPlayer = ExoPlayer.Builder(mCtx).build()
                        bind.videoView.player = exoPlayer
                        val mediaItem = item.let { MediaItem.fromUri(it) }

                        exoPlayer.setMediaItem(mediaItem)
                        bind.videoView.isVisible = true
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = false

                    } else {
                        bind.image.isVisible = true
                        bind.videoView.isVisible = false
                        bind.image.setImageURI(item.toUri())
                    }
                }
            }

        }
    }

    private fun isVideoFile(path: String): Boolean {
        val videoExtensions = listOf(".mp4", ".mov", ".avi", ".mkv", ".3gp", ".webm")
        val lowerPath = path.lowercase()
        return videoExtensions.any { lowerPath.endsWith(it) }
    }
}