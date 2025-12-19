package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.GridItemBinding
import io.bidswipe.app.databinding.SellerToolsInnerItemBinding
import io.bidswipe.app.databinding.SellerToolsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.model.SellerToolModel
import io.bidswipe.app.utils.setHapticClickListener

class SellerToolsAdapter(
	mList : MutableList<SellerToolModel>, val mClicks : RecyclerClicks,
) : BaseAdapter<SellerToolModel , SellerToolsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SellerToolsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SellerToolsItemBinding> ,
		position : Int ,
		item : SellerToolModel? ,
	) {
		with(holder) {

			bind.title.text = item?.title ?: ""

			bind.gridRecycler.adapter= SellerToolsInnerAdapter(item?.list ?: mutableListOf() , object: RecyclerClicks{
				override fun itemClick(pos: Int, status: String?) {

						mClicks.itemClick(position,pos.toString())

				}

			}
			)

		}
	}
}


class SellerToolsInnerAdapter(
	mList : MutableList<MoreModel>, val mClicks : RecyclerClicks,
) : BaseAdapter<MoreModel , SellerToolsInnerItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SellerToolsInnerItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SellerToolsInnerItemBinding>,
		position : Int,
		item : MoreModel?,
	) {
		with(holder) {

			bind.title.text = item?.title ?: ""

			bind.image.setImageDrawable(
				ContextCompat.getDrawable(
					mCtx ,
					item?.icon ?: R.drawable.notification
				)
			)

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

		}
	}
}

