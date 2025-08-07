package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.Application
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.callback.ZIMMessageSentFullCallback
import im.zego.zim.entity.ZIMAppConfig
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
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShareSheetAdapter
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.ProductSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.databinding.ShopSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.ZIMExtendedData
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import org.json.JSONObject

class LiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityLiveShowBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()
	private lateinit var pipParams: PictureInPictureParams
	private lateinit var commentAdapter: CommentAdapter
	private lateinit var zim: ZIM
	private val updateStatusHandler = Handler(Looper.getMainLooper())
	private val handler = Handler(Looper.getMainLooper())
	private val bidTimerHandler = Handler(Looper.getMainLooper())
	private lateinit var durationRunnable: Runnable
	private var runnable: Runnable? = null
	private lateinit var bidRunnable: Runnable
	private var commentList = mutableListOf<LiveChatModel?>()
	private var liveStatus = true
	var isFrontCamera = true
	var roomID = ""
	var showId = ""
	var bidCounter = 30
	private var startTimeMillis: Long = 0L
	private var zoomLevel = 1L

	private var liveData : LiveShowModel ? = null

	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot: DataSnapshot) {
			log("Value : ${snapshot.value}")

			liveData = snapshot.getValue(LiveShowModel::class.java)

			bind.liveCount.text = liveData?.viewerCount.toString()

			val isSold = liveData?.products?.firstOrNull()?.status == "sold"

		}

		override fun onCancelled(error: DatabaseError) {

		}

	}

	private var startTimeListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot: DataSnapshot) {

			log("StartTime : ${snapshot.value}")
			if (snapshot.value != null) {
				startBidTimer()
			}else{

			}
		}

		override fun onCancelled(error: DatabaseError) {

		}

	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		immersionBar {
			transparentBar()
			navigationBarDarkIcon(true)
			navigationBarColor(clr.surface)
			supportActionBar(false)
			fitsSystemWindows(false)
			keyboardEnable(true)
		}

		initPip()

		bind.root.setMargins(0, 0, 0, navigationBarHeight)

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		showId = intent.getStringExtra("showId") ?: ""

		bind.hostName.text = userName

		bind.hostImage.loadUrl(this, userImage)

		createEngine()

		startListenEvent()

		bind.more.setOnClickListener {
			showMoreSheet()
		}

		bind.promote.setOnClickListener {
			showPromoteSheet()
		}

		bind.clip.setOnClickListener {
			createClipSheet()
		}

		bind.share.setOnClickListener {
			shareSheet()
		}

		bind.cutButton.setOnClickListener {
			endShowSheet()
			/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				enterPictureInPictureMode(pipParams)
			}*/
		}

		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
