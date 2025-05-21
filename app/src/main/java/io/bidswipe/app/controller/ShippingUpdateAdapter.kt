package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingUpdateItemBinding

class ShippingUpdateAdapter  (val mList: MutableList<String>
) : BaseAdapter<String, ShippingUpdateItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingUpdateItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingUpdateItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {



        }
    }
}