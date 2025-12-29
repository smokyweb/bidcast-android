package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SubcategoryListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SubCategoryListAdapter(
	items: List<GetSubCategoriesResponse.Data.Subcategory?>,
	val mClicks: RecyclerClicks,
) : BaseAdapter<GetSubCategoriesResponse.Data.Subcategory?, SubcategoryListItemBinding>(items) {

	private val data = items.toMutableList()

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		SubcategoryListItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<SubcategoryListItemBinding>,
		position: Int,
		item: GetSubCategoriesResponse.Data.Subcategory??,
	) {
		with(holder) {
			bind.subcategoryTitle.text = item?.name
			bind.subcategoryIcon.loadUrl(mCtx, item?.image ?: "")
			
			bind.viewerCount.text = buildString {
				append(item?.let { "0" } ?: "0")
				append(" Viewers")
			}

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
		}
	}
}




