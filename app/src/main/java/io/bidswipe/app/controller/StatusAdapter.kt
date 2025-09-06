package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.StatusItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class StatusAdapter(
	mList : MutableList<String> , val mClicks : RecyclerClicks ,
) : BaseAdapter<String , StatusItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		StatusItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<StatusItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

			bind.status.text = item
			bind.title.text = "Marketplace Vender Status"
			bind.subTitle.text = "Seller Rating: 4.8/5"
			bind.icon.setImageResource(R.drawable.ic_shop)
			bind.root.setOnClickListener {
				mClicks.itemClick(position , item)
			}


		}
	}
}