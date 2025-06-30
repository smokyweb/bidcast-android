package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.utils.loadUrl

class ShopSheetAdapter(mList: MutableList<GetProductsResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetProductsResponse.Data?, ShopSheetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) = ShopSheetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShopSheetItemBinding>,
        position: Int,
        item: GetProductsResponse.Data?
    ) {
        with(holder) {
            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.img.loadUrl(mCtx, item?.images?.get(0).toString())

            bind.productName.text = item?.title.toString()

            bind.price.text = buildSpannedString {
                append(item?.description)
            }

//            bind.price.text = item?.pricing.toString().asMoney()

        }
    }
}