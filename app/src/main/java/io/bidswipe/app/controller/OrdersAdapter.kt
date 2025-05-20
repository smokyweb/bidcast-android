package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class OrdersAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, MyOrdersItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        MyOrdersItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<MyOrdersItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}