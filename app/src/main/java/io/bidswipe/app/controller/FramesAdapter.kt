package io.bidswipe.app.controller

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.databinding.FrameImageItemBinding
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class FramesAdapter(
val	mList : MutableList<Bitmap?> =mutableListOf()
) : BaseAdapter<Bitmap? , FrameImageItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		FrameImageItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<FrameImageItemBinding> ,
		position : Int ,
		item : Bitmap? ,
	) {
		with(holder) {

			bind.image.layoutParams.apply {
				height=mCtx.resources.dpToPx(64)
//				width=mCtx.resources.dpToPx(48)
			}
			bind.image.setImageBitmap(item)
		}
	}

	fun submit(list: List<Bitmap>) {
		mList.clear()
		mList.addAll(list)
		notifyDataSetChanged()
	}

}
