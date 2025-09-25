package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.GoodsItemBinding
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class GoodsExampleAdapter(
	mList : MutableList<String?> ,
) : BaseAdapter<String , GoodsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		GoodsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<GoodsItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {

			}

			bind.img.loadUrl(mCtx , item.toString())


		}
	}
}