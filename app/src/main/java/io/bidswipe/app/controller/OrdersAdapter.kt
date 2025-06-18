package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.databinding.MyOrdersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class OrdersAdapter (mList: MutableList<GetOrdersResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetOrdersResponse.Data?, MyOrdersItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        MyOrdersItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<MyOrdersItemBinding>,
        position: Int,
        item: GetOrdersResponse.Data?
    ) {
        with(holder) {

            bind.orderId.text = item?.orderId
            bind.status.text = item?.status

            bind.orderAmount.text = item?.product?.pricing.toString().asMoney()

            bind.orderDate.text = Utils.getFormattedDateTime("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'","MMM dd, yyyy, HH:mm",item?.createdAt.toString())

            bind.userImage.loadUrl(mCtx,item?.user?.profileImage.toString())

            bind.sellerName.text = item?.user?.name
            bind.sellerAddress.text = item?.shippingAddress

        }
    }
}