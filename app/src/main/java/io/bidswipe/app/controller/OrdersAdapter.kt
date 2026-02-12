package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class OrdersAdapter(
    mList: MutableList<GetOrdersResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetOrdersResponse.Data?, MyOrdersItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        MyOrdersItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<MyOrdersItemBinding>,
        position: Int,
        item: GetOrdersResponse.Data?,
    ) {
        with(holder) {
            bind.root.setHapticClickListener { mClicks.itemClick(position) }

            bind.buyerLayout.setHapticClickListener {
                mClicks.itemClick(position, "profile")
            }

            bind.orderId.text = item?.orderId
            bind.status.text = item?.status?.replace("_", " ")?.asCapital()

            when (item?.status?.lowercase()) {
                "cancelled", "rejected" -> {
                    bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.errorContainer))
                    bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.error)
                    bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.error))
                }

                "delivered" -> {
                    bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.successContainer))
                    bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.success)
                    bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.success))
                }

                else -> {
                    bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
                    bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
                    bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
                }
            }

            val price = if (item?.productId != null) {
                (item.product?.pricing ?: "0")
            } else {
                (item?.productSetItemUnit?.price ?: 0.0).toString()
            }

            bind.orderAmount.text = buildSpannedString {
                color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                    append("Sold For: ")
                }
                append(price.asMoney())
            }

            val time = Utils.getTimeStampFromServerTime(item?.createdAt.toString())
            bind.orderDate.text = Utils.getTimeFromServerTimestamp(
                time,
                Const.MMM_dd_yyyy_HH_mm,
            )

            if (item?.productId != null) {
                bind.productCard.isVisible = true
                bind.productImage.loadUrl(mCtx, item?.product?.images?.get(0) ?: "")
            } else {
                bind.productCard.isVisible = false
            }

            bind.productName.text = if (item?.productId != null) item.product?.title?.asCapital() else item?.productSet?.name + " #${item?.productSetItemUnitId}"

            bind.buyerName.text = item?.user?.name?.asCapital()

        }
    }
}