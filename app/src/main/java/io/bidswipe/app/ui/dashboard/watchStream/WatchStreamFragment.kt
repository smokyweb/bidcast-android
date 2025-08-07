package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.database.ChildEventListener
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
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.databinding.ShopSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.ZIMExtendedData
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.getCurrentProduct
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.value
import kotlin.math.abs

class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {

	override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	) = FragmentWatchStreamBinding.inflate(inflater, view, false)

	private lateinit var roomID: String
	private lateinit var streamID: String
	private var highestBidAmount: String? =  ""
	private var commentList = mutableListOf<LiveChatModel?>()
	private lateinit var commentAdapter: CommentAdapter
	private var product : LiveShowModel.Product? = null
	var productList = mutableListOf<GetMyInventoryResponse.Data?>()

	companion object {
		fun newInstance(roomID: String, streamID: String) = WatchStreamFragment().apply {
			arguments = Bundle().apply {
				putString("roomID", roomID)
				putString("streamID", streamID)
			}
		}
	}

	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot: DataSnapshot) {

			val data = LiveShowModel().fromMap(snapshot) ?: return


			// Safely update highestBidAmount
			highestBidAmount = data.highestBid?.bidAmount ?: "0"


			// Show viewer count or default to 0
			bind.liveCount.text = (data.viewerCount ?: 0).toString()

			// Find the current product once
			val currentProduct = data.products?.find { it?.id == data.highestBid?.productId }

			log("CURRENT PRODUCT Value : ${currentProduct}")


			// Determine sale status once
			val isSold = currentProduct?.status == "sold"
			bind.soldLayout.isVisible = isSold
			bind.productLayout.isVisible = !isSold

			if (isSold && data.highestBid?.userId == userId) {
				bind.soldOutText.text = "You won the bid"

			}

	/*		// Handle product sale status
			val isSold = data.products?.find { it?.isCurrent == true }?.status == "sold"
			bind.soldLayout.isVisible = isSold
			bind.productLayout.isVisible = !isSold

			if (data.highestBid == null){
				val currentProduct = data.products?.find { it?.isCurrent == true }

				bind.productName.text = currentProduct?.name

				bind.productImage.loadUrl(
					mCtx,
					currentProduct?.image.toString(),
					placeHolder = draw.product_img
				)

				bind.bidPrice.text = currentProduct?.price.toString().asMoney()

				try {
					bind.quantity.text = buildString {
						append("Price: ")
						append(currentProduct?.price.toString().asMoney())
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}

			}

			if (isSold && data.highestBid?.userId == userId) {
				bind.soldOutText.text = "You won the bid"
			}*/

			// Show bid countdown if available
			val countdown = snapshot.child("bidCountDown").value?.toString()
			if (!countdown.isNullOrEmpty()) {
				bind.bidTime.isVisible = true
				bind.bidTime.text = "Ends in $countdown"
			} else {
				bind.bidTime.isVisible = false
			}
		}

		override fun onCancelled(error: DatabaseError) {
			log("Firebase cancelled: ${error.message}")
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
		setUpSwipe()

		bind.cutButton.setOnClickListener {
			finish()
		}

		/*bind.shop.setOnClickListener {
			shopSheet()
		}*/

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		FireRef.LIVE_SESSIONS.child(roomID).addValueEventListener(eventListener)

		FireRef.LIVE_SESSIONS.child(roomID).addChildEventListener(object : ChildEventListener{
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
				if (snapshot.key == "highestBid"){

					FireRef.LIVE_SESSIONS.child(roomID).addListenerForSingleValueEvent(object : ValueEventListener {
						override fun onDataChange(snapshot: DataSnapshot) {

							val data = LiveShowModel().fromMap(snapshot)

							val currentProduct = data.products?.find { it?.isCurrent == true }

							log("CURRENT PRODUCT : ${currentProduct}")


							if (currentProduct != null) {
								bind.productName.text = currentProduct.name
								bind.productImage.loadUrl(
									mCtx,
									currentProduct.image ?: "",
									placeHolder = draw.product_img
								)
								bind.bidPrice.text = currentProduct.price.toString().asMoney()

								bind.quantity.text = buildString {
									append("Price: ")
									append(currentProduct.price.toString().asMoney())
								}

								bind.bid.setCompleted(false,true)
							}
						}

						override fun onCancelled(error: DatabaseError) {

						}
					})

				}
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
			}

			override fun onCancelled(error: DatabaseError) {
			}


		})

		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
