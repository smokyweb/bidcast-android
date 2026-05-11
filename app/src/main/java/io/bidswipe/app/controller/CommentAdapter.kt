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
import io.bidswipe.app.databinding.LiveCommentItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class CommentAdapter(
	mList: MutableList<LiveChatModel?>,
	private var sellerId: String?,
	val mClicks: RecyclerClicks
) : BaseAdapter<LiveChatModel, LiveCommentItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		LiveCommentItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<LiveCommentItemBinding>,
		position: Int,
		item: LiveChatModel?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.userName.text = item?.userName?.asCapital()
			bind.userImage.loadUrl(mCtx, item?.userImage.toString())
			bind.message.text = if (item?.message?.contains("?") == true) {
				buildSpannedString {
					color(ContextCompat.getColor(mCtx, R.color.primary)) {
						bold { append(item.message) }
					}
				}
			} else {
				item?.message
			}
			bind.hostView.isVisible = sellerId.toString() == item?.userId
		}
	}
}