package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingAddressItemBinding
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ShippingAddressAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String?, ShippingAddressItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingAddressItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingAddressItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

        }
    }
}