package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.callback.ZIMMessageSentFullCallback
import im.zego.zim.entity.ZIMError
import im.zego.zim.entity.ZIMMediaMessage
import im.zego.zim.entity.ZIMMessage
import im.zego.zim.entity.ZIMMessageReceivedInfo
import im.zego.zim.entity.ZIMMessageSendConfig
import im.zego.zim.entity.ZIMMultipleMessage
import im.zego.zim.entity.ZIMRoomInfo
import im.zego.zim.entity.ZIMTextMessage
import im.zego.zim.entity.ZIMUserInfo
import im.zego.zim.enums.ZIMConversationType
import im.zego.zim.enums.ZIMMessagePriority
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.CommentModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value
import org.json.JSONObject

class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {

	override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	) = FragmentWatchStreamBinding.inflate(inflater, view, false)

	private lateinit var roomID: String
	private lateinit var streamID: String
	private var commentList = mutableListOf<CommentModel?>()
	private lateinit var commentAdapter: CommentAdapter
	private var isLoggedIn = false

	companion object {
		fun newInstance(roomID: String, streamID: String) = WatchStreamFragment().apply {
			arguments = Bundle().apply {
				putString("roomID", roomID)
				putString("streamID", streamID)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		roomID = requireArguments().getString("roomID") ?: ""
		streamID = requireArguments().getString("streamID") ?: ""
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		log("RoomId: $roomID")

		bind.cutButton.setOnClickListener {
			finish()
		}

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).addValueEventListener(object : ValueEventListener {
			@SuppressLint("NotifyDataSetChanged")
			override fun onDataChange(snapshot: DataSnapshot) {

				val data = snapshot.getValue(LiveShowModel::class.java)

				bind.liveCount.text = data?.viewerCount.toString()

				if (data?.product?.status == "sold") {
					bind.soldLayout.isVisible = true
					bind.productLayout.isVisible = false
				} else {
					bind.soldLayout.isVisible = false
					bind.productLayout.isVisible = true
				}

			}

			override fun onCancelled(error: DatabaseError) {

			}

		})

		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
//                sendMessage(bind.text.value())

				if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
					sendZimMessage(bind.text.value())
				}else{
					verificationDialog()
				}
			}
		}

		viewModel.selectedStream.observe(viewLifecycleOwner) { stream ->
			if (stream.roomId == roomID) {

				bind.userImage.loadUrl(
					mCtx,
					stream.seller?.image.toString(),
					placeHolder = draw.user_image
				)

				bind.userName.text = stream.seller?.name.toString()
				bind.productName.text = stream.product?.name
				bind.productImage.loadUrl(
					mCtx,
					stream?.product?.image.toString(),
					placeHolder = draw.product_img
				)

				try {
					bind.quantity.text = buildString {
						append("Price: ")
						append(stream.product?.price.toString())
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}

				if (stream.seller?.isFollowed == true) {
					bind.follow.setBackgroundColor(
						ContextCompat.getColor(
							mCtx,
							R.color.outline
						)
					)
					bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
					bind.follow.text = "Unfollow"
				} else {
					bind.follow.setBackgroundColor(
						ContextCompat.getColor(
							mCtx,
							R.color.primary
						)
					)
					bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.background))
					bind.follow.text = "Follow"
				}

				bind.follow.setOnClickListener {

					viewModel.followUser(stream.seller?.id?.request())

				}

				try {
					bind.max.text = stream.product?.price.toString().asMoney()

					bind.bid.text = "Swipe to Bid for ${(stream.product?.price?.toInt()?.plus(1)).toString().asMoney()}"
				} catch (e: Exception) {
					e.printStackTrace()
				}

				bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
					override fun onSlideComplete(view: SlideToActView) {
						log("SWIPED")

						bind.loader.isVisible = true

						viewModel.createBid(
							stream.showId?.request(),
							userId.request(),
							stream.product?.id.toString().request(),
							stream.product?.price?.request()
						)

					}
				}

			}
		}

		viewModel.createBidRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					viewModel.createBidRepo.value = null

					bind.loader.isVisible = false

					it.value.data

					Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).child("product").child("status").setValue("sold")
						.addOnCompleteListener {

						}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

		viewModel.followUserShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					val mData = it.value.data

					if (mData?.status == true) {
						bind.follow.setBackgroundColor(
							ContextCompat.getColor(
								mCtx,
								R.color.outline
							)
						)
						bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
						bind.follow.text = "Unfollow"
					} else {
						bind.follow.setBackgroundColor(
							ContextCompat.getColor(
								mCtx,
								R.color.primary
							)
						)
						bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.background))
						bind.follow.text = "Follow"
					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

		verificationDialog()

	}

	override fun onResume() {
		super.onResume()

		loginAndPlay()

	}

	override fun onPause() {
		super.onPause()
		stopStream()
	}

	private fun loginAndPlay() {
		val user = ZegoUser(userName.replace(" ", ".") + "_" + userId, userImage)
		ZegoExpressEngine.getEngine().loginRoom(roomID, user, ZegoRoomConfig())

		val canvas = ZegoCanvas(bind.hostView).apply {
			viewMode = ZegoViewMode.ASPECT_FILL
		}
		ZegoExpressEngine.getEngine().startPlayingStream(roomID, canvas)
		if (viewModel.previousRoomId.isNotEmpty()){
			val roomInfo = ZIMRoomInfo().also {
				it.roomID = roomID
				it.roomName = roomID + "_room"
			}
			ZIM.getInstance().switchRoom(viewModel.previousRoomId, roomInfo,false,null) { roomInfo, errorInfo ->
				if (errorInfo != null) {
					log("JOINED ROOM CHAT $roomInfo")

					ZIM.getInstance().setEventHandler(zimEventHandler)

					viewModel.previousRoomId = roomID

					sendZimMessage("joined \uD83D\uDC4B")

				} else {
					log("JOIN ROOM CHAT ERROR : ${errorInfo.toString()}")
				}
			}
		}else{
			setupZIMChat()
		}
	}

	private fun stopStream() {
		ZegoExpressEngine.getEngine().stopPlayingStream(roomID)
		ZegoExpressEngine.getEngine().logoutRoom(roomID)
	}

	private fun destroyEngine() {
		ZegoExpressEngine.destroyEngine(null)
	}

    private fun setupZIMChat() {


        val userInfo = ZIMUserInfo().also {
            it.userID = userName.replace(" ", ".") + "_" + userId
            it.userName = userImage
        }

	    ZIM.getInstance().login(userInfo) { error ->
            if (error != null) {
                log("LOGGED INTO ZIM")
	            ZIM.getInstance().joinRoom(roomID) { roomInfo, errorInfo ->
                    if (errorInfo != null) {
                        log("JOINED ROOM CHAT $roomInfo")

	                    ZIM.getInstance().setEventHandler(zimEventHandler)
	                    viewModel.previousRoomId = roomID

	                    sendZimMessage("joined \uD83D\uDC4B")

                    } else {
                        log("JOIN ROOM CHAT ERROR : ${errorInfo.toString()}")
                    }
                }
            } else {
                log("LOG IN ROOM ERROR : ${error.toString()}")
            }
        }

    }

    private val zimEventHandler = object : ZIMEventHandler() {
        override fun onRoomMessageReceived(
            zim: ZIM?,
            messageList: java.util.ArrayList<ZIMMessage?>?,
            info: ZIMMessageReceivedInfo?,
            fromRoomID: String?
        ) {
	        super.onRoomMessageReceived(zim, messageList, info, fromRoomID)
	        log("MESSAGE RECEIVED onRoomMessageReceived: ${messageList?.joinToString("\n\n")}")

	        if (messageList != null) {
		        for (zimMessage in messageList) {
			        if (zimMessage is ZIMTextMessage) {
				        val zimTextMessage = zimMessage as ZIMTextMessage
				        Log.e(TAG, "Received message: ${zimTextMessage.message}")
				        log("Received Extended Data: ${zimTextMessage.extendedData}")

				        try {
					        val jsonObject = JSONObject(zimMessage.extendedData)
					        val senderImage = jsonObject.getString("userImage")
					        val senderName = jsonObject.getString("userName")
					        val senderId = jsonObject.getString("userId")
					        commentList.add(CommentModel(senderImage, senderName, senderId, zimMessage.message))
					        commentAdapter.notifyItemInserted(commentList.size - 1)
					        bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
				        } catch (e: Exception) {
					        e.printStackTrace()
					        null
				        }

			        }
		        }
	        }

        }
    }

	/*fun sendMessage(message: String) {

		log("RoomID: ${roomID} Message:${message}")

		ZegoExpressEngine.getEngine()
			.sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
				override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
					if (errorCode == 0) {
						bind.text.setText("")
						commentList.add(CommentModel(userImage, userName, message))
						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
						Log.d("CHAT", "Message sent successfully")
					} else {
						Log.e("CHAT", "Failed to send message")
					}
				}
			})

	}*/

	fun sendZimMessage(content: String) {

		val zimMessage = ZIMTextMessage(content)

		val extendedData = JSONObject().apply {
			put("userImage", userImage)
			put("userId", userId)
			put("userName", userName)
		}

		zimMessage.extendedData = extendedData.toString()

		val config = ZIMMessageSendConfig().also {
			it.priority = ZIMMessagePriority.HIGH
		}

		ZIM.getInstance().sendMessage(zimMessage, roomID, ZIMConversationType.ROOM, config, object : ZIMMessageSentFullCallback {
			override fun onMessageAttached(message: ZIMMessage?) {

			}

			override fun onMessageSent(message: ZIMMessage?, errorInfo: ZIMError?) {
				if (errorInfo != null) {
					log("MESSAGE SENT SUCCESSFULLY : ${message?.conversationType}")

					bind.text.setText("")

					val jsonObject = JSONObject(message?.extendedData)
					val senderImage = jsonObject.getString("userImage")
					val senderId = jsonObject.getString("userId")
					val senderName = jsonObject.getString("userName")
					commentList.add(CommentModel(senderImage, senderName, senderId,zimMessage.message))
					commentAdapter.notifyItemInserted(commentList.size - 1)
					bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }

				} else {
					log("MESSAGE SENT ERROR : ${errorInfo.toString()}")
				}
			}

			override fun onMediaUploadingProgress(
				message: ZIMMediaMessage?,
				currentFileSize: Long,
				totalFileSize: Long
			) {

			}

			override fun onMultipleMediaUploadingProgress(
				message: ZIMMultipleMessage?,
				currentFileSize: Long,
				totalFileSize: Long,
				messageInfoIndex: Int,
				currentIndexFileSize: Long,
				totalIndexFileSize: Long
			) {

			}
		})

	}

	fun receiveZimMessage() {
		ZIM.getInstance().setEventHandler(object : ZIMEventHandler() {
			override fun onReceiveRoomMessage(
				zim: ZIM?,
				messageList: java.util.ArrayList<ZIMMessage?>?,
				fromRoomID: String?
			) {

				log("Message Received")

				if (messageList != null) {
					for (zimMessage in messageList) {
						if (zimMessage is ZIMTextMessage) {
							val zimTextMessage = zimMessage as ZIMTextMessage
							Log.e(TAG, "Received message: ${zimTextMessage.message}")

						}
					}
				}

				super.onReceiveRoomMessage(zim, messageList, fromRoomID)

			}
		})

	}

	private fun verificationDialog() {
		AppBottomSheet(
			mCtx,
			R.drawable.ic_info,
			title = when(App.profileResponse.value?.buyerIdentityStatus){

				"null" ->{
					"Become a Verified Buyer!"
				}

				"pending" ->{
					"Verification Pending!"
				}

				"rejected" ->{
					"Verification Rejected!"
				}

				else -> {
					"Become a Verified Buyer!"
				}
			},
			"Before you interact with lives shows, You need to become a Verified Buyer.",
			primaryBtnText = "Okay",
			secondaryBtnText = "Cancel",
			canCancel = true,
			showSecondary = false,
			iconPadding = 16,
			alertType = AlertType.INFO,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
					startActivity(Intent(mCtx , TrustedBuyerActivity::class.java).putExtra("slug","buyer"))
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}
		).show()

	}

	/*fun fetchMessage() {
		ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {
			override fun onIMRecvBroadcastMessage(
				roomID: String?,
				messageList: ArrayList<ZegoBroadcastMessageInfo?>?
			) {
				Log.d("ZEGO", "Broadcast message received for room: $roomID")
				if (messageList != null) {
					for (msgInfo in messageList) {
						Log.d(
							"BROADCAST",
							"Received broadcast message from ${msgInfo?.fromUser?.userName}: ${msgInfo?.message}"
						)

						val name = msgInfo?.fromUser?.userID?.split("_")?.get(0)?.replace(".", " ")

						commentList.add(
							CommentModel(
								msgInfo?.fromUser?.userName,
								name,
								msgInfo?.message
							)
						)
						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
					}
				}
			}

		})
	}*/

}