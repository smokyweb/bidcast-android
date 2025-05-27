package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.utils.loadUrl

class ProductAdapter (val mList: MutableList<GetProductsResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetProductsResponse.Data?, ProductListItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductListItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ProductListItemBinding>,
        position: Int,
        item: GetProductsResponse.Data?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.productName.text = item?.title
            bind.prodSubTitle.text = item?.description
            bind.quantity.text = buildString {
                append("Quantity: ")
                append(item?.quantity)
            }

            bind.img.loadUrl(mCtx,item?.images.toString())

        }
    }
}