package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
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
			bind.orderId.text = item?.orderId
			bind.status.text = item?.status?.replace("_", " ")?.asCapital()
			bind.orderAmount.text = item?.product?.pricing.toString().asMoney()
			
			bind.orderDate.text = Utils.getFormattedDateTime(
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
				"MMM dd, yyyy, HH:mm",
				item?.createdAt.toString()
			)
			
			bind.productImage.loadUrl(mCtx, item?.product?.images?.get(0) ?:"")
			
			bind.productName.text = item?.product?.title?.asCapital()
			bind.category.text = item?.product?.category?.name
			
		}
	}
}