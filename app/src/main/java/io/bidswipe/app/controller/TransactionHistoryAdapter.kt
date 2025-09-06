package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TransactionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney

class TransactionHistoryAdapter(
	mList : MutableList<GetTransactionsHistoryResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetTransactionsHistoryResponse.Data , TransactionItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TransactionItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<TransactionItemBinding> ,
		position : Int ,
		item : GetTransactionsHistoryResponse.Data? ,
	) {
		with(holder) {

			bind.title.setText("Purchase Completed")

			bind.amount.text = item?.total.toString().asMoney()

			bind.date.text = Utils.getFormattedDateTime(
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" ,
				"MMM dd, yyyy" ,
				item?.date.toString()
			)

		}
	}
}