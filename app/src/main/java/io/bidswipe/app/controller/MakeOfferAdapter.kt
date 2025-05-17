package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.OfferPriceItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.asMoney

class MakeOfferAdapter(
	mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, OfferPriceItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		OfferPriceItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<OfferPriceItemBinding>,
		position: Int,
		item: String?
	) {
		with(holder) {
			
			bind.root.setOnClickListener {
				mClicks.viewClick(position)
			}
			bind.amount.text = item?.asMoney()
		}
	}
}