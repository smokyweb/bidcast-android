package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class InventoryAdapter(
	mList : MutableList<GetMyInventoryResponse.Data?> ,
	private val isSelectionMode : Boolean ,
	val mClicks : RecyclerClicks ,
) : BaseAdapter<GetMyInventoryResponse.Data? , InventoryItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		InventoryItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<InventoryItemBinding> ,
		position : Int ,
		item : GetMyInventoryResponse.Data? ,
	) {
		with(holder) {

			bind.productName.text = item?.title?.asCapital()
			bind.prodSubTitle.text = item?.description?.asCapital()
			bind.price.text = item?.pricing.toString().asMoney()
			bind.productImage.loadUrl(mCtx , item?.images?.get(0).toString())


			if (isSelectionMode) {
				if (item?.selected == true) {
					bind.root.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.secondaryContainer))
					bind.root.strokeWidth = 2
					bind.root.strokeColor = ContextCompat.getColor(mCtx , R.color.primary)
				} else {
					bind.root.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.surface))
					bind.root.strokeWidth = 0
				}

				bind.root.setOnClickListener {
					mClicks.itemClick(position , "toggle")
				}
			} else {
				bind.root.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.surface))
				bind.root.strokeWidth = 0
				bind.root.setOnClickListener {
					mClicks.itemClick(position)
				}
			}
		}
	}
}