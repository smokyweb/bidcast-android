package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UserSelectorItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.utils.setHapticClickListener

class LiveSellerAdapter(
	val mList: MutableList<GetLiveSellerResponse.Data?> , val mClicks: RecyclerClicks ,
) : BaseAdapter<GetLiveSellerResponse.Data?, UserSelectorItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		UserSelectorItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<UserSelectorItemBinding>,
		position: Int,
		item: GetLiveSellerResponse.Data?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.radioBtn.isVisible = true

//			bind.title.text = item

		}
	}
}