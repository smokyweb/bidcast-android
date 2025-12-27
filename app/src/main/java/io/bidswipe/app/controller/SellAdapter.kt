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
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener

class SellAdapter(mList : MutableList<SellModel> , val type : String , val mClicks : RecyclerClicks , ) : BaseAdapter<SellModel , SellSheetItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) = SellSheetItemBinding.inflate(inflater , parent , false)

	override fun onBind(holder : BaseViewHolder<SellSheetItemBinding> , position : Int , item : SellModel? , ) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			if (type == "getStarted" || type == "tips" || type == "affiliate") {
				bind.next.isVisible = false
			}

			if (type == "shipping") {
				bind.root.background.setTint(ContextCompat.getColor(mCtx , R.color.background))
			}

			if (type == "shipping" && position == 0){
				bind.status.isVisible = true
				bind.status.text = item?.status
			}else{
				bind.status.isVisible = false
			}

			bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx , item?.icon ?: draw.ic_add_outline))
			bind.subTitle.text = item?.subtitle
			bind.title.text = item?.title
		}
	}
}