package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PaymentCardItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPaymentCardsResponse

class PaymentCardAdapter(
	mList : MutableList<GetPaymentCardsResponse.Data.PaymentProfile?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetPaymentCardsResponse.Data.PaymentProfile? , PaymentCardItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		PaymentCardItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<PaymentCardItemBinding> ,
		position : Int ,
		item : GetPaymentCardsResponse.Data.PaymentProfile? ,
	) {
		with(holder) {

			bind.root.setOnClickListener {
				mClicks.itemClick(position)
			}

			bind.cardNumber.text = item?.payment?.creditCard?.cardNumber

			bind.expiryDate.text = buildString {
				append(item?.payment?.creditCard?.expirationDate)
			}

			bind.defaultAddress.isVisible = item?.isDefault == true

			bind.moreIcon.setOnClickListener { view ->
				val popup = PopupMenu(view.context , view)
				popup.inflate(R.menu.card_action_menu)  // Your menu XML
				popup.setOnMenuItemClickListener { menuItem ->
					when (menuItem.itemId) {
						R.id.setDefault -> {
							mClicks.itemClick(position , "default")
							true
						}

						R.id.delete -> {
							mClicks.itemClick(position , "delete")
							true
						}

						else -> false
					}
				}
				popup.show()
			}


		}
	}
}