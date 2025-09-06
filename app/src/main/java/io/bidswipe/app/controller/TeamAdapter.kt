package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TeamItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl

class TeamAdapter(
	mList : MutableList<AboutUsResponse.Data.Team?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<AboutUsResponse.Data.Team? , TeamItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TeamItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<TeamItemBinding> ,
		position : Int ,
		item : AboutUsResponse.Data.Team? ,
	) {
		with(holder) {

			bind.title.text = item?.name ?: ""
			bind.description.text = item?.role ?: ""

			bind.image.loadUrl(mCtx , item?.image.toString() , draw.person)

			bind.root.setOnClickListener {
				mClicks.itemClick(position)
			}

		}
	}
}