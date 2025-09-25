package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.GridItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.utils.setHapticClickListener

class GridAdapter(
	mList : MutableList<MoreModel> , val mClicks : RecyclerClicks ,
) : BaseAdapter<MoreModel , GridItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		GridItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<GridItemBinding> ,
		position : Int ,
		item : MoreModel? ,
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