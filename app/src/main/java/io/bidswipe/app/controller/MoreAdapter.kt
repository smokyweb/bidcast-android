package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MenuItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.utils.setHapticClickListener

class MoreAdapter(
	val mList : MutableList<MoreModel> ,
	val mClicks : RecyclerClicks ,
) : BaseAdapter<MoreModel , MenuItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		MenuItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<MenuItemBinding> ,
		position : Int ,
		item : MoreModel? ,
	) {
		with(holder) {

			bind.title.text = item?.title

			bind.root.setHapticClickListener {

				mClicks.itemClick(position)

			}

		}
	}
}