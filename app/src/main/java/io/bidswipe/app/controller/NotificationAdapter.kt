package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import com.zerobranch.layout.SwipeLayout
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.NotificationItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.setHapticClickListener

class NotificationAdapter(
	mList: MutableList<GetNotificationResponse.Data?>, val click: RecyclerClicks,
) : BaseAdapter<GetNotificationResponse.Data?, NotificationItemBinding>(mList) {

	val posList = mutableListOf<Int>()

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		NotificationItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<NotificationItemBinding>,
		position: Int,
		item: GetNotificationResponse.Data?,
	) {
		with(holder) {
			bind.message.text = item?.message
			bind.title.text = item?.title
			bind.time.text = Utils.getTimeAgo(item?.createdAt ?: "")

			bind.deleteNotification.setHapticClickListener {
				click.itemClick(position, "delete")
			}

			bind.swipeLayout.close()

			bind.swipeLayout.setOnActionsListener(object : SwipeLayout.SwipeActionsListener {
				override fun onOpen(direction: Int, isContinuous: Boolean) {
					if (posList.isNotEmpty()) {
						val posi = posList.first()
						posList.clear()
						notifyItemChanged(posi)
					}
					posList.add(position)
				}

				override fun onClose() {
					posList.remove(position)
				}

			})

			bind.click.setHapticClickListener {

			}
		}
	}
}