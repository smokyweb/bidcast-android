package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.NotificationItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.utils.Utils

class NotificationAdapter(mList: MutableList<GetNotificationResponse.Data?>,val click : RecyclerClicks
) : BaseAdapter<GetNotificationResponse.Data?, NotificationItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        NotificationItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<NotificationItemBinding>,
        position: Int,
        item: GetNotificationResponse.Data?
    ) {
        with(holder) {
            bind.message.text = item?.message
            bind.title.text = item?.title
            bind.time.text = Utils.getTimeAgo(item?.createdAt ?: "")

            bind.deleteNotification.setOnClickListener {

                click.itemClick(position,"delete")

            }
        }
    }
}