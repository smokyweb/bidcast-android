package io.bidswipe.app.controller

import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale
import kotlin.math.log

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
			bind.root.setOnClickListener {
				mClicks.itemClick(position, "")
			}

			val currentUserId = Prefs(mCtx).getUserData()?.id.toString()
			val currentUserIsSender = item?.users?.senderId == currentUserId

			if (currentUserIsSender) {
				// Current user sent the last message — show other party's info
				bind.name.text = item?.users?.receiverName?.asCapital()
				if (item?.users?.receiverImage?.isEmpty() == false) bind.icon.loadUrl(mCtx, item.users?.receiverImage ?: "", draw.placeholder_user, bind.name.text.toString())
			} else {
				// Other party sent the last message
				bind.name.text = item?.users?.senderName?.asCapital()
				if (item?.users?.senderImage?.isEmpty() == false) bind.icon.loadUrl(mCtx, item?.users?.senderImage ?: "", draw.placeholder_user, bind.name.text.toString())
			}

			bind.message.text = item?.message

			// #37: Show/hide unread count badge
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

			// #37: Dim the message preview row when the user has responded
			// (i.e. they are the sender of the last message in this thread).
			// A "seen" thread where current user responded should appear grayed-out
			// to indicate it is waiting for the other party to reply.
			val userHasResponded = currentUserIsSender && (item?.seen == true || unreadCount == 0)
			bind.message.alpha = if (userHasResponded) 0.45f else 1.0f
			bind.root.alpha = if (userHasResponded) 0.7f else 1.0f

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
