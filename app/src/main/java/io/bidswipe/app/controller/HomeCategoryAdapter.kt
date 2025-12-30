package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HomeCategoryTileBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.loadUrl

class HomeCategoryAdapter(
    mList: MutableList<CategoryTile>,
    private val mClick: RecyclerClicks,
) : BaseAdapter<HomeCategoryAdapter.CategoryTile, HomeCategoryTileBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        HomeCategoryTileBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<HomeCategoryTileBinding>,
        position: Int,
        item: CategoryTile?,
    ) {
        with(holder) {
            bind.title.text = item?.title

            when (item?.tileType ?: TileType.CATEGORY) {
                TileType.FOR_YOU -> {
                    bind.icon.clearColorFilter()
                    if (item?.iconRes != null) bind.icon.setImageResource(item.iconRes)
//					bind.icon.setColorFilter(ContextCompat.getColor(mCtx, R.color.scrim))
                }

                TileType.CATEGORY -> {
                    bind.icon.clearColorFilter()
                    bind.icon.loadUrl(mCtx, item?.imageUrl ?: "", R.drawable.placeholder_square)
                }

                TileType.SEE_ALL -> {
                    if (item?.iconRes != null) bind.icon.setImageResource(item.iconRes)
//                    bind.icon.setColorFilter(ContextCompat.getColor(mCtx, R.color.onPrimary))
                }
            }

            if (item?.isSelected == true) {
                bind.root.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        mCtx,
                        R.drawable.category_selected_background
                    )
                )

            } else {

                bind.root.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        mCtx,
                        R.drawable.card_8
                    )
                )

            }

            bind.title.setTextColor(
                ContextCompat.getColor(
                    mCtx,
                    if (item?.isSelected == true) R.color.scrim else R.color.onSurface
                )
            )

            bind.root.setOnClickListener {
                mClick.itemClick(position)
            }
        }
    }

    data class CategoryTile(
        val id: String,
        val title: String,
        val imageUrl: String? = null,
        val iconRes: Int? = null,
        val tileType: TileType = TileType.CATEGORY,
        var isSelected: Boolean = false
    )

    enum class TileType {
        FOR_YOU,
        CATEGORY,
        SEE_ALL
    }

}