//               sendMessage(bind.text.value())
				sendZimMessage(bind.text.value())
			}
		}

		bind.cameraSwitch.setOnClickListener {

			if (isFrontCamera) {
				ZegoExpressEngine.getEngine().useFrontCamera(false)
				isFrontCamera = false

			} else {
				ZegoExpressEngine.getEngine().useFrontCamera(true)
				isFrontCamera = true

			}

		}

		bind.shop.setOnClickListener {

			showProductSheet()
//			shopSheet()

		}

		bind.loader.isVisible = true

		viewModel.generateToken(showId.request())

		bind.startBtn.setOnClickListener {

			bind.loader.isVisible = true

			viewModel.updateLiveStatus(showId.request(), "true".request())

		}

		viewModel.generateTokenRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					roomID = mData?.roomId.toString()

					log("ROOM ID FOR HOST: $roomID ")

					startPreview()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								finish()

							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

								finish()

							}
						})
					}
				}

				else -> {}

			}
		}

		viewModel.updateLiveStatusRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					if (liveStatus) {

						runSafe {
							updateFirebaseNode(mData)
							loginRoom(roomID)
							startUpdatingFirebaseEvery5Minutes()
							FireRef.LIVE_SESSIONS.child(roomID).addValueEventListener(eventListener)
							FireRef.LIVE_SESSIONS.child(roomID).child("highestBid").child("startTime").addValueEventListener(startTimeListener)
							bind.startBtn.isVisible = false
						}

					} else {
						finish()
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								finish()

							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

								finish()
							}
						})
					}
				}

				else -> {}
			}
		}

		viewModel.createBidRepo.observe(this) {
			when (it) {
				is Resource.Success -> {

					viewModel.createBidRepo.value = null

					bind.loader.isVisible = false

					FireRef.LIVE_SESSIONS.child(roomID).child("products").child("0").updateChildren(
						mapOf(
							"status" to "sold",
							"isCurrent" to false
						)
					)

					if ((liveData?.products?.size ?: 0) > 1) {


						showProductSheet()

					} else {
						Alerts.error(this, "Your Current Product has been sold, Please select next one to your shop")
					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this, TAG, object : AlertClicks {
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

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {

				if (::zim.isInitialized) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						enterPictureInPictureMode(pipParams)
					}
				} else {
					finishAfterTransition()
				}
			}
		})

	}

	override fun onPause() {
		super.onPause()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			enterPictureInPictureMode(pipParams)
		}
	}

	override fun onDestroy() {
		super.onDestroy()

		FireRef.LIVE_SESSIONS.child(roomID).removeValue()

		stopPublish()

		if (::zim.isInitialized) {
			zim.logout()
			zim.destroy()
		}

		stopLiveDurationTimer()
		stopBidTimeTimer()
		logoutRoom()
		destroyEngine()

	}

	private fun createEngine() {
		val profile = ZegoEngineProfile().apply {
			appID = Const.APP_ID.toLong()
			appSign = Const.APP_SIGN
			scenario = ZegoScenario.GENERAL
			application = applicationContext as Application
		}

		ZegoExpressEngine.createEngine(profile, null)
	}

	private fun setupZIMChat() {
		val appConfig = ZIMAppConfig().also {
			it.appID = Const.APP_ID.toLong()
			it.appSign = Const.APP_SIGN
		}

		zim = ZIM.create(appConfig, application)

		val userInfo = ZIMUserInfo().also {
			it.userID = userName.replace(" ", ".") + "_" + userId
			it.userName = userImage
		}

		zim.login(userInfo) { error ->
			if (error != null) {
				log("LOGGED INTO ZIM")
				val roomInfo = ZIMRoomInfo().also {
					it.roomID = roomID
					it.roomName = roomID + "_room"
				}

				zim.createRoom(roomInfo) { roomInfo, errorInfo ->
					if (errorInfo != null) {
						log("CREATED ROOM : $roomInfo")

						zim.setEventHandler(zimEventHandler)
					} else {
						log("CREATE ROOM ERROR : ${errorInfo.toString()}")
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

	private fun destroyEngine() {
		ZegoExpressEngine.destroyEngine(null)
	}

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
				val context = applicationContext
				for (user in userList) {
					val message = when (updateType) {
						ZegoUpdateType.ADD -> "${user.userID} logged in to the room."
						ZegoUpdateType.DELETE -> "${user.userID} logged out of the room."
						else -> ""
					}
					Toast.makeText(context, message, Toast.LENGTH_LONG).show()
				}
			}

			override fun onRoomStateChanged(
				roomID: String,
				reason: ZegoRoomStateChangedReason,
				errorCode: Int,
				extendedData: JSONObject
			) {
				super.onRoomStateChanged(roomID, reason, errorCode, extendedData)
				val context = applicationContext
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
						// Other room states can be handled here if needed
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
					Toast.makeText(
						applicationContext,
						"ZegoPublisherState.NO_PUBLISH",
						Toast.LENGTH_LONG
					).show()
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
					Toast.makeText(
						applicationContext,
						"onPlayerStateUpdate, state: $state errorCode: $errorCode",
						Toast.LENGTH_LONG
					).show()
				}

				if (state == ZegoPlayerState.NO_PLAY) {
					Toast.makeText(applicationContext, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_LONG)
						.show()
				}
			}

			override fun onIMRecvBroadcastMessage(
				roomID: String?,
				messageList: java.util.ArrayList<ZegoBroadcastMessageInfo?>?
			) {
				super.onIMRecvBroadcastMessage(roomID, messageList)
				Log.d("ZEGO", "Barrage message received for room: $roomID")
				if (messageList != null) {
					for (msg in messageList) {
						/*Log.d(
							"CHAT",
							"Received message from ${msg?.fromUser?.userName}: ${msg?.message}"
						)

						val name = msg?.fromUser?.userID?.split("_")?.get(0)?.replace(".", " ")

						commentList.add(LiveChatModel(msg?.fromUser?.userName, name, msg?.message))
						commentAdapter.notifyItemInserted(commentList.size - 1)
						bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
//                        bind.recycler.smoothScrollToPosition(commentList.lastIndex)
						// Update UI accordingly*/
					}
				}
			}
		})

	}

	private fun stopListenEvent() {
		ZegoExpressEngine.getEngine().setEventHandler(null)
	}

	fun loginRoom(roomId: String) {
		val user = ZegoUser(userName.replace(" ", ".") + "_" + userId, userImage)

		val roomConfig = ZegoRoomConfig()
		roomConfig.isUserStatusNotify = true
		ZegoExpressEngine.getEngine().loginRoom(
			roomId,
			user,
			roomConfig
		) { error: Int, extendedData: JSONObject? ->
			if (error == 0) {
				Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()

				startPublish()
				startLiveDurationTimer()

			} else {
				Toast.makeText(this, "Login failed. error = $error", Toast.LENGTH_LONG).show()
			}
		}
	}

	fun logoutRoom() {
		ZegoExpressEngine.getEngine().logoutRoom()
	}

	fun startPreview() {
		val previewCanvas = ZegoCanvas(bind.hostView).apply {
			viewMode = ZegoViewMode.ASPECT_FILL
		}
		ZegoExpressEngine.getEngine().startPreview(previewCanvas)
	}

	fun stopPreview() {
		ZegoExpressEngine.getEngine().stopPreview()
	}

	fun startPublish() {
		val previewCanvas = ZegoCanvas(bind.hostView).apply {
			viewMode = ZegoViewMode.ASPECT_FILL
		}
		ZegoExpressEngine.getEngine().startPreview(previewCanvas)

		ZegoExpressEngine.getEngine().startPublishingStream(roomID)
		setupZIMChat()
	}

	fun stopPublish() {
		ZegoExpressEngine.getEngine().stopPublishingStream()
	}

	/*	fun sendMessage(message: String) {
			log("RoomID: $roomID Message:${message}")
			ZegoExpressEngine.getEngine().sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
					@SuppressLint("NotifyDataSetChanged")
					override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
						if (errorCode == 0) {
							Log.d("CHAT", "Message sent successfully")
							bind.text.setText("")
							commentList.add(LiveChatModel(userImage, userName, message))
							commentAdapter.notifyItemInserted(commentList.size - 1)
							bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
						} else {
							Log.e("CHAT", "Failed to send message")
						}
					}
				})
		}*/

	fun updateFirebaseNode(data: UpdateLiveStatusResponse.Data?) {
		val user = data?.user

		val seller = LiveShowModel.Seller(
			id = user?.id.toString(),
			image = user?.profileImage,
			isFollowed = false,
			name = user?.name,
			rating = user?.rating ?: ""
		)

		val prdcts = data?.products?.map { it?.toLiveShowProduct() }

		prdcts?.first()?.isCurrent = true

		val liveShow = LiveShowModel(
			products = prdcts,
			roomId = roomID,
			seller = seller,
			showDetail = "",
			thumbnail = data?.thumbnail?.get(0) ?: "",
			viewerCount = 1,
			highestBid = null,
			isLive = true,
			time = Utils.timestamp().toString(),
			showId = showId
		).toMap()

		FireRef.LIVE_SESSIONS.child(roomID).updateChildren(liveShow)
	}

	fun startUpdatingFirebaseEvery5Minutes() {
		runnable = object : Runnable {
			override fun run() {
				val updateValue = Utils.timestamp().toString()
				FireRef.LIVE_SESSIONS.child(roomID).updateChildren(
					mapOf(
						"time" to Utils.timestamp().toString()
					)
				).addOnSuccessListener {
					Log.d("FirebaseUpdate", "Successfully updated value: $updateValue")
				}
					.addOnFailureListener {
						Log.e("FirebaseUpdate", "Failed to update value", it)
					}

				updateStatusHandler.postDelayed(this, 4 * 60 * 1000)
			}
		}

		runnable?.let { updateStatusHandler.post(it) }
	}

	fun stopUpdatingFirebase() {
		runnable?.let { updateStatusHandler.removeCallbacks(it) }
	}

	private fun startLiveDurationTimer() {
		startTimeMillis = System.currentTimeMillis()

		durationRunnable = object : Runnable {
			override fun run() {
				val elapsed = System.currentTimeMillis() - startTimeMillis
				val seconds = (elapsed / 1000) % 60
				val minutes = (elapsed / (1000 * 60)) % 60
				val hours = (elapsed / (1000 * 60 * 60))

				val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
				bind.duration.text = buildString {
					append("Show Time: ")
					append(formatted)
				}

				handler.postDelayed(this, 1000)
			}
		}

		handler.post(durationRunnable)
	}

	private fun startBidTimer() {

		 bidRunnable = object : Runnable {
			override fun run() {

				if (bidCounter > 0) {
					bidCounter = bidCounter -1
				}else{

					log("STOP BID TIMER")

					stopBidTimeTimer()

					if (liveData !=null){
						bind.loader.isVisible = true
						viewModel.createBid(
							showId.request(),
							liveData?.highestBid?.userId?.request(),
							liveData?.products?.get(0)?.id?.request(),
							liveData?.highestBid?.bidAmount?.request()
						)
					}

					return
				}

				log("TIME DIFFERENCE : ${bidCounter}")

				FireRef.LIVE_SESSIONS.child(roomID).updateChildren(
					mapOf(
						"bidCountDown" to bidCounter.toString()
					)
				).addOnSuccessListener {
				}
					.addOnFailureListener {
					}

				bidTimerHandler.postDelayed(this, 1000)
			}
		}

		bidRunnable.let { bidTimerHandler.post(it) }

	}

	private fun stopLiveDurationTimer() {
		if (this::durationRunnable.isInitialized) {
			handler.removeCallbacks(durationRunnable)
		}
	}

	private fun stopBidTimeTimer() {
		if (this::bidRunnable.isInitialized) {
			bidTimerHandler.removeCallbacks(bidRunnable)
		}
	}

	private fun sendZimMessage(content: String) {
		if (::zim.isInitialized) {
			val zimMessage = ZIMTextMessage(content)
			zimMessage.extendedData = ZIMExtendedData(userImage, userId, userName).toJson()

			val config = ZIMMessageSendConfig().also { it.priority = ZIMMessagePriority.HIGH }

			zim.sendMessage(
				zimMessage,
				roomID,
				ZIMConversationType.ROOM,
				config,
				object : ZIMMessageSentFullCallback {
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
		} else {
			Alerts.error(this, "Start the live streaming to send Messages")
		}
	}

	fun shopSheet() {
		val shopSheetBind = ShopSheetBinding.bind(layoutInflater.inflate(R.layout.shop_sheet, null, false))
		val shopSheet = Alerts.appBottomSheet(this, true, shopSheetBind)

		val productList = mutableListOf<GetMyInventoryResponse.Data?>()

		val categoryList = mutableListOf("Auction", "Buy Now", "Freebie", "Sold")

		categoryList.forEach { it ->
			shopSheetBind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = this, text = it, selected = false
				)
			)
		}

		shopSheetBind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}

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

		shopSheetBind.loader.isVisible = true

		viewModel.getMyInventory("active".request(), "1".request())

		viewModel.getMyInventoryRepo.observe(this) {
			when (it) {
				is Resource.Success -> {

					shopSheetBind.loader.isVisible = false

					val mData = it.value.data

					productList.clear()

					mData?.forEach {
						productList.add(it)
					}

					shopAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					shopSheetBind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this, TAG, object : AlertClicks {
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

		shopSheetBind.close.setOnClickListener {
			shopSheet.dismiss()
		}

		shopSheet.show()
	}

	fun showMoreSheet() {
		val moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu, null, false))
		val moreSheet = Alerts.appBottomSheet(this, true, moreSheetBind)

		moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}
		})

		moreSheetBind.zoomInLayout.setOnClickListener {
			zoomLevel++
			ZegoExpressEngine.getEngine().setCameraZoomFactor(zoomLevel.toFloat())
			moreSheet.dismiss()
		}

		moreSheetBind.micLayout.setOnClickListener {
			if (ZegoExpressEngine.getEngine().isMicrophoneMuted) {
				ZegoExpressEngine.getEngine().muteMicrophone(false)
				moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
			} else {
				ZegoExpressEngine.getEngine().muteMicrophone(true)
				moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
			}
//            moreSheet.dismiss()
		}

		moreSheetBind.switchCameraLayout.setOnClickListener {
			if (isFrontCamera) {
				ZegoExpressEngine.getEngine().useFrontCamera(false)
				isFrontCamera = false

			} else {
				ZegoExpressEngine.getEngine().useFrontCamera(true)
				isFrontCamera = true
			}
			moreSheet.dismiss()
		}

		moreSheetBind.close.setOnClickListener {
			moreSheet.dismiss()
		}

		moreSheet.show()
	}

	fun showPromoteSheet() {
		var promoteSheetBind = PromoteShowSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.promote_show_sheet,
				null,
				false
			)
		)

		var promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)
		var mList = mutableListOf<String?>()

		repeat(3) {
			mList.add("")
		}

		promoteSheetBind.optionList.adapter = PromoteSheetAdapter(mList, object : RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}
		})

		promoteSheetBind.close.setOnClickListener {
			promoteSheet.dismiss()
		}


		promoteSheet.show()
	}

	fun createClipSheet() {
		val clipSheetBind = CreateClipSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.create_clip_sheet,
				null,
				false
			)
		)
		val clipSheet = Alerts.appBottomSheet(this, true, clipSheetBind)
		val mList = mutableListOf<String?>()

		repeat(3) {
			mList.add("")
		}


		clipSheetBind.close.setOnClickListener {
			clipSheet.dismiss()
		}

		clipSheet.show()
	}

	fun shareSheet() {
		val shareSheetBind = ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet, null, false))
		val shareSheet = Alerts.appBottomSheet(this, true, shareSheetBind)
		val mList = mutableListOf<String?>()

		repeat(2) {
			mList.add("")
		}

		shareSheetBind.optionList.adapter = ShareSheetAdapter(mList, object : RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}
		})

		shareSheetBind.close.setOnClickListener {
			shareSheet.dismiss()
		}

		shareSheet.show()
	}

	fun endShowSheet() {
		val endShowSheetBind = EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet, null, false))
		val endShowSheet = Alerts.appBottomSheet(this, true, endShowSheetBind)

		endShowSheetBind.close.setOnClickListener {
			endShowSheet.dismiss()
		}

		endShowSheetBind.endBtn.setOnClickListener {
			endShowSheet.dismiss()
			stopUpdatingFirebase()
			liveStatus = false
			bind.loader.isVisible = true
			viewModel.updateLiveStatus(showId.request(), "false".request())
		}

		endShowSheet.show()
	}

	fun showProductSheet() {

		val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet, null, false))
		val productSheet = Alerts.appBottomSheet(this, true, productSheetBind)

		val productList = mutableListOf<LiveShowModel.Product?>()

		var selectedPos =-1

		FireRef.LIVE_SESSIONS.child(roomID).addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {

				val data = snapshot.getValue(LiveShowModel::class.java)

				if (data?.products != null) {
					productList.clear()
					productList.addAll(
						data.products!!
					)
				}

				log("LIVE ADDED PRODUCTS : ${data}")

				val productAdapter = FirebaseProductAdapter(productList, object : RecyclerClicks {
					override fun itemClick(pos: Int, status: String?) {

						if (productList[pos]?.status == "sold") {

							Alerts.error(this@LiveShowActivity, "This product is already sold")

						} else {
							productList.forEachIndexed { index, item ->

								item?.selected = index == pos
								productSheetBind.recycler.adapter?.notifyDataSetChanged()

							}
							selectedPos = pos
						}
					}

				})

				productSheetBind.recycler.adapter = productAdapter

				productSheet.show()

			}

			override fun onCancelled(error: DatabaseError) {
			}

		})

		productSheetBind.close.setOnClickListener {

			productSheet.dismiss()
		}

		productSheetBind.addBtn.setOnClickListener {

			if (selectedPos != -1){
				val updates = hashMapOf<String, Any?>(
					"highestBid" to null,
					"bidCountDown" to null
				)

				FireRef.LIVE_SESSIONS.child(roomID).child("products").child(selectedPos.toString()).updateChildren(mapOf("isCurrent" to true)).addOnSuccessListener{

					FireRef.LIVE_SESSIONS.child(roomID).updateChildren(updates)

					bidCounter = 30

				}.addOnFailureListener {

				}

				productSheet.dismiss()

			}else{
				Alerts.error(this@LiveShowActivity,"Please select a product")
			}



		}

	}

	fun initPip() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val visibleRect = Rect()
			bind.root.getGlobalVisibleRect(visibleRect)

			pipParams = PictureInPictureParams.Builder().apply {
				setAspectRatio(Rational(100, 200))
				setSourceRectHint(visibleRect)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					setAutoEnterEnabled(true)
				}
			}.build()

			setPictureInPictureParams(pipParams)
		}
	}

	override fun onPictureInPictureModeChanged(
		isInPictureInPictureMode: Boolean,
		newConfig: Configuration
	) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

		if (isInPictureInPictureMode) {
			bind.profileLayout.isVisible = false
			bind.rehearsalLayout.isVisible = false
			bind.recycler.isVisible = false
			bind.menuLayout.isVisible = false
			bind.message.isVisible = false
			App.PIPMode = true
		} else {
			bind.profileLayout.isVisible = true
			bind.rehearsalLayout.isVisible = true
			bind.recycler.isVisible = true
			bind.menuLayout.isVisible = true
			bind.message.isVisible = true
			App.PIPMode = false

		}
	}

	override fun onUserLeaveHint() {
		super.onUserLeaveHint()
		if (!isInPictureInPictureMode) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				setPictureInPictureParams(pipParams)
				enterPictureInPictureMode(pipParams)
			}
			Alerts.log(javaClass.simpleName, "STARTED IN PIP MODE")
		} else {
			Alerts.log(javaClass.simpleName, "ALREADY IN PIP MODE")
		}
	}

}