package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingAddressItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.setHapticClickListener

class ShippingAddressAdapter(
	mList : MutableList<GetShippingAddressResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetShippingAddressResponse.Data? , ShippingAddressItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ShippingAddressItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ShippingAddressItemBinding> ,
		position : Int ,
		item : GetShippingAddressResponse.Data? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.address.text = item?.streetAddress
			bind.name.text = item?.name?.asCapital()
			bind.type.text = item?.type

			bind.defaultAddress.isVisible = item?.isDefault == true

            bind.moreIcon.setHapticClickListener { view ->
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