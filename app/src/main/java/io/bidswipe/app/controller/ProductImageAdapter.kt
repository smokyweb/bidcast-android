package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.LiveCommentItemBinding
import io.bidswipe.app.databinding.ProductImageItemBinding
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl

class ProductImageAdapter(
	mList : MutableList<String?>
) : BaseAdapter<String , ProductImageItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ProductImageItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ProductImageItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {
			bind.image.loadUrl(mCtx,item?:"")
		}
	}
}