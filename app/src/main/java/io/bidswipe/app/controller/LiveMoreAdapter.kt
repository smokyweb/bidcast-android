package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MoreOptionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.utils.setHapticClickListener

class LiveMoreAdapter(
	mList : MutableList<LiveMoreOption> , val mClicks : RecyclerClicks ,
) : BaseAdapter<LiveMoreOption , MoreOptionItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		MoreOptionItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<MoreOptionItemBinding> ,
		position : Int ,
		item : LiveMoreOption? ,
	) {
		with(holder) {

			bind.title.text = item?.name
			bind.icon.setImageResource(item?.image !!)

            bind.root.setHapticClickListener {
				mClicks.itemClick(position , item.name)
			}

		}
	}
}