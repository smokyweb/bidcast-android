package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PayoutItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.PayoutHistoryResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney

class PayoutAdapter(
	mList : MutableList<PayoutHistoryResponse.Data?>, val mClicks : RecyclerClicks,
) : BaseAdapter<PayoutHistoryResponse.Data? , PayoutItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		PayoutItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<PayoutItemBinding> ,
		position : Int ,
		item : PayoutHistoryResponse.Data? ,
	) {
		with(holder) {

			bind.amount.text=item?.total.toString().asMoney()
			bind.status.text=item?.status?.asCapital()
			bind.date.text=	Utils.getFormattedDateTime(
				Const.SERVER_TIME_FORMAT ,
				"MMMM dd, yyyy" ,
				item?.date.toString()
			)
		}
	}
}