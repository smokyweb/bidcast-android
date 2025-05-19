package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class InventoryAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, InventoryItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        InventoryItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<InventoryItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}