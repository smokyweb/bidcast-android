package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class ShopAdapter(
    mList: MutableList<GetProductsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetProductsResponse.Data?, ShopItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShopItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShopItemBinding>,
        position: Int,
        item: GetProductsResponse.Data?,
    ) {
        with(holder) {
            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.productImage.loadUrl(mCtx, item?.images?.get(0).toString())

            bind.productName.text = item?.title.toString()

            bind.category.text = buildSpannedString {
                append(item?.description)
            }

            bind.price.text = item?.pricing.toString().asMoney()

        }
    }
}