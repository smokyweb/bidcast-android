package io.bidswipe.app.controller

import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.loadUrl
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

            if (item?.users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
                bind.name.text = item.users?.receiverName
                bind.icon.loadUrl(mCtx, item.users?.receiverImage.toString())
            } else {
                bind.name.text = item?.users?.senderName
                bind.icon.loadUrl(mCtx, item?.users?.senderImage.toString())
            }

            bind.message.text = item?.message

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

            holder.bind.root.setOnClickListener {
                mClicks.itemClick(position, "")
            }
        }
    }
}