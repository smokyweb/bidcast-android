package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.databinding.SurpriseProductItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ManageProductItemAdapter(
	mList : MutableList<SurpriseProductModel?>, val mClicks : RecyclerClicks,
) : BaseAdapter<SurpriseProductModel? , SurpriseProductItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SurpriseProductItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SurpriseProductItemBinding> ,
		position : Int ,
		item : SurpriseProductModel? ,
	) {
		with(holder) {
			bind.deleteIcon.setHapticClickListener {
				mClicks.itemClick(position)
			}

			if(position%2==0){
				bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.primaryContainer))
			}else{
				bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.transparent))
			}

			bind.name.text = item?.name ?: " "
			bind.desc.text = item?.description ?: " "
			bind.index.text = (position+1).toString()
			bind.quantity.text = item?.quantity.toString()

		}
	}
}