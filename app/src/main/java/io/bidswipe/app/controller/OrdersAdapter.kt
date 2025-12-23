package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
                "cancelled","rejected" -> {
                    bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.errorContainer))
                    bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.error)
                    bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.error))
                }
                "delivered" -> {}
                else -> {
                    bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
                    bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
                    bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
                }
            }
			bind.orderAmount.text = item?.product?.pricing.toString().asMoney()
			
			bind.orderDate.text = Utils.getFormattedDateTime(
				Const.SERVER_TIME_FORMAT,
				"MMM dd, yyyy, HH:mm",
				item?.createdAt.toString()
			)
			
			bind.productImage.loadUrl(mCtx, item?.product?.images?.get(0) ?:"")
			
			bind.productName.text = item?.product?.title?.asCapital()

			bind.buyerName.text = item?.user?.name?.asCapital()

			bind.buyerLayout.setOnClickListener {
				mClicks.itemClick(position,"buyerInfo")
			}
		}
	}
}