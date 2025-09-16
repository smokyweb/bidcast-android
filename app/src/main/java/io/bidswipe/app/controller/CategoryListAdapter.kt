package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CategoryListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.utils.setHapticClickListener

class CategoryListAdapter(
	mList : MutableList<GetCategoryResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetCategoryResponse.Data? , CategoryListItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		CategoryListItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<CategoryListItemBinding> ,
		position : Int ,
		item : GetCategoryResponse.Data? ,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.category.text = item?.name

		}
	}
}