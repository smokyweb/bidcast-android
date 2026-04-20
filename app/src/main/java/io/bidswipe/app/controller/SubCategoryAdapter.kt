package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.databinding.CategoryItemBinding
import io.bidswipe.app.databinding.HomeCategoryTileBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SubCategoryAdapter(
    items: List<GetSubCategoriesResponse.Data.Subcategory?>,
    private val mClicks: RecyclerClicks,
    private val type:String? = "home"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val EXPLORE_SUB_CATEGORY = 1
    }

    private val itemList = items.toMutableList()

    override fun getItemCount() = itemList.size

    override fun getItemViewType(position: Int): Int {
        return if (type == "home") {
            TYPE_NORMAL
        } else {
            EXPLORE_SUB_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            EXPLORE_SUB_CATEGORY -> {
                val binding =
                    HomeCategoryTileBinding.inflate(inflater, parent, false)
                ExploreViewHolder(binding)
            }

            else -> {
                val binding =
                    CategoryItemBinding.inflate(inflater, parent, false)
                NormalViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]

        when (holder) {
            is NormalViewHolder -> holder.bind(item)
            is ExploreViewHolder -> holder.bind(item)
        }
    }

    inner class NormalViewHolder(
        private val binding: CategoryItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GetSubCategoriesResponse.Data.Subcategory?) {
            with(binding) {
                title.text = item?.name
                categoryImage.loadUrl(root.context, item?.image ?: "")

                if (item?.isSelected == true) {
                    main.setCardBackgroundColor(root.context.getColor(R.color.primaryContainer))
                    main.strokeColor = root.context.getColor(R.color.primary)
                    main.strokeWidth = root.context.resources.dpToPx(2)
                } else {
                    main.setCardBackgroundColor(root.context.getColor(R.color.background))
                    main.strokeColor = root.context.getColor(R.color.transparent)
                    main.strokeWidth = 0
                }

                root.setHapticClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        mClicks.itemClick(pos)
                    }
                }
            }
        }
    }

    inner class ExploreViewHolder(
        private val binding: HomeCategoryTileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GetSubCategoriesResponse.Data.Subcategory?) {
            with(binding) {
                title.text = item?.name
                icon.loadUrl(root.context, item?.image ?: "")

                if (item?.isSelected == true) {
                    root.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.category_selected_background
                        )
                    )

                } else {

                    root.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            root.context,
                            R.drawable.card_8
                        )
                    )

                }
                root.setHapticClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        mClicks.itemClick(pos)
                    }
                }
            }
        }
    }
}
