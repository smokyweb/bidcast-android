package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CouponsListItemBinding
import io.bidswipe.app.databinding.LiveCommentItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class CouponsAdapter(
	mList: MutableList<String>,
	val mClicks: RecyclerClicks
) : BaseAdapter<String, CouponsListItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		CouponsListItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<CouponsListItemBinding>,
		position: Int,
		item: String?,
	) {
		with(holder) {


		}
	}
}