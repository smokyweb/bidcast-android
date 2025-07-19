package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PaymentCardItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPaymentCardsResponse

class PaymentCardAdapter(
    mList: MutableList<GetPaymentCardsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetPaymentCardsResponse.Data?, PaymentCardItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PaymentCardItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PaymentCardItemBinding>,
        position: Int,
        item: GetPaymentCardsResponse.Data?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.cardNumber.text = buildString {
                append("XXXX-XXXX-XXXX-")
                append(item?.last4)
            }

            bind.expiryDate.text = buildString {
                append(item?.expMonth)
                append("/")
                append(item?.expYear.toString().drop(2))
            }


        }
    }
}