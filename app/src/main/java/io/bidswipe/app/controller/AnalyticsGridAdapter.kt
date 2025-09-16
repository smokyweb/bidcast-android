package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.utils.setHapticClickListener

class AnalyticsGridAdapter(
	mList : MutableList<SellModel> , val mClicks : RecyclerClicks ,
) : BaseAdapter<SellModel , BenifitsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		BenifitsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<BenifitsItemBinding> ,
		position : Int ,
		item : SellModel? ,
	) {
		with(holder) {

			bind.title.text = item?.title ?: ""
			bind.description.text = item?.subtitle ?: ""

			bind.image.setImageDrawable(
				ContextCompat.getDrawable(
					mCtx ,
					item?.icon ?: R.drawable.notification
				)
			)

			bind.image.setBackgroundColor(
				ContextCompat.getColor(
					mCtx ,
					item?.color ?: R.color.primaryContainer
				)
			)

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

		}
	}
}