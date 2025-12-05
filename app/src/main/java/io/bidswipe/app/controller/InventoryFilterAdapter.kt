package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryFilterItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class InventoryFilterAdapter(
    mList: MutableList<InventoryFilterModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<InventoryFilterModel, InventoryFilterItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        InventoryFilterItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<InventoryFilterItemBinding>,
        position: Int,
        item: InventoryFilterModel?,
    ) {
        with(holder) {
            bind.icon.setImageDrawable(
                ContextCompat.getDrawable(
                    mCtx,
                    item?.icon ?: R.drawable.ic_dollar_2
                )
            )
            bind.category.text = item?.title

            bind.moreIcon.setOnClickListener {
                mClicks.itemClick(position, "open")
            }

            Log.d(TAG, "onBind: CLCICKER ${item?.isOpened}")

            bind.innerLayout.isExpanded = item?.isOpened ?: false

            bind.innerRecycler.adapter = InventoryFilterInnerAdapter(item?.list ?: mutableListOf(), object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {

                }
            })
        }
    }
}

data class InventoryFilterModel(
    val icon: Int,
    val title: String,
    val slug: String,
    val list: MutableList<InnerModel>,
    var isOpened: Boolean = false,
    val isMultiSelection: Boolean = false,
) {
    class InnerModel(
        val id: Int?,
        val title: String?,
    )
}