package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SubcategoryRecyclerItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.loadUrl

class SubCategoryRecyclerAdapter(
	items: List<GetSubCategoriesResponse.Data?>,
	val mClicks: RecyclerClicks,
) : BaseAdapter<GetSubCategoriesResponse.Data, SubcategoryRecyclerItemBinding>(items) {
	override fun bindView(
		inflater: LayoutInflater,
		parent: ViewGroup,
	) = SubcategoryRecyclerItemBinding.inflate(inflater, parent, false)

	lateinit var subCategoryAdapter: SubCategoryAdapter

	override fun onBind(
		holder: BaseViewHolder<SubcategoryRecyclerItemBinding>,
		position: Int,
		item: GetSubCategoriesResponse.Data?,
	) {
		with(holder.bind) {
			heading.text = item?.name
			headingImage.loadUrl(mCtx, item?.image ?: "")

			root.setOnClickListener {
				mClicks.itemClick(position, null)
			}

			subCategoryAdapter = SubCategoryAdapter(item?.subcategories ?: mutableListOf(), object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {
					mClicks.itemClick(position, pos.toString())
				}
			})
			recyclerView.adapter = subCategoryAdapter
		}
	}

}