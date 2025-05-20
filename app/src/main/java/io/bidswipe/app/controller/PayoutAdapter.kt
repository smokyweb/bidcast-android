package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.databinding.PayoutItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class PayoutAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, PayoutItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PayoutItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PayoutItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}