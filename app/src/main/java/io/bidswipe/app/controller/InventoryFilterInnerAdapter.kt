package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.databinding.InventoryFilterInnnerIitemBinding
import io.bidswipe.app.databinding.InventoryFilterItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class InventoryFilterInnerAdapter(
    mList: MutableList<InventoryFilterModel.InnerModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<InventoryFilterModel.InnerModel, InventoryFilterInnnerIitemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        InventoryFilterInnnerIitemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<InventoryFilterInnnerIitemBinding>,
        position: Int,
        item: InventoryFilterModel.InnerModel?,
    ) {
        with(holder) {
            bind.checkbox.text = item?.title

            bind.checkbox.setOnCheckedChangeListener { _,_->

            }
        }
    }
}