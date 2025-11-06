package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ProductAdapter(
	val mList : MutableList<GetMyInventoryResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetMyInventoryResponse.Data? , ProductListItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ProductListItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ProductListItemBinding> ,
		position : Int ,
		item : GetMyInventoryResponse.Data? ,
	) {
		with(holder) {

			Log.d(TAG , "onBind: sadsafdsh")
            bind.root.setHapticClickListener {
				mClicks.itemClick(position , "select")
			}

            bind.edit.setHapticClickListener {
				mClicks.itemClick(position , "edit")
			}

            bind.trash.setHapticClickListener {
				mClicks.itemClick(position , "delete")
			}

			if (item?.selected == true) {
				bind.root.strokeWidth = 2
				bind.root.strokeColor = ContextCompat.getColor(mCtx , R.color.primary)
			} else {
				bind.root.strokeWidth = 0
			}

			bind.productName.text = item?.title?.asCapital()
			bind.prodSubTitle.text = item?.description
			bind.quantity.text = buildString {
				append("Quantity: ")
				append(item?.quantity)
			}

			bind.img.loadUrl(mCtx , item?.images.toString())

		}
	}
	
	override fun getItemCount(): Int {
		Log.d(TAG, "getItemCount: ${mList.size}")
		return mList.size
	}
}