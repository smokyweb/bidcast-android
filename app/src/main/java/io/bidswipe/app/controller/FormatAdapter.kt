package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.FormatItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.FormatModel
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.setHapticClickListener

class FormatAdapter(
	mList : MutableList<FormatModel> , val mClicks : RecyclerClicks ,
) : BaseAdapter<FormatModel , FormatItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		FormatItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<FormatItemBinding> ,
		position : Int ,
		item : FormatModel? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.icon.setImageDrawable(
				ContextCompat.getDrawable(
					mCtx ,
					item?.icon ?: R.drawable.notification
				)
			)
			bind.title.text = item?.title

			if (item?.selected == true) {
				bind.root.strokeWidth = mCtx.resources.dpToPx(2)
				bind.root.strokeColor = ContextCompat.getColor(mCtx , R.color.secondary)
			} else {
				bind.root.strokeWidth = mCtx.resources.dpToPx(0)

				bind.root.strokeColor = ContextCompat.getColor(mCtx , R.color.outline)
			}

		}
	}
}