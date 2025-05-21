package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SellSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel

class SellAdapter(
	mList: MutableList<SellModel>, val type: String, val mClicks: RecyclerClicks
) : BaseAdapter<SellModel, SellSheetItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		SellSheetItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<SellSheetItemBinding>,
		position: Int,
		item: SellModel?
	) {
		with(holder) {
			
			bind.root.setOnClickListener {
				mClicks.itemClick(position)
			}
			
			if (type == "getStarted" || type == "tips") {
				bind.next.isVisible = false
			}
			
			if (type == "shipping") {
				bind.root.background.setTint(ContextCompat.getColor(mCtx, R.color.background))
			}
			
			bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, item?.icon ?: R.drawable.notification))
			bind.iconCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, item?.color ?: R.color.primaryContainer))
			bind.subTitle.text = item?.subtitle
			bind.title.text = item?.title
			
		}
	}
}