package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CategoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SubCategoryAdapter(
	items : List<GetSubCategoriesResponse.Data.Subcategory?> ,
	val mClicks : RecyclerClicks ,
) : BaseAdapter<GetSubCategoriesResponse.Data.Subcategory , CategoryItemBinding>(items) {

	override fun bindView(
		inflater : LayoutInflater ,
		parent : ViewGroup ,
	) = CategoryItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<CategoryItemBinding> ,
		position : Int ,
		item : GetSubCategoriesResponse.Data.Subcategory? ,
	) {
		with(holder.bind) {
			root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			title.text = item?.name
			categoryImage.loadUrl(mCtx , item?.image ?: "")

			if (item?.isSelected == true) {
				main.setCardBackgroundColor(mCtx.getColor(R.color.primaryContainer))
				main.strokeColor = mCtx.getColor(R.color.primary)
				main.strokeWidth = mCtx.resources.dpToPx(2)
			} else {
				main.setCardBackgroundColor(mCtx.getColor(R.color.outline))
				main.strokeColor = mCtx.getColor(R.color.transparent)
				main.strokeWidth = 0
			}

		}
	}
}