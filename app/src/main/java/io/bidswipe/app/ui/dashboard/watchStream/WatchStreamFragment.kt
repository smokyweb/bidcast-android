package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
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
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.callback.ZIMLoggedInCallback
import im.zego.zim.callback.ZIMRoomJoinedCallback
import im.zego.zim.entity.ZIMAppConfig
import im.zego.zim.entity.ZIMError
import im.zego.zim.entity.ZIMMessage
import im.zego.zim.entity.ZIMMessageReceivedInfo
import im.zego.zim.entity.ZIMRevokeMessage
import im.zego.zim.entity.ZIMRoomFullInfo
import im.zego.zim.entity.ZIMTextMessage
import im.zego.zim.entity.ZIMUserInfo
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.CommentModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

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

    private lateinit var zim: ZIM

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
				sendZimMessage()
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

	}

	override fun onResume() {
		super.onResume()

		loginAndPlay()

		receiveZimMessage()
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

        setupZIMChat()
	}

	private fun stopStream() {
		ZegoExpressEngine.getEngine().stopPlayingStream(roomID)
		ZegoExpressEngine.getEngine().logoutRoom(roomID)
	}

	private fun destroyEngine() {
		ZegoExpressEngine.destroyEngine(null)
	}

    private fun setupZIMChat() {
        val appConfig = ZIMAppConfig().also {
            it.appID = Const.APP_ID.toLong()
            it.appSign = Const.APP_SIGN
        }

        zim = ZIM.create(appConfig, activity?.application)

        val userInfo = ZIMUserInfo().also {
            it.userID = userName.replace(" ", ".") + "_" + userId
            it.userName = userImage
        }

        zim.login(userInfo) { error ->
            if (error != null) {
                log("LOGGED INTO ZIM")
                zim.joinRoom(roomID) { roomInfo, errorInfo ->
                    if (errorInfo != null) {
                        log("JOINED ROOM CHAT $roomInfo")

	                    zim.setEventHandler(zimEventHandler)
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

	fun sendZimMessage() {

		/*   val conversationID = roomID

		   val zimMessage = ZIMTextMessage()
		   zimMessage.message = "Message content"
		   val config = ZIMMessageSendConfig()
		   config.priority = ZIMMessagePriority.LOW
		   val pushConfig = ZIMPushConfig()
		   pushConfig.title = "Title of the offline push"
		   pushConfig.content = "Content of the offline push"
		   config.pushConfig = pushConfig*/

		/* ZIM.getInstance().sendRoomMessage(
			 zimMessage,
			 roomID,
			 config,
			 object : ZIMMessageSentCallback {
				 override fun onMessageAttached(message: ZIMMessage?) {

				 }

				 override fun onMessageSent(
					 message: ZIMMessage?,
					 errorInfo: ZIMError?
				 ) {

				 }

			 })*/


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