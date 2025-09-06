package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.RequirementItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPremierShopResponse

class RequirementAdapter(
	mList : MutableList<GetPremierShopResponse.Data.Requirement?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetPremierShopResponse.Data.Requirement? , RequirementItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		RequirementItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<RequirementItemBinding> ,
		position : Int ,
		item : GetPremierShopResponse.Data.Requirement? ,
	) {
		with(holder) {

			bind.title.text = item?.platform
			bind.subTitle.text = item?.url

		}
	}
}