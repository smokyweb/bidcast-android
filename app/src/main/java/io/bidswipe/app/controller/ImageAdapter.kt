package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import java.io.File

class ImageAdapter(
	mList : MutableList<String?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<String? , UploadImageItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		UploadImageItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<UploadImageItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			if (item != null) {
				val isVideo = isVideoFile(item)
				
				// Show video indicator if it's a video
				// You may need to add a video indicator view in the layout
				// For now, we'll just load the thumbnail/image
				
				if (item.contains(Const.BASE_URL)) {
					bind.image.loadUrl(mCtx , item)
				} else {
					if (isVideo) {
						// For videos, you might want to show a video thumbnail
						// For now, just show a placeholder or video icon
						// You can use MediaMetadataRetriever to get video thumbnail
						bind.image.setImageURI(item.toUri())
					} else {
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