//                sendMessage(bind.text.value())

				if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
					sendZimMessage(bind.text.value())
				} else {
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

				 product = stream.products?.find { it?.isCurrent == true }

				highestBidAmount = stream.highestBid?.bidAmount.toString()

				bind.userName.text = stream.seller?.name.toString()

				bind.productName.text = product?.name

				bind.productImage.loadUrl(
					mCtx,
					product?.image.toString(),
					placeHolder = draw.product_img
				)

				bind.bidPrice.text = (product?.price ?:"0").asMoney()

				try {
					bind.quantity.text = buildString {
						append("Price: ")
						append(product?.price.toString().asMoney())
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}

				if (stream.seller?.isFollowed == true) {
					bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.outline))
					bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
					bind.follow.text = "Unfollow"
				} else {
					bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.primary))
					bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.background))
					bind.follow.text = "Follow"
				}

				bind.follow.setOnClickListener {
					viewModel.followUser(stream.seller?.id?.request())
				}

				runSafe {
					bind.bid.text = "Swipe to Bid ${(product?.price?.toInt()?.plus(2)).toString().asMoney()}"
				}

//				productList.add(stream.products?.getCurrentProduct().let { GetMyInventoryResponse.Data(it?.id, it?.name, it?.price, it?.image) })

				bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
					override fun onSlideComplete(view: SlideToActView) {

						log("SWIPED")

						val ref = FireRef.LIVE_SESSIONS.child(roomID).child("highestBid")

						ref.addListenerForSingleValueEvent(object : ValueEventListener {
							override fun onDataChange(snapshot: DataSnapshot) {
								val bidData = mutableMapOf<String, Any?>(
									"bidAmount" to highestBidAmount,
									"userName" to userName,
									"userImage" to userImage,
									"userId" to userId,
									"productId" to product?.id.toString()
								)

								// If startTime doesn't exist, it's a new bid; otherwise, update existing
								if (!snapshot.hasChild("startTime")) {
									bidData["startTime"] = Utils.timestamp().toString()
									bidData["productStatus"] = "processed"
								}

								ref.updateChildren(bidData)
							}

							override fun onCancelled(error: DatabaseError) {
								log("Firebase Error: ${error.message}")
							}

						})

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
						bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.outline))
						bind.follow.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
						bind.follow.text = "Unfollow"
					} else {
						bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.primary))
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

		if (App.profileResponse.value?.buyerIdentityStatus != "verified") {
			verificationDialog()
		}

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
//		startListenEvent()
		if (viewModel.previousRoomId.isNotEmpty()) {
			val roomInfo = ZIMRoomInfo().also {
				it.roomID = roomID
				it.roomName = roomID + "_room"
			}
			ZIM.getInstance().switchRoom(viewModel.previousRoomId, roomInfo, false, null) { roomInfo, errorInfo ->
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
			// Callback for receiving in-room messages.
			messageList?.forEach { zimMessage ->
				if (zimMessage is ZIMTextMessage) {
					log("NEW MESSAGE RECEIVED:\n${zimMessage.message}\nExtended Data: ${zimMessage.extendedData}")

					runSafe {
						commentList.add(LiveChatModel.fromZIMMessage(zimMessage))

						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post {
							bind.recycler.smoothScrollToPosition(commentList.size)
						}
					}
				}
			}
		}
	}

	fun sendZimMessage(content: String) {
		val zimMessage = ZIMTextMessage(content)
		zimMessage.extendedData = ZIMExtendedData(userImage, userId, userName).toJson()

		val config = ZIMMessageSendConfig().also { it.priority = ZIMMessagePriority.HIGH }

		ZIM.getInstance().sendMessage(zimMessage, roomID, ZIMConversationType.ROOM, config, object : ZIMMessageSentFullCallback {
			override fun onMessageAttached(message: ZIMMessage?) {

			}

			override fun onMessageSent(message: ZIMMessage?, errorInfo: ZIMError?) {
				if (errorInfo != null) {
					log("MESSAGE SENT SUCCESSFULLY : ${message?.conversationType}")

					bind.text.setText("")

					runSafe {
						commentList.add(LiveChatModel.fromZIMMessage(zimMessage))

						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post {
							bind.recycler.smoothScrollToPosition(commentList.size)
						}
					}
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

	private fun verificationDialog() {
		AppBottomSheet(
			mCtx,
			R.drawable.ic_info,
			title = when (App.profileResponse.value?.buyerIdentityStatus) {

				"null" -> {
					"Become a Verified Buyer!"
				}

				"pending" -> {
					"Verification Pending!"
				}

				"rejected" -> {
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
					startActivity(Intent(mCtx, TrustedBuyerActivity::class.java).putExtra("slug", "buyer"))
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}
		).show()

	}

	/*fun sendMessage(message: String) {

		log("RoomID: ${roomID} Message:${message}")

		ZegoExpressEngine.getEngine()
			.sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
				override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
					if (errorCode == 0) {
						bind.text.setText("")
						commentList.add(LiveChatModel(userImage, userName, message))
						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
						Log.d("CHAT", "Message sent successfully")
					} else {
						Log.e("CHAT", "Failed to send message")
					}
				}
			})

    fun fetchMessage() {
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
							LiveChatModel(
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

/*
	private fun startListenEvent() {
		ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {

			override fun onRoomStreamUpdate(
				roomID: String,
				updateType: ZegoUpdateType,
				streamList: ArrayList<ZegoStream>,
				extendedData: JSONObject
			) {
				super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData)
				if (streamList.isNotEmpty()) {
					streamList[0].streamID
				}
			}

			override fun onRoomUserUpdate(
				roomID: String,
				updateType: ZegoUpdateType,
				userList: ArrayList<ZegoUser>
			) {
				super.onRoomUserUpdate(roomID, updateType, userList)

			}

			override fun onRoomStateChanged(
				roomID: String,
				reason: ZegoRoomStateChangedReason,
				errorCode: Int,
				extendedData: JSONObject
			) {
				super.onRoomStateChanged(roomID, reason, errorCode, extendedData)
				when (reason) {
					ZegoRoomStateChangedReason.LOGIN_FAILED ->
						Toast.makeText(
							context,
							"ZegoRoomStateChangedReason.LOGIN_FAILED",
							Toast.LENGTH_LONG
						).show()

					ZegoRoomStateChangedReason.RECONNECT_FAILED ->
						Toast.makeText(
							context,
							"ZegoRoomStateChangedReason.RECONNECT_FAILED",
							Toast.LENGTH_LONG
						).show()

					ZegoRoomStateChangedReason.KICK_OUT ->
						Toast.makeText(
							context,
							"ZegoRoomStateChangedReason.KICK_OUT",
							Toast.LENGTH_LONG
						).show()

					else -> {
					}
				}
			}

			override fun onPublisherStateUpdate(
				streamID: String,
				state: ZegoPublisherState,
				errorCode: Int,
				extendedData: JSONObject
			) {
				super.onPublisherStateUpdate(streamID, state, errorCode, extendedData)
				if (errorCode != 0) {
					// Handle publish error
				}

				if (state == ZegoPublisherState.NO_PUBLISH) {

				}
			}

			override fun onPlayerStateUpdate(
				streamID: String,
				state: ZegoPlayerState,
				errorCode: Int,
				extendedData: JSONObject
			) {
				super.onPlayerStateUpdate(streamID, state, errorCode, extendedData)

				if (errorCode != 0) {

				}

				if (state == ZegoPlayerState.NO_PLAY) {

				}
			}

		})

	}
*/

	@SuppressLint("ClickableViewAccessibility")
	fun setUpSwipe() {

		var downX = 0f

		bind.viewFlipper.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					downX = event.x
					true
				}
				MotionEvent.ACTION_UP -> {
					val deltaX = event.x - downX

					if (abs(deltaX) > 100) {
						if (deltaX > 0) {
							bind.viewFlipper.setInAnimation(mCtx, R.anim.slide_in_left)
							bind.viewFlipper.setOutAnimation(mCtx, R.anim.slide_out_right)
							bind.viewFlipper.showPrevious()
						} else {

							bind.viewFlipper.setInAnimation(mCtx, R.anim.slide_in_right)
							bind.viewFlipper.setOutAnimation(mCtx, R.anim.slide_out_left)
							bind.viewFlipper.showNext()
						}
					}
					true
				}
				else -> false
			}
		}

		bind.controls.setOnTouchListener { _, event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					downX = event.x
					true
				}
				MotionEvent.ACTION_UP -> {
					val deltaX = event.x - downX

					if (abs(deltaX) > 100) {
						if (deltaX > 0) {
							bind.viewFlipper.setInAnimation(mCtx, R.anim.slide_in_left)
							bind.viewFlipper.setOutAnimation(mCtx, R.anim.slide_out_right)
							bind.viewFlipper.showPrevious()
						} else {

							bind.viewFlipper.setInAnimation(mCtx, R.anim.slide_in_right)
							bind.viewFlipper.setOutAnimation(mCtx, R.anim.slide_out_left)
							bind.viewFlipper.showNext()
						}
					}
					true
				}
				else -> false
			}
		}

	}

	fun shopSheet() {
		val shopSheetBind = ShopSheetBinding.bind(layoutInflater.inflate(R.layout.shop_sheet, null, false))
		val shopSheet = Alerts.appBottomSheet(mCtx, true, shopSheetBind)

		val shopAdapter = ShopSheetAdapter(productList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				productList.forEachIndexed { index, item ->

					item?.selected = index == pos

					shopSheetBind.recycler.adapter?.notifyDataSetChanged()

				}

			}

		})

		shopSheetBind.recycler.adapter = shopAdapter

		/*shopSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}
		})*/


		shopSheetBind.close.setOnClickListener {
			shopSheet.dismiss()
		}

		shopSheet.show()
	}

}