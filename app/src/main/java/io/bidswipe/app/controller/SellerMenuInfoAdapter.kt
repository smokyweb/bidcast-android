package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SellerInfoItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.utils.setHapticClickListener

class SellerMenuInfoAdapter (
	val mList : MutableList<MoreModel>,
	val mClicks : RecyclerClicks,
) : BaseAdapter<MoreModel, SellerInfoItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater, parent : ViewGroup) = SellerInfoItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SellerInfoItemBinding> ,
		position : Int ,
		item : MoreModel? ,
	) {
		with(holder) {

			bind.title.text = item?.title

			bind.icon.setImageResource(item?.icon!!)

			bind.root.setHapticClickListener {

				mClicks.itemClick(position)

			}

		}
	}
}