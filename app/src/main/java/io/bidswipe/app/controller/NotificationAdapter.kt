package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.NotificationItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.utils.NotificationCategory
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.setHapticClickListener

// Issue #36 fix: Track locally-marked-as-read notification IDs so the list
// immediately reflects read state when a notification is tapped, without
// waiting for a server round-trip.  When the backend exposes a mark-as-read
// endpoint (e.g. api/notification/seen), wire viewModel.markNotificationSeen()
// here instead of / in addition to the local set.
class NotificationAdapter(
	mList: MutableList<GetNotificationResponse.Data?>, val click: RecyclerClicks,
) : BaseAdapter<GetNotificationResponse.Data?, NotificationItemBinding>(mList) {

	/** IDs that have been tapped during this session (treated as locally-seen). */
	private val locallySeenIds = mutableSetOf<Int>()

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		NotificationItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<NotificationItemBinding>,
		position: Int,
		item: GetNotificationResponse.Data?,
	) {
		with(holder) {
			bind.message.text = item?.message
			bind.title.text = item?.title?.asCapital()
			bind.time.text = Utils.getTimeAgo(item?.createdAt ?: "")

			// Task cmph7xsgt: set category icon based on backend `type` string.
			// Falls back to the neutral bell icon for unknown types.
			val category = NotificationCategory.fromRawType(item?.type)
			bind.categoryIcon.setImageResource(category.iconRes)

			// #36: Dim the row when the notification has already been seen
			// (either marked by the server or tapped in this session).
			val isSeen = (item?.isSeen == 1) || (item?.id != null && locallySeenIds.contains(item.id))
			bind.click.alpha = if (isSeen) 0.55f else 1.0f

			// #36: Tapping the notification row marks it as read locally.
			bind.click.setHapticClickListener {
				if (item?.id != null && !isSeen) {
					locallySeenIds.add(item.id)
					notifyItemChanged(position)
				}
				click.itemClick(position, "open")
			}

			bind.deleteIcon.setHapticClickListener {
				click.itemClick(position, "delete")
			}
		}
	}
}