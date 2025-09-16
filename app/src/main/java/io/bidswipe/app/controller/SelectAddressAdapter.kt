package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SelectableAddressItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.utils.setHapticClickListener

class SelectAddressAdapter(

	mList : MutableList<GetShippingAddressResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetShippingAddressResponse.Data? , SelectableAddressItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SelectableAddressItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SelectableAddressItemBinding> ,
		position : Int ,
		item : GetShippingAddressResponse.Data? ,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.address.text = buildString {
				append(item?.streetAddress)
			}

			bind.selectBtn.isChecked = item?.selected == true

		}
	}
}