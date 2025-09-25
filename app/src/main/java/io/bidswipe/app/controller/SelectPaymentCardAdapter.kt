package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SelcetableCardItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.utils.setHapticClickListener

class SelectPaymentCardAdapter(
	mList : MutableList<GetPaymentCardsResponse.Data.PaymentProfile?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetPaymentCardsResponse.Data.PaymentProfile? , SelcetableCardItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SelcetableCardItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SelcetableCardItemBinding> ,
		position : Int ,
		item : GetPaymentCardsResponse.Data.PaymentProfile? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.cardNumber.text = buildString {
				append(item?.payment?.creditCard?.cardNumber)
			}

			bind.selectBtn.isChecked = item?.selected == true

            bind.root.setHapticClickListener {

				mClicks.itemClick(position)

			}


		}
	}
}