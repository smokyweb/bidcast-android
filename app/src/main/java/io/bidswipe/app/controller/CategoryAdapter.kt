package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CategoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.utils.loadUrl

class CategoryAdapter(
    items: List<GetCategoryResponse.Data?>,
    val mClicks: RecyclerClicks
) : BaseAdapter<GetCategoryResponse.Data,CategoryItemBinding>(items) {

    private val selectedPositions = mutableSetOf<Int>()

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        CategoryItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<CategoryItemBinding>,
        position: Int,
        item: GetCategoryResponse.Data?
    ) {
        with(holder.bind) {
            title.text = item?.name
            categoryImage.loadUrl(mCtx,item?.image ?: "")

            root.isSelected = selectedPositions.contains(position)

            root.elevation = if (selectedPositions.contains(position)) 8f else 0f
            main.strokeWidth = if (selectedPositions.contains(position)) 2 else 0
            main.strokeColor = if (selectedPositions.contains(position)) mCtx.getColor(R.color.primary) else mCtx.getColor(R.color.tertiaryContainer)

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