package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PurchasesItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class PurchasesAdapter(
    mList: MutableList<GetProductsByStatusResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetProductsByStatusResponse.Data, PurchasesItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PurchasesItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PurchasesItemBinding>,
        position: Int,
        item: GetProductsByStatusResponse.Data?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.price.text = item?.product?.pricing.toString().asMoney()

            bind.productId.text = buildString {
                append(item?.product?.title?.asCapital())
                append(" #")
                append(item?.product?.id.toString())
            }

            bind.prodSubTitle.text = buildString {
                append("Buyer: ")
                append(item?.user?.name)
            }

            Log.d(TAG, "onBind: ${bind.date.text}")
            bind.date.text = Utils.getFormattedDateTime(
                "dd-MM-yyyy HH:mm:ss",
                "MM/dd/yyyy",
                item?.product?.createdAt.toString()
            )

            bind.productImage.loadUrl(mCtx, item?.product?.images?.get(0).toString())

        }
    }
}