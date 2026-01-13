package io.bidswipe.app.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.databinding.ExploreItemBinding
import io.bidswipe.app.databinding.SubcategoryExpandedRowBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

// Sealed class to represent different item types
sealed class ExploreItem {
    data class CategoryItem(val data: GetCategoryResponse.Data?) : ExploreItem()
    data class SubcategoryRowItem(val subcategories: List<GetSubCategoriesResponse.Data.Subcategory?>) : ExploreItem()
}

class ExploreAdapter(
    val mList: MutableList<GetCategoryResponse.Data?>,
    val mClicks: RecyclerClicks,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_SUBCATEGORY_ROW = 1
    }

    private lateinit var mCtx: Context

    private var selectedPosition: Int = -1
    private var subcategories: List<GetSubCategoriesResponse.Data.Subcategory?> = emptyList()

    private var subcategoryAdapter: SubCategoryListAdapter? = null

    // 🔥 Cached display list
    private val displayItems = mutableListOf<ExploreItem>()

    /* ---------------- DISPLAY LIST ---------------- */

    private fun rebuildDisplayItems() {
        displayItems.clear()

        val count = if (App.mCtx.resources.isTablet()) 4 else 3
        val subcategoryInsertAfterIndex = if (selectedPosition != -1 && subcategories.isNotEmpty()) {

            val rowNumber = selectedPosition / count
            val endOfRowIndex = (rowNumber + 1) * count - 1
            minOf(endOfRowIndex, mList.size - 1)
        } else {
            -1
        }

        mList.forEachIndexed { index, category ->
            displayItems.add(ExploreItem.CategoryItem(category))
            // Insert subcategory row after the row containing selected category is complete
            if (index == subcategoryInsertAfterIndex && subcategoryInsertAfterIndex != -1) {
                displayItems.add(ExploreItem.SubcategoryRowItem(subcategories))
            }
        }

        // Edge case: If selected category is at the very end and we haven't inserted yet
        if (selectedPosition != -1 && subcategories.isNotEmpty() &&
            selectedPosition == mList.size - 1 &&
            subcategoryInsertAfterIndex == mList.size - 1 &&
            displayItems.lastOrNull() !is ExploreItem.SubcategoryRowItem
        ) {
            displayItems.add(ExploreItem.SubcategoryRowItem(subcategories))
        }
    }

    /* ---------------- ADAPTER OVERRIDES ---------------- */

    override fun getItemCount(): Int {
        rebuildDisplayItems()
        return displayItems.size
    }

    override fun getItemViewType(position: Int): Int {
        rebuildDisplayItems()
        return when (displayItems[position]) {
            is ExploreItem.CategoryItem -> VIEW_TYPE_CATEGORY
            is ExploreItem.SubcategoryRowItem -> VIEW_TYPE_SUBCATEGORY_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mCtx = parent.context
        return when (viewType) {
            VIEW_TYPE_CATEGORY ->
                CategoryViewHolder(
                    ExploreItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )

            VIEW_TYPE_SUBCATEGORY_ROW ->
                SubcategoryRowViewHolder(
                    SubcategoryExpandedRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )

            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is ExploreItem.CategoryItem -> {
                val categoryIndex = getCategoryIndex(position)
                (holder as CategoryViewHolder).bind(item.data, categoryIndex)
            }

            is ExploreItem.SubcategoryRowItem -> {
                val categoryIndex = getCategoryIndex(position)
                (holder as SubcategoryRowViewHolder).bind(item.subcategories, categoryIndex)
            }
        }
    }

    /* ---------------- POSITION MAPPING ---------------- */

    private fun getCategoryIndex(displayPosition: Int): Int {
        rebuildDisplayItems()
        if (displayPosition >= displayItems.size) {
            return mList.size - 1
        }

        when (val item = displayItems[displayPosition]) {
            is ExploreItem.CategoryItem -> {
                // Count how many category items come before this position
                var categoryCount = 0
                for (i in 0 until displayPosition) {
                    if (displayItems[i] is ExploreItem.CategoryItem) {
                        categoryCount++
                    }
                }
                return categoryCount
            }

            is ExploreItem.SubcategoryRowItem -> {
                // Subcategory row belongs to the selected category
                return selectedPosition
            }
        }
    }

    /* ---------------- VIEW HOLDERS ---------------- */

    inner class CategoryViewHolder(val bind: ExploreItemBinding) :
        RecyclerView.ViewHolder(bind.root) {

        fun bind(item: GetCategoryResponse.Data?, categoryIndex: Int) {

            bind.title.text = item?.name
            bind.subTitle.text = "${item?.liveCount ?: "0"} Viewers"
            bind.icon.loadUrl(mCtx, item?.image ?: "")

            val isSelected = selectedPosition == categoryIndex

            if (isSelected) {
                bind.root.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        mCtx,
                        R.drawable.category_selected_background
                    )
                )
            } else {
                bind.root.setCardBackgroundColor(mCtx.getColor(R.color.background))
                bind.root.strokeColor = mCtx.getColor(R.color.transparent)
                bind.root.strokeWidth = 0
            }

            bind.click.setHapticClickListener {
                mClicks.itemClick(categoryIndex)
            }

        }
    }

    inner class SubcategoryRowViewHolder(val bind: SubcategoryExpandedRowBinding) :
        RecyclerView.ViewHolder(bind.root) {

        fun bind(
            subcategories: List<GetSubCategoriesResponse.Data.Subcategory?>,
            categoryIndex: Int
        ) {
            if (bind.subcategoryRecyclerView.layoutManager == null) {
                bind.subcategoryRecyclerView.layoutManager = LinearLayoutManager(mCtx)
            }

            // Always create a fresh adapter to ensure data is updated
            // This is necessary because RecyclerView might reuse ViewHolders
            val newAdapter = SubCategoryListAdapter(
                items = subcategories,
                mClicks = object : RecyclerClicks {
                    override fun itemClick(pos: Int, status: String?) {
                        mClicks.itemClick(categoryIndex, pos.toString())
                    }
                }
            )

            // Always set the adapter to ensure it's updated with latest data
            bind.subcategoryRecyclerView.adapter = newAdapter
            subcategoryAdapter = newAdapter
        }
    }

    /* ---------------- PUBLIC API (UNCHANGED) ---------------- */

    fun setSelectedPosition(
        position: Int,
        subcategories: List<GetSubCategoriesResponse.Data.Subcategory?>
    ) {
        selectedPosition = position
        this.subcategories = subcategories
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun getSubcategoryAt(position: Int): GetSubCategoriesResponse.Data.Subcategory? =
        subcategories.getOrNull(position)

    fun getAllSubCategories()=subcategories

    fun clearSelection() {
        selectedPosition = -1
        subcategories = emptyList()
        subcategoryAdapter = null
        notifyDataSetChanged()
    }
}

