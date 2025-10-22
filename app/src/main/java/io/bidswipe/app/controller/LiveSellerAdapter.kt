package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ExploreItemBinding
import io.bidswipe.app.databinding.UserSelectorItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class LiveSellerAdapter(
	val mList: MutableList<String?>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, UserSelectorItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		UserSelectorItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<UserSelectorItemBinding>,
		position: Int,
		item: String?,
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