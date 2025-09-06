package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShareItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ShareSheetAdapter(
	mList : MutableList<String?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<String? , ShareItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ShareItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ShareItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

			bind.root.setOnClickListener {
				mClicks.itemClick(position)
			}


		}
	}
}