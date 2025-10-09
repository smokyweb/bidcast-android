package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.databinding.DateChatItemBinding
import io.bidswipe.app.databinding.ReceiverChatItemBinding
import io.bidswipe.app.databinding.SenderChatItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Chats
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import java.time.Instant
import java.util.Calendar

class ChatAdapter(
	private val mCtx: Context,
	private val senderId: String,
	private val mList: MutableList<ChatModel>,
	private val mClicks: RecyclerClicks,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	companion object {
		const val ITEM_RECEIVE = 1
		const val ITEM_SEND = 2
		const val ITEM_DATE = 3
	}

	@RequiresApi(Build.VERSION_CODES.O)
	@SuppressLint("SetTextI18n")
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val chat = mList[position]

		if (chat.type != Chats.ChatType.DATE) {
			if (chat.users?.senderId == senderId) {
				with(holder as SentViewHolder) {
					when (chat.type) {
						Chats.ChatType.TEXT -> {
							bind.chatView.isVisible = true
							bind.imageView.isVisible = false
						}

						Chats.ChatType.IMAGE -> {
							bind.chatView.isVisible = false
							bind.imageView.isVisible = true
							bind.image.loadUrl(mCtx, chat.attachment?.image.toString())
						}
					}

					bind.imageView.setHapticClickListener {
						mClicks.itemClick(position, "image")
					}

					bind.replyView.root.setBackgroundColor(
						ContextCompat.getColor(
							mCtx, clr.primaryContainer
						)
					)
					bind.replyView.replyDivider.backgroundTintList =
						ColorStateList.valueOf(ContextCompat.getColor(mCtx, clr.onPrimaryContainer))

					bind.root.setOnLongClickListener {
						mClicks.itemClick(position, "select")
						true
					}

					bind.replyView.root.setHapticClickListener {
						mClicks.itemClick(position, "reply_click")
					}

					bind.replyView.root.isVisible = chat.isReply ?: false
					if (chat.replyMessage?.type == "image") {
						bind.replyView.replyImage.loadUrl(
							mCtx, chat.replyMessage?.message.toString()
						)
						bind.replyView.replyMsg.text = "Photo"
					} else {
						bind.replyView.replyImage.isVisible = false
						bind.replyView.replyMsg.text = chat.replyMessage?.message
					}

					if (chat.replyMessage?.senderName == (Prefs(mCtx).getUserData()?.firstName + " " + Prefs(
							mCtx
						).getUserData()?.lastName)
					) {
						bind.replyView.replyName.text = "You"
					} else {
						bind.replyView.replyName.text = chat.replyMessage?.senderName
					}

					bind.message.text = chat.message.toString()
					bind.msgTime.text = Utils.getTimeFromTimestamp(chat.timestamp ?: 0)

					if (chat.seen == true) {
						bind.read.setImageResource(draw.read_check)
						bind.imgRead.setImageResource(draw.read_check)
					} else {
						bind.read.setImageResource(draw.ic_check)
						bind.imgRead.setImageResource(draw.ic_check)
					}
				}
			} else {
				with(holder as ReceiveViewHolder) {
					when (chat.type) {
						Chats.ChatType.TEXT -> {
							bind.chatView.isVisible = true
							bind.imageView.isVisible = false
						}

						Chats.ChatType.IMAGE -> {
							bind.chatView.isVisible = false
							bind.imageView.isVisible = true
							bind.image.loadUrl(mCtx, chat.attachment?.image.toString())
						}
					}

					bind.imgRead.isVisible = false

					bind.root.setOnLongClickListener {
						mClicks.itemClick(position, "select")
						true
					}

					bind.replyView.root.setHapticClickListener {
						mClicks.itemClick(position, "reply_click")
					}

					bind.replyView.root.isVisible = chat.isReply ?: false
					if (chat.replyMessage?.type == "image") {
						bind.replyView.replyImage.loadUrl(
							mCtx, chat.replyMessage?.message.toString()
						)
						bind.replyView.replyMsg.text = "Photo"
					} else {
						bind.replyView.replyImage.isVisible = false
						bind.replyView.replyMsg.text = chat.replyMessage?.message
					}

					if (chat.replyMessage?.senderName == Prefs(mCtx).getUserData()?.name) {
						bind.replyView.replyName.text = "You"
					} else {
						bind.replyView.replyName.text = chat.replyMessage?.senderName?.asCapital()
					}

					bind.message.text = chat.message
					bind.msgTime.text = Utils.getTimeFromTimestamp(chat.timestamp ?: 0)

				}
			}
		} else {
			with(holder as DateViewHolder) {

				try {
					val calendar = Calendar.getInstance()
					calendar.timeInMillis =
						Instant.ofEpochSecond(chat.timestamp ?: 0L).toEpochMilli()

					val now = Calendar.getInstance()

					bind.date.text = when {
						now.get(Calendar.DATE) == calendar.get(Calendar.DATE) -> {
							"Today"
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
					chat.message
				}

			}
		}

	}

	override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
		return when (type) {
			ITEM_RECEIVE -> ReceiveViewHolder(
				ReceiverChatItemBinding.bind(
					LayoutInflater.from(parent.context)
						.inflate(R.layout.receiver_chat_item, parent, false)
				)
			)

			ITEM_DATE -> DateViewHolder(
				DateChatItemBinding.bind(
					LayoutInflater.from(parent.context)
						.inflate(R.layout.date_chat_item, parent, false)
				)
			)

			else -> SentViewHolder(
				SenderChatItemBinding.bind(
					LayoutInflater.from(parent.context)
						.inflate(R.layout.sender_chat_item, parent, false)
				)
			)
		}
	}

	override fun getItemViewType(position: Int): Int {
		return if (mList[position].type == Chats.ChatType.DATE) {
			ITEM_DATE
		} else {
			if (mList[position].users?.senderId == senderId) {
				ITEM_SEND
			} else {
				ITEM_RECEIVE
			}
		}
	}

	override fun getItemCount() = mList.size

	class SentViewHolder(var bind: SenderChatItemBinding) : RecyclerView.ViewHolder(bind.root)

	class ReceiveViewHolder(var bind: ReceiverChatItemBinding) : RecyclerView.ViewHolder(bind.root)

	class DateViewHolder(var bind: DateChatItemBinding) : RecyclerView.ViewHolder(bind.root)

}
