package io.bidswipe.app.controller

import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale

class MessagesAdapter(
	mList: MutableList<ChatModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<ChatModel, MessagesItemsBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		MessagesItemsBinding.inflate(inflater, parent, false)

	@RequiresApi(Build.VERSION_CODES.O)
	override fun onBind(
		holder: BaseViewHolder<MessagesItemsBinding>,
		position: Int,
		item: ChatModel?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position, "")
			}

			if (item?.users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
				bind.name.text = item.users?.receiverName?.asCapital()
				bind.icon.loadUrl(mCtx, item.users?.receiverImage ?:"", draw.placeholder_user)
			} else {
				bind.name.text = item?.users?.senderName?.asCapital()
				bind.icon.loadUrl(mCtx, item?.users?.senderImage ?:"",draw.placeholder_user)
			}

			bind.message.text = item?.message
			
			// Show/hide unread count badge
			val unreadCount = item?.unreadCount ?: 0
			if (unreadCount > 0) {
				bind.notificationBadge.isVisible = true
				bind.notificationBadge.text = when {
					unreadCount > 99 -> "99+"
					else -> unreadCount.toString()
				}
			} else {
				bind.notificationBadge.isVisible = false
			}

			try {
				val calendar = Calendar.getInstance()
				calendar.timeInMillis = Instant.ofEpochSecond(item?.timestamp ?: 0L).toEpochMilli()

				val now = Calendar.getInstance()

				bind.time.text = when {
					now.get(Calendar.DATE) == calendar.get(Calendar.DATE) -> {
						SimpleDateFormat(
							"hh:mm a",
							Locale.getDefault()
						).format(Instant.ofEpochSecond(item?.timestamp ?: 0L).toEpochMilli())
							.toString()
					}

					now.get(Calendar.DATE) - calendar.get(Calendar.DATE) == 1 -> {
						"Yesterday"
					}

					else -> {
						DateFormat.format("MM-dd-yyyy", calendar).toString()
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(
					Instant.ofEpochSecond(
						item?.timestamp ?: 0L
					).toEpochMilli()
				).toString()
			}

		}
	}
}