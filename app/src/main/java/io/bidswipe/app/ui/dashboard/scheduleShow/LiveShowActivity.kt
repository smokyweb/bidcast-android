package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShareSheetAdapter
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
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
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import com.google.firebase.database.FirebaseDatabase
import io.bidswipe.app.utils.ChatManager
import io.bidswipe.app.utils.FirebaseLiveSessionManager
import io.bidswipe.app.utils.StreamingManager

class LiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityLiveShowBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()

	// Managers
	private lateinit var streamingManager: StreamingManager
	private lateinit var chatManager: ChatManager
	private lateinit var firebaseLiveSessionManager: FirebaseLiveSessionManager

	private lateinit var commentAdapter: CommentAdapter
	private val handler = Handler(Looper.getMainLooper())
	private var durationRunnable: Runnable? = null
	private var liveStatus = true
	private var isFrontCamera = true
	private var roomID = ""
	private var showId = ""
	private var startTimeMillis: Long = 0L
	private var zoomLevel = 1L
	private var commentList = mutableListOf<LiveChatModel?>()

	private lateinit var pipParams: PictureInPictureParams

	private val updateStatusHandler = Handler(Looper.getMainLooper())
	private var runnable: Runnable? = null

	private val eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot: DataSnapshot) {
			log("Value : ${snapshot.value}")

			val data = snapshot.getValue(LiveShowModel::class.java)

			bind.liveCount.text = data?.viewerCount.toString()

			/* if (data?.product?.status == "sold") {
				 bind.soldLayout.isVisible = true
				 bind.productLayout.isVisible = false
			 } else {
				 bind.soldLayout.isVisible = false
				 bind.productLayout.isVisible = true
			 }*/

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

		// Initialize managers
		streamingManager = StreamingManager(this, application)
		chatManager = ChatManager(application, Const.APP_ID.toLong(), Const.APP_SIGN)
		firebaseLiveSessionManager = FirebaseLiveSessionManager(FirebaseDatabase.getInstance().getReference("LIVE_SESSIONS"))

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		showId = intent.getStringExtra("showId") ?: ""

		// Set up chat callbacks
		chatManager.onMessageReceived = { model ->
			commentList.add(model)
			commentAdapter.notifyItemInserted(commentList.size - 1)
			bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
		}
		chatManager.onMessageSent = { model ->
			bind.text.setText("")
			commentList.add(model)
			commentAdapter.notifyItemInserted(commentList.size - 1)
			bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
		}

		// Set up streaming callbacks
		streamingManager.onUserJoined = { userId ->
			Toast.makeText(this, "$userId logged in to the room.", Toast.LENGTH_LONG).show()
		}
		streamingManager.onUserLeft = { userId ->
			Toast.makeText(this, "$userId logged out of the room.", Toast.LENGTH_LONG).show()
		}
		streamingManager.onStreamError = { errorMsg ->
			Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
		}
		streamingManager.onPublisherStateChanged = { state, errorCode ->
			if (state == im.zego.zegoexpress.constants.ZegoPublisherState.NO_PUBLISH) {
				Toast.makeText(this, "ZegoPublisherState.NO_PUBLISH", Toast.LENGTH_LONG).show()
			}
		}
		streamingManager.onPlayerStateChanged = { state, errorCode ->
			if (state == im.zego.zegoexpress.constants.ZegoPlayerState.NO_PLAY) {
				Toast.makeText(this, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_LONG).show()
			}
		}
		streamingManager.onRoomStateChanged = { reason, errorCode ->
			when (reason) {
				im.zego.zegoexpress.constants.ZegoRoomStateChangedReason.LOGIN_FAILED ->
					Toast.makeText(this, "ZegoRoomStateChangedReason.LOGIN_FAILED", Toast.LENGTH_LONG).show()
				im.zego.zegoexpress.constants.ZegoRoomStateChangedReason.RECONNECT_FAILED ->
					Toast.makeText(this, "ZegoRoomStateChangedReason.RECONNECT_FAILED", Toast.LENGTH_LONG).show()
				im.zego.zegoexpress.constants.ZegoRoomStateChangedReason.KICK_OUT ->
					Toast.makeText(this, "ZegoRoomStateChangedReason.KICK_OUT", Toast.LENGTH_LONG).show()
				else -> {}
			}
		}

		streamingManager.createEngine(Const.APP_ID.toLong(), Const.APP_SIGN)
		chatManager.initializeZIM()

		bind.more.setOnClickListener { showMoreSheet() }
		bind.promote.setOnClickListener { showPromoteSheet() }
		bind.clip.setOnClickListener { createClipSheet() }
		bind.share.setOnClickListener { shareSheet() }
		bind.cutButton.setOnClickListener { endShowSheet() }
		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
				chatManager.sendTextMessage(
					content = bind.text.value(),
					extendedData = ZIMExtendedData(userImage, userId, userName).toJson(),
					roomId = roomID
				)
			}
		}

		bind.cameraSwitch.setOnClickListener {
			streamingManager.toggleCamera()
			isFrontCamera = streamingManager.isUsingFrontCamera()
		}

		bind.shop.setOnClickListener {

			shopSheet()

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

					loginRoom(mData?.roomId.toString())

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
							firebaseLiveSessionManager.updateLiveSessionNode(roomID, mData)
							startPublish()
							startLiveDurationTimer()
							firebaseLiveSessionManager.startPeriodicTimeUpdate(roomID)
							firebaseLiveSessionManager.listenToLiveSession(roomID) { data ->
								bind.liveCount.text = data?.viewerCount.toString()
							}
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

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {

				if (::pipParams.isInitialized) {
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

		firebaseLiveSessionManager.removeLiveSession(roomID)

		stopPublish()

		chatManager.logout()
		chatManager.destroy()

		stopLiveDurationTimer()
		streamingManager.logoutRoom()
		streamingManager.destroyEngine()
		firebaseLiveSessionManager.removeListener()
		firebaseLiveSessionManager.stopPeriodicTimeUpdate()

	}

	private fun loginRoom(roomId: String) {
		streamingManager.loginRoom(
			roomId = roomId,
			userId = userName.replace(" ", ".") + "_" + userId,
			userName = userImage,
			userImage = userImage
		) { error, _ ->
			if (error == 0) {
				Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()
				streamingManager.startPreview(bind.hostView)
				setupZIMChat()
			} else {
				Toast.makeText(this, "Login failed. error = $error", Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun setupZIMChat() {
		chatManager.initializeZIM()
		chatManager.login(
			userId = userName.replace(" ", ".") + "_" + userId,
			userName = userImage
		) { error ->
			if (error == null) {
				chatManager.createRoom(roomID, roomID + "_room") { _, errorInfo ->
					// No need to set event handler here; handled by ChatManager
				}
			}
		}
	}

	private fun startPublish() {
		streamingManager.startPublishingStream(roomID, bind.hostView)
	}

	private fun stopPublish() {
		streamingManager.stopPublishingStream()
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
		handler.post(durationRunnable!!)
	}

	private fun stopLiveDurationTimer() {
		durationRunnable?.let { handler.removeCallbacks(it) }
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
			firebaseLiveSessionManager.stopPeriodicTimeUpdate()
			liveStatus = false
			bind.loader.isVisible = true
			viewModel.updateLiveStatus(showId.request(), "false".request())
		}
		endShowSheet.show()
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
		} else {
			bind.profileLayout.isVisible = true
			bind.rehearsalLayout.isVisible = true
			bind.recycler.isVisible = true
			bind.menuLayout.isVisible = true
			bind.message.isVisible = true
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