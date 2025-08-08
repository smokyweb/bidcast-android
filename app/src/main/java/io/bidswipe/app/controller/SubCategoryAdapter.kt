package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SubCategoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.loadUrl

class SubCategoryAdapter(
    items: List<GetSubCategoriesResponse.Data?>,
    val mClicks: RecyclerClicks,
) : BaseAdapter<GetSubCategoriesResponse.Data, SubCategoryItemBinding>(items) {

    private val selectedPositions = mutableSetOf<Int>()
    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ) = SubCategoryItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SubCategoryItemBinding>,
        position: Int,
        item: GetSubCategoriesResponse.Data?,
    ) {
        with(holder.bind) {
            title.text = item?.name
            categoryImage.loadUrl(mCtx, item?.image ?: "")

            root.isSelected = selectedPositions.contains(position)

            root.elevation = if (selectedPositions.contains(position)) 16f else 0f
            main.strokeWidth = if (selectedPositions.contains(position)) 4 else 0
            main.strokeColor =
                if (selectedPositions.contains(position)) mCtx.getColor(R.color.primary) else mCtx.getColor(
                    R.color.tertiaryContainer
                )

            root.setOnClickListener {
                mClicks.itemClick(position, null)
                toggleSelection(position)
            }
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)

    }
}