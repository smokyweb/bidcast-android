package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImageContract
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ChatAdapter
import io.bidswipe.app.databinding.ActivityChatBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Chats
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.MessageSwiper
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody

@SuppressLint("ClickableViewAccessibility")
class ChatActivity : BaseActivity() {

	private val bind by bind(ActivityChatBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()
	private var chatList = mutableListOf<ChatModel>()

	private lateinit var chatRef: DatabaseReference
	private lateinit var chatAdapter: ChatAdapter
	private var reply: ChatModel.Reply? = null
	private lateinit var chats: Chats
	private var isReply = false

	private lateinit var receiverImage: String
	private lateinit var receiverName: String
	private lateinit var receiverId: String
	private lateinit var chatKey: String

	private var firstTimeLoad = true
	private var loadMore = false

	private var chatLimit = 20
	private var isBlockedByMe = false
	private var isBlockedByOther = false

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos: Int, status: String?) {
			when (status) {
				"image" -> {
					/*val imgList = mutableListOf(chatList[pos].attachment?.image.toString())
					StfalconImageViewer.Builder(this@ChatActivity , imgList , ::loadImage).withBackgroundColorResource(clr.surface)
						.withHiddenStatusBar(false)
						.allowSwipeToDismiss(true).allowZooming(true).show(true)*/
				}

				"reply_click" -> {
					val notifyIndex =
						chatList.indexOf(chatList.find { it.id == chatList[pos].replyMessage?.messageId })
					bind.chats.scrollToPosition(notifyIndex)
				}
			}
		}
	}

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val profileUri = result.uriContent
			Alerts.log(TAG, "URI $profileUri")

			/*            if (profileUri != null) {
							chats.sendImage(profileUri) {
			//					viewModel.chatNotification(chatKey.request() , "Shared the image".request() , "image".request() , receiverId.request())
							}
						}
						else {
							errorToast("Couldn't select the image")
						}*/
		} else {
			result.error?.printStackTrace()
		}
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(bind.root)
		
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0,system.top,0, system.bottom)
			CONSUMED
		}
		
		bind.chats.setOnTouchListener { _, _ ->
			hideKeyboard()
			return@setOnTouchListener false
		}

		bind.header.setHapticClickListener {
			hideKeyboard()
		}

		App.isUserOnChatScreen = true
		window.navigationBarColor = ContextCompat.getColor(this, clr.background)

		receiverImage = intent.getStringExtra("image").toString()
		receiverName = intent.getStringExtra("name").toString()
		receiverId = intent.getStringExtra("id").toString()

		bind.root.viewTreeObserver.addOnGlobalLayoutListener {
			val r = Rect()
			bind.root.getWindowVisibleDisplayFrame(r)
			val screenHeight = bind.root.rootView.height
			val keypadHeight = screenHeight - r.bottom

			if (keypadHeight > screenHeight * 0.15) {
				if (chatList.isNotEmpty()) {
					bind.chats.post {
						bind.chats.smoothScrollToPosition(chatList.size - 1)
					}
				}
			}
		}


		chatKey = if (userId > receiverId) {
			receiverId + "_chats_" + userId
		} else {
			userId + "_chats_" + receiverId
		}

		chatRef = FireRef.CHAT.child(chatKey)

		// Reset unread count when opening chat
		FireRef.CHAT_LIST.child(userId).child(receiverId)
			.updateChildren(mapOf("unreadCount" to 0))

		bind.title.text = receiverName.asCapital()
		bind.userImage.loadUrl(this, receiverImage)

		chats = Chats(
			this, chatKey, ChatModel.Users(
				"" + userId,
				"" + userName,
				"" + userImage,
				"" + receiverId,
				"" + receiverName,
				"" + receiverImage,
			)
		)

		hideKeyboard()

		bind.toolbar.setNavigationOnClickListener { finishAfterTransition() }

		chatAdapter = ChatAdapter(this, userId, chatList, mClick)

		bind.chats.also {
			it.adapter = chatAdapter
			it.setHasFixedSize(true)
			it.itemAnimator = SlideInUpAnimator().also { animator ->
				animator.addDuration = 60
				animator.moveDuration = 60
				animator.changeDuration = 60
			}

			it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					super.onScrolled(recyclerView, dx, dy)
					val layoutManager = bind.chats.layoutManager as LinearLayoutManager
					if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 && dy < 0) {
						getMoreChats()
					}

				}
			})
		}

		val helper = ItemTouchHelper(
			MessageSwiper(
				this,
				object : MessageSwiper.SwipeControllerActions {
					override fun showReplyUI(position: Int) {
						if (isBlockedByMe || isBlockedByOther) return

						val message = if (chatList[position].type == "image") {
							chatList[position].attachment?.image.toString()
						} else {
							chatList[position].message.toString()
						}

						reply = ChatModel.Reply(
							"" + chatList[position].type.toString(),
							"" + message,
							"" + chatList[position].users?.senderId.toString(),
							"" + chatList[position].users?.senderName.toString(),
							"" + chatList[position].id.toString(),
						)

						showQuotedMessage(
							message,
							chatList[position].users?.senderName.toString(),
							chatList[position].type.toString()
						)
					}
				})
		)

		helper.attachToRecyclerView(bind.chats)

		bind.send.setHapticClickListener {
			if (isBlockedByMe || isBlockedByOther) return@setHapticClickListener

			if (bind.message.value().isNotEmpty()) {

				val model = if (isReply) ChatModel(
					isReply = true,
					message = bind.message.value(),
					replyMessage = reply
				)
				else ChatModel(isReply = false, message = bind.message.value())

				chats.sendChat(model) {
					viewModel.sendChatNotification(
						receiverId.request(),
						bind.message.value().request()
					)
					bind.message.text = null
					bind.message.isFocusableInTouchMode = true
					showReply(false)
				}


			}
		}

		bind.messageBox.setEndIconOnClickListener {
			if (isBlockedByMe || isBlockedByOther) return@setEndIconOnClickListener
			hideKeyboard()
			requestPerms(Const.PERMISSIONS) {
				if (it) {
					imageResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
				}
			}
		}

		bind.cancel.setHapticClickListener {
			showReply(false)
		}
		checkBlockStatus()
		getChats()


		viewModel.blockUnblockUserRepo.observe(this) { resource ->
			bind.loader.isVisible = false
			when (resource) {
				is Resource.Success -> {
					viewModel.blockUnblockUserRepo.value = null
					isBlockedByMe = !isBlockedByMe
					updateChatUI()
					successToast(resource.value.message ?: "Operation successful")

				}

				is Resource.Error -> {
					viewModel.blockUnblockUserRepo.value = null
					errorToast("Something went wrong")
				}

				else -> {}
			}
		}

	}

	private fun checkBlockStatus() {
		viewModel.getBlockedUsers()
		viewModel.getBlockedUsersRepo.observe(this) { it ->
			when (it) {
				is Resource.Success -> {
					val mData = it.value.data
					isBlockedByMe = mData?.blockedByMe?.any { it?.id.toString() == receiverId } ?: false
					isBlockedByOther = mData?.blockedMe?.any { it?.id.toString() == receiverId } ?: false
					updateChatUI()
				}

				is Resource.Error -> {
				}

				else -> {}
			}
		}
	}

	private fun updateChatUI() {
		if (isBlockedByOther) {
			bind.userBlocked.text = "You are blocked by $receiverName"
			bind.userBlocked.isVisible = true
			bind.chatBox.isVisible = false
			bind.send.isVisible = false
		} else if (isBlockedByMe) {
			unBlockText(bind.userBlocked)
			bind.userBlocked.isVisible = true
			bind.chatBox.isVisible = false
			bind.send.isVisible = false

		} else {
			bind.userBlocked.isVisible = false
			bind.chatBox.isVisible = true
			bind.send.isVisible = true
		}
	}

	private fun unBlockText(view: TextView) {
		val spanTxt = SpannableStringBuilder("You have blocked $receiverName ")
		spanTxt.append(buildSpannedString { bold { append("Click here") } })
		spanTxt.setSpan(object : ClickableSpan() {
			override fun onClick(widget: View) {
				try {
					showUnblockConfirmation()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

			override fun updateDrawState(ds: TextPaint) {
				super.updateDrawState(ds)
				ds.color = ContextCompat.getColor(this@ChatActivity, R.color.error)
				ds.isUnderlineText = true
			}

		}, spanTxt.length - "Click here".length, spanTxt.length, 0)
		spanTxt.append(" to unblock them.")
		view.apply {
			movementMethod = LinkMovementMethod.getInstance()
			highlightColor = Color.TRANSPARENT
			setText(spanTxt, TextView.BufferType.SPANNABLE)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		runSafe { chatRef.removeEventListener(chatPageListener) }
		App.isUserOnChatScreen = false

	}

	private fun getChats() {
		chatRef.removeEventListener(chatPageListener)
		chatRef.orderByChild("id").limitToLast(chatLimit)
			.addListenerForSingleValueEvent(mainChatListener)
	}

	private fun getMoreChats() {
		if (!loadMore) {
			chatRef.removeEventListener(chatPageListener)
			loadMore = true
			bind.expandView.expand(true)
			bind.chats.scrollToPosition(0)

			val firstId = chatList.first().id.toString()

			val ref = chatRef.orderByChild("id").endBefore(firstId, firstId).limitToLast(chatLimit)

			ref.addListenerForSingleValueEvent(mainChatListener)
		}
	}

	private val chatPageListener = object : ChildEventListener {
		override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
			bind.loader.isVisible = false

			val chatMsg = ChatModel().fromMap(snapshot)
			if (chatMsg.users?.receiverId == userId) {
				snapshot.child("seen").ref.setValue(true).addOnSuccessListener {
					// Update the OTHER person's chat list that message is seen (but don't touch their unreadCount)
					chats.updateChatList(mapOf("seen" to true))
					// Update current user's own chat list to reset unreadCount
					FireRef.CHAT_LIST.child(userId).child(receiverId)
						.updateChildren(mapOf("unreadCount" to 0))
				}
			}

			if (chatList.isNotEmpty()) {

				if ((chatList.last().timestamp ?: 0) != (chatMsg.timestamp
						?: 0) || chatList.last().type != Chats.ChatType.DATE
				) {
					if (chatList.find { chat -> chat.id == chatMsg.id } == null) {
						chatList.add(chatMsg)
						chatAdapter.notifyItemInserted(chatList.size - 1)
						bind.chats.smoothScrollToPosition(chatList.size - 1)
					}
				} else {
					if (chatList.find { chat -> chat.id == chatMsg.id } == null) {
						chatList.add(chatMsg)
						chatAdapter.notifyItemInserted(chatList.size - 1)
						bind.chats.smoothScrollToPosition(chatList.size - 1)
					}
				}
			} else {
				chatList.add(chatMsg)
				chatAdapter.notifyItemInserted(0)
				bind.chats.smoothScrollToPosition(chatList.size - 1)
//				bind.chats.isVisible = true
			}

			bind.message.requestFocus()

		}

		override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
			Alerts.log(TAG, "changed ${snapshot.children.count()}")
			runSafe {
				val index = chatList.indexOf(chatList.find { it.id == snapshot.key })
				chatList[index].seen = snapshot.child("seen").getValue(Boolean::class.java)
				chatAdapter.notifyItemChanged(index)
			}
		}

		override fun onChildRemoved(snapshot: DataSnapshot) {
			Alerts.log(TAG, "ChildRemoved $snapshot")
			runSafe {
				val removeAt = chatList.indexOf(chatList.find { it.id == snapshot.key })
				chatList.removeAt(removeAt)
				chatAdapter.notifyItemRemoved(removeAt)
				chatAdapter.notifyItemRangeChanged(0, chatList.size)
			}
		}

		override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
			Alerts.log(TAG, "ChildMoved $snapshot")
		}

		override fun onCancelled(error: DatabaseError) {
			Alerts.log(TAG, "ChildMoved $error")
		}

	}

	private val mainChatListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot: DataSnapshot) {
			bind.loader.isVisible = false

			val tempList = mutableListOf<ChatModel>()

			snapshot.children.forEach {
				val chat = ChatModel().fromMap(it)
				tempList.add(chat)
				if (chat.users?.receiverId == userId) {
					it.ref.updateChildren(mapOf("seen" to true))
				}
			}

			if (chatList.isNotEmpty()) {
				tempList.reverse()
				tempList.forEach {
					if (chatList.find { chat -> chat.id == it.id } == null) {
						if (it.id != null) {
							chatList.add(0, it)
							chatAdapter.notifyItemInserted(0)
						}
					}
				}
			} else {
				chatList.addAll(tempList)
				try {
					chatAdapter.notifyItemRangeInserted(0, chatList.size - 1)
				} catch (_: Exception) {
					chatAdapter.notifyDataSetChanged()
				}
			}

			loadMore = false

			viewModel.viewModelScope.launch {
				delay(1000)
				runOnUiThread {
					bind.expandView.collapse(true)
				}
			}

			runSafe {
				if (firstTimeLoad) {
					firstTimeLoad = false
					if (chatList.size <= chatLimit) {
						bind.chats.smoothScrollToPosition(chatList.size - 1)
					}
				}
			}

//			bind.chats.isVisible = true

			log("SIZE => ${chatList.size}")

			chatRef.limitToLast(chatLimit).addChildEventListener(chatPageListener)
		}

		override fun onCancelled(error: DatabaseError) {
			error.toException().printStackTrace()
			Alerts.log(TAG, "CHAT READ ERROR : ${error.message}")
		}


	}

	private fun showUnblockConfirmation() {
		AppBottomSheet(
			this,
			R.drawable.ic_block,
			"Unblock User",
			"Are you sure you want to unblock $receiverName?",
			primaryBtnText = "Unblock",
			secondaryBtnText = "Cancel",
			canCancel = true,
			showSecondary = true,
			iconPadding = 16,
			alertType = AlertType.INFO,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
					unblockUser()
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}).show()
	}

	private fun unblockUser() {
		bind.loader.isVisible = true
		viewModel.blockUnblockUser(receiverId.toRequestBody())

		viewModel.blockUnblockUserRepo.observe(this) { resource ->
			bind.loader.isVisible = false
			when (resource) {
				is Resource.Success -> {
					viewModel.blockUnblockUserRepo.value = null
					isBlockedByMe = !isBlockedByMe
					updateChatUI()
					successToast(resource.value.message ?: "User unblocked successfully")
				}

				is Resource.Error -> {
					viewModel.blockUnblockUserRepo.value = null
					errorToast("Something went wrong")
				}

				else -> {}
			}
		}
	}

	private fun showQuotedMessage(message: String, name: String, type: String) {
		bind.message.requestFocus()
		val inputMethodManager =
			this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
		inputMethodManager.showSoftInput(bind.message, InputMethodManager.SHOW_IMPLICIT)

		if (type == "image") {
			bind.replyImage.isVisible = true
			bind.replyImage.loadUrl(this, message)
			bind.quotedText.text = buildString { append("Photo") }
		} else {
			bind.quotedText.text = message
			bind.replyImage.isVisible = false
		}

		if (name == (Prefs(this).getUserData()?.firstName + " " + Prefs(this).getUserData()?.lastName)) {
			bind.name.text = buildString { append("You") }
		} else {
			bind.name.text = name
		}

		showReply(true)
	}

	private fun showReply(state: Boolean) {
		isReply = state

		if (state) {
			bind.replyLayout.expand(true)
		} else {
			bind.replyLayout.collapse(true)
		}
	}

}