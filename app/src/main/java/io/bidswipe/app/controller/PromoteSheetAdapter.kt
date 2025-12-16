package io.bidswipe.app.controller

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PromoteItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class PromoteSheetAdapter(
	mList: MutableList<GetPromotePlansResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetPromotePlansResponse.Data?, PromoteItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		PromoteItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<PromoteItemBinding>,
		position: Int,
		item: GetPromotePlansResponse.Data?,
	) {
		with(holder) {
			
			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
			
			bind.title.text = item?.title
			bind.subTitle.text = item?.subTitle
			bind.description.text = item?.description
			bind.titleIcon.loadUrl(mCtx, item?.icon ?: "", R.drawable.ic_flash)
//			bind.amount.setTextColor((item?.colors?.start?.toColorInt() ?: ContextCompat.getColor(mCtx, R.color.boost_full_start)))
			
			bind.amount.text = buildString {
				append("Promote Show")
				append(" • ")
				append(item?.price?.asMoney())
			}
			
//			val gradientDrawable = GradientDrawable(
//				GradientDrawable.Orientation.LEFT_RIGHT,
//				intArrayOf(
//					item?.colors?.start?.toColorInt() ?: ContextCompat.getColor(mCtx, R.color.boost_full_start),
//					item?.colors?.end?.toColorInt() ?: ContextCompat.getColor(mCtx, R.color.boost_full_end)
//				)
//			)
//
//			gradientDrawable.cornerRadius = mCtx.resources.dpToPx(16).toFloat()
//			bind.mainLayout.background = gradientDrawable
			
		}
	}
}