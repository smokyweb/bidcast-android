package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class InventoryAdapter(
    mList: MutableList<GetMyInventoryResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetMyInventoryResponse.Data?, InventoryItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        InventoryItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<InventoryItemBinding>,
        position: Int,
        item: GetMyInventoryResponse.Data?,
    ) {
        with(holder) {

            bind.productName.text = item?.title?.asCapital()
            bind.prodSubTitle.text = item?.description?.asCapital()
            bind.price.text = item?.pricing.toString().asMoney()

            bind.productImage.loadUrl(mCtx, item?.images?.get(0).toString())
            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

        }
    }
}