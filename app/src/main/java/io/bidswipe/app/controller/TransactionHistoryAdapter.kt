package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TransactionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney

class TransactionHistoryAdapter(
    mList: MutableList<GetTransactionsHistoryResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetTransactionsHistoryResponse.Data, TransactionItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        TransactionItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<TransactionItemBinding>,
        position: Int,
        item: GetTransactionsHistoryResponse.Data?,
    ) {
        with(holder) {
            Log.d(TAG, "onBind: $item")
            val titleText = when (item?.sourceType) {
                "tip_amount" -> "Sent tip to ${item.receiver?.name}"
                "account" -> if (item.type == "withdraw") "Payout" else if (item.type == "debited") "Debited" else ""
                "card" -> if (item.type == "debited" && item.orderId != null) "Product Purchased" else "Debited"
                else -> "Debited"
            }

            bind.title.text = buildSpannedString {
                append(titleText)
                if (item?.show != null) {
                    append(" during ")
                    append(item.show.title.toString())
                }
            }

            val icon = when (item?.status?.lowercase()) {
                "process" -> R.drawable.ic_transaction_processing
                "paid" -> R.drawable.ic_transaction_success
                else -> R.drawable.ic_transaction
            }

            bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, icon))

            bind.amount.text = item?.total.toString().asMoney()

            bind.date.text = Utils.getFormattedDateTime(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "MMM dd, yyyy",
                item?.date.toString()
            )

        }
    }
}