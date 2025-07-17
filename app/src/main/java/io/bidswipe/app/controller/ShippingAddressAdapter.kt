package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingAddressItemBinding
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetShippingAddressResponse

class ShippingAddressAdapter (mList: MutableList<GetShippingAddressResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetShippingAddressResponse.Data?, ShippingAddressItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingAddressItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingAddressItemBinding>,
        position: Int,
        item: GetShippingAddressResponse.Data?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.address.text = item?.streetAddress
            bind.name.text = item?.name
            bind.type.text = item?.type

        }
    }
}