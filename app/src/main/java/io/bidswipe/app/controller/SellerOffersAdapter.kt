package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.databinding.SellerOffersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class SellerOffersAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, SellerOffersItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SellerOffersItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SellerOffersItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}