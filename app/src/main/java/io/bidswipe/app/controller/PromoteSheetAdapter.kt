package io.bidswipe.app.controller

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PromoteItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PromoteShowModel
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.setHapticClickListener

class PromoteSheetAdapter(
	mList : MutableList<PromoteShowModel> , val mClicks : RecyclerClicks ,
) : BaseAdapter<PromoteShowModel? , PromoteItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		PromoteItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<PromoteItemBinding> ,
		position : Int ,
		item : PromoteShowModel? ,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
			bind.title.text = item?.title
			bind.subTitle.text = item?.subtitle
			bind.description.text = item?.description
			bind.titleIcon.setImageResource(item?.iconResId ?: R.drawable.ic_flash)
			bind.amount.setTextColor(ContextCompat.getColor(mCtx , item?.gradientColors?.first() ?: R.color.primary))
			bind.amount.text = buildString {
				append("Select")
				append(" • ")
				append(item?.price)
			}

			val gradientDrawable = GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT ,
				item?.gradientColors?.map { ContextCompat.getColor(mCtx , it) }?.toIntArray()
			)
			gradientDrawable.cornerRadius = mCtx.resources.dpToPx(16).toFloat()
			bind.mainLayout.background = gradientDrawable

		}
	}
}