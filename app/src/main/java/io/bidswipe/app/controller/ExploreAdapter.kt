package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ExploreItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ExploreAdapter(
	val mList : MutableList<GetCategoryResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetCategoryResponse.Data? , ExploreItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ExploreItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ExploreItemBinding> ,
		position : Int ,
		item : GetCategoryResponse.Data?? ,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.title.text = item?.name
			bind.icon.loadUrl(mCtx , item?.image ?: "")
			bind.iconCard.setCardBackgroundColor(
				item?.color?.toColorInt() ?: ContextCompat.getColor(mCtx , R.color.primary)
			)

		}
	}
}