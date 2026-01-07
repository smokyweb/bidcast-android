package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingUpdateItemBinding
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital

class ShippingUpdateAdapter(
	val mList : MutableList<GetOrderDetailsResponse.Data.ShippingTracking?> ,
) : BaseAdapter<GetOrderDetailsResponse.Data.ShippingTracking , ShippingUpdateItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ShippingUpdateItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ShippingUpdateItemBinding> ,
		position : Int ,
		item : GetOrderDetailsResponse.Data.ShippingTracking? ,
	) {
		with(holder) {

			bind.title.text = item?.title?.asCapital()

			bind.subTitle.text = Utils.getFormattedDateTime(
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" ,
				"MMM dd, yyyy - HH:mm" ,
				item?.createdAt.toString()
			)

			if (position == mList.size - 1) {
				bind.view.visibility = View.INVISIBLE
			} else {
				bind.view.visibility = View.VISIBLE
			}

		}
	}
}