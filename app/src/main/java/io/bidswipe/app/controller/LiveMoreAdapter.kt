package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MoreOptionItemBinding
import io.bidswipe.app.databinding.OfferPriceItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl

class LiveMoreAdapter(
	mList: MutableList<LiveMoreOption>, val mClicks: RecyclerClicks
) : BaseAdapter<LiveMoreOption, MoreOptionItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		MoreOptionItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<MoreOptionItemBinding>,
		position: Int,
		item: LiveMoreOption?
	) {
		with(holder) {

            bind.title.text= item?.name
			bind.icon.setImageResource(item?.image!!)

			bind.root.setOnClickListener {
				mClicks.itemClick(position, item.name)
			}

		}
	}
}