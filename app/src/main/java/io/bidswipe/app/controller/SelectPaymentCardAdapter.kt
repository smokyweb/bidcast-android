package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SelcetableCardItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPaymentCardsResponse

class SelectPaymentCardAdapter(
    mList: MutableList<GetPaymentCardsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetPaymentCardsResponse.Data?, SelcetableCardItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SelcetableCardItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SelcetableCardItemBinding>,
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

            bind.selectBtn.isChecked = item?.selected == true


        }
    }
}