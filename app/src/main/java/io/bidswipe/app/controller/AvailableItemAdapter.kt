package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.AvailableItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.setHapticClickListener

class AvailableItemAdapter(
    mList: MutableList<GetSurpriseProductsResponse.Data.Item?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetSurpriseProductsResponse.Data.Item?, AvailableItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        AvailableItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<AvailableItemBinding>,
        position: Int,
        item: GetSurpriseProductsResponse.Data.Item?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.productName.text = item?.name
            bind.quantity.text = "Qty: "+((item?.quantity ?: 0)-(item?.soldQuantity?:0)).toString()
            bind.status.text = item?.status?.asCapital()

        }
    }
}