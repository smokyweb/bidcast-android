package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import io.bidswipe.app.utils.StreamingManager
import im.zego.zim.entity.ZIMTextMessage
import io.bidswipe.app.utils.ChatManager
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.ProductSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.SendTipSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.databinding.ShopSheetBinding
import io.bidswipe.app.databinding.ShowConfirmationAlertBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.model.PromoteShowModel
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
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import org.json.JSONObject

@SuppressLint("NotifyDataSetChanged" , "ClickableViewAccessibility")
class LiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityLiveShowBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()

	private lateinit var pipParams : PictureInPictureParams
	private lateinit var commentAdapter : CommentAdapter
	private var chatManager : ChatManager? = null
	private var streamingManager : StreamingManager? = null
	private val updateStatusHandler = Handler(Looper.getMainLooper())
	private val handler = Handler(Looper.getMainLooper())
	private val bidTimerHandler = Handler(Looper.getMainLooper())
	private lateinit var durationRunnable : Runnable
	private var runnable : Runnable? = null
	private lateinit var bidRunnable : Runnable
	private var commentList = mutableListOf<LiveChatModel?>()
	private var liveStatus = true
	var isFrontCamera = true
	var roomID = ""
	var showId = ""
	var showTime = ""
	var bidCounter = 30
	private var startTimeMillis : Long = 0L
	private var zoomLevel = 1L

	private var liveData : LiveShowModelOld? = null

	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {
			log("Value : ${snapshot.value}")

			liveData = LiveShowModelOld().fromMap(snapshot)

			bind.liveCount.text = liveData?.viewerCount.toString()

		}

		override fun onCancelled(error : DatabaseError) {

		}

	}

	private var startTimeListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {

			log("StartTime : ${snapshot.value}")
			if (snapshot.value != null) {
				startBidTimer()
			}
		}

		override fun onCancelled(error : DatabaseError) {

		}

	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(bind.root)

		immersionBar {
			transparentBar()
			navigationBarColor(R.color.transparent)
			supportActionBar(false)
			keyboardEnable(true)
		}
		
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView){ v, insets  ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			
			bind.profileLayout.setMargins(top = system.top)
			bind.startBtn.setMargins(resources.dpToPx(16) , resources.dpToPx(0) , resources.dpToPx(16) , system.bottom)
			
			insets
		}

		initPip()

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		showId = intent.getStringExtra("showId") ?: ""
		showTime = intent.getStringExtra("time") ?: ""

		log("SHOW ID : $showId")

		bind.hostName.text = userName

		bind.controls.setHapticClickListener {
			hideKeyboard()
		}

		bind.clip.isVisible = App.profileResponse.value?.preferences?.enableClips == true

		bind.recycler.setOnTouchListener { view , event ->
			hideKeyboard()
			return@setOnTouchListener false
		}

		bind.hostImage.loadUrl(this , userImage)

		initializeStreamingManager()

		bind.more.setHapticClickListener {
			showMoreSheet()
		}

		bind.promote.setHapticClickListener {
			showPromoteSheet()
		}

		bind.clip.setHapticClickListener {
			createClipSheet()
		}

		bind.share.setHapticClickListener {

			val shareText = buildString {
				append(Const.BASE_URL)
				append("/live-show?showId=$showId")
			}

			val shareIntent = Intent().apply {
				action = Intent.ACTION_SEND
				putExtra(Intent.EXTRA_TEXT , shareText)
				type = "text/plain"
			}

			val chooserIntent = Intent.createChooser(shareIntent , "Share via")

			if (shareIntent.resolveActivity(packageManager) != null) {
				startActivity(chooserIntent)
			} else {
				errorToast("No sharing apps available")
			}
		}

		bind.cutButton.setHapticClickListener {
			if (chatManager != null) {
				endShowSheet()
			} else {
				finishAfterTransition()
			}

		}

		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
				val extended = ZIMExtendedData(userImage , userId , userName).toJson()
				chatManager?.sendTextMessage(roomID , bind.text.value() , extended)
			}
		}

		bind.cameraSwitch.setHapticClickListener {
			streamingManager?.toggleCamera()
			isFrontCamera = streamingManager?.isUsingFrontCamera() ?: true
		}

		bind.shop.setHapticClickListener {

			if (chatManager != null) {
				showProductSheet()
			} else {
				Alerts.error(this , "Please start live show to access this feature")
			}

		}

		bind.loader.isVisible = true

		viewModel.generateToken(showId.request())

		bind.startBtn.setHapticClickListener {

			showConfirmationAlert()

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
						it.parse(this , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
								finish()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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
					bind.message.setMargins(resources.dpToPx(16) , resources.dpToPx(16) , resources.dpToPx(16) , navigationBarHeight)

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
						it.parse(this , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
								finish()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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

					val index = liveData?.products?.indexOfFirst { product -> product?.isCurrent == true }

					FireRef.LIVE_SESSIONS.child(roomID).child("products").child(index.toString()).updateChildren(
						mapOf(
							"status" to "sold" ,
							"isCurrent" to false
						)
					)

					if ((liveData?.products?.size ?: 0) > 1) {

						showProductSheet()

					} else {
						Alerts.error(this , "Your Current Product has been sold, Please select next one to your shop")
					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}
						})
					}
				}

				else -> {}

			}
		}

		onBackPressedDispatcher.addCallback(this , object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {

				if (chatManager != null) {
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
		chatManager?.shutdown()
		streamingManager?.destroyEngine()

		stopLiveDurationTimer()
		stopBidTimeTimer()
		logoutRoom()

	}

	private fun initializeStreamingManager() {

		streamingManager = StreamingManager(this)

		streamingManager?.createEngine(Const.APP_ID.toLong() , Const.APP_SIGN , ZegoScenario.GENERAL)

		// Set up streaming event handlers
		streamingManager?.onUserJoined = { userId ->
			Toast.makeText(this , "$userId logged in to the room." , Toast.LENGTH_LONG).show()
		}

		streamingManager?.onUserLeft = { userId ->
			Toast.makeText(this , "$userId logged out of the room." , Toast.LENGTH_LONG).show()
		}

		streamingManager?.onStreamError = { error ->
			Toast.makeText(this , "Stream error: $error" , Toast.LENGTH_LONG).show()
		}

		streamingManager?.onPublisherStateChanged = { state , errorCode ->
			if (errorCode != 0) {
				Toast.makeText(this , "Publisher state: $state, error: $errorCode" , Toast.LENGTH_LONG).show()
			}
		}

		streamingManager?.onRoomStateChanged = { reason , errorCode ->
			when (reason) {
				ZegoRoomStateChangedReason.LOGIN_FAILED ->
					Toast.makeText(this , "Login failed" , Toast.LENGTH_LONG).show()

				ZegoRoomStateChangedReason.RECONNECT_FAILED ->
					Toast.makeText(this , "Reconnect failed" , Toast.LENGTH_LONG).show()

				ZegoRoomStateChangedReason.KICK_OUT ->
					Toast.makeText(this , "Kicked out" , Toast.LENGTH_LONG).show()

				else -> {}
			}
		}
	}

	// Streaming functionality now handled by StreamingManager

	private fun initializeChat() {
		if (chatManager == null) {
			chatManager = ChatManager(
				application = application ,
				appId = Const.APP_ID.toLong() ,
				appSign = Const.APP_SIGN ,
				userId = userId ,
				userName = userName ,
				userImage = userImage
			)
		}

		chatManager?.setListener(object : ChatManager.Listener {
			override fun onMessageReceived(message : ZIMTextMessage) {
				log("NEW MESSAGE RECEIVED:\n${message.message}\nExtended Data: ${message.extendedData}")
				runSafe {
					commentList.add(LiveChatModel.fromZIMMessage(message))
					commentAdapter.notifyItemInserted(commentList.size - 1)
					bind.recycler.post {
						bind.recycler.smoothScrollToPosition(commentList.size)
					}
					// Clear input when we reflect the sent message in UI
					bind.text.setText("")
				}
			}

			override fun onRoomStateChanged(state : String) {
				log("ROOM STATE CHANGED: $state")
			}
		})
		chatManager?.initializeAndLogin(roomID) {
			val extended = ZIMExtendedData(userImage , userId , userName).toJson()
			chatManager?.sendTextMessage(roomID , "Active \uD83D\uDC4B" , extended)
		}
	}
	// Event handling now managed by StreamingManager

	fun loginRoom(roomId : String) {
		streamingManager?.loginRoom(
			roomId ,
			userId ,
			userName ,
			userImage
		) { error : Int , extendedData : JSONObject? ->
			if (error == 0) {
				Toast.makeText(this , "Login successful." , Toast.LENGTH_LONG).show()

				log("LOGIN Successful")

				startPublish()
				startLiveDurationTimer()

			} else {
				Toast.makeText(this , "Login failed. error = $error" , Toast.LENGTH_LONG).show()
			}
		}
	}

	fun logoutRoom() {
		streamingManager?.logoutRoom()
	}

	fun startPreview() {
		streamingManager?.startPreview(bind.hostView)
	}

	fun stopPreview() {
		streamingManager?.stopPreview()
	}

	fun startPublish() {
		streamingManager?.startPublishingStream(roomID , bind.hostView)
		initializeChat()
	}

	fun stopPublish() {
		streamingManager?.stopPublishingStream()
	}

	fun updateFirebaseNode(data : UpdateLiveStatusResponse.Data?) {
		val user = data?.user

		val seller = LiveShowModelOld.Seller(
			id = user?.id.toString() ,
			image = user?.profileImage ,
			isFollowed = false ,
			name = user?.name ,
			rating = user?.rating ?: ""
		)

		val products = data?.products?.map { it?.toLiveShowProduct() }

		products?.first()?.isCurrent = true

		val liveShow = LiveShowModelOld(
			products = products ,
			roomId = roomID ,
			seller = seller ,
			showDetail = "" ,
			thumbnail = data?.thumbnail?.get(0) ?: "" ,
			viewerCount = 1 ,
			highestBid = null ,
			isLive = true ,
			time = Utils.timestamp().toString() ,
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
					Log.d("FirebaseUpdate" , "Successfully updated value: $updateValue")
				}.addOnFailureListener {
					Log.e("FirebaseUpdate" , "Failed to update value" , it)
				}

				updateStatusHandler.postDelayed(this , 4 * 60 * 1000)

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

				val formatted = String.format("%02d:%02d:%02d" , hours , minutes , seconds)
				bind.duration.text = buildString {
					append("Show Time: ")
					append(formatted)
				}

				handler.postDelayed(this , 1000)
			}
		}

		handler.post(durationRunnable)
	}

	private fun startBidTimer() {

		bidRunnable = object : Runnable {
			override fun run() {

				if (bidCounter > 0) {
					bidCounter = bidCounter - 1
				} else {

					log("STOP BID TIMER")

					stopBidTimeTimer()

					if (liveData != null) {
						bind.loader.isVisible = true

						viewModel.createBid(
							showId.request() ,
							liveData?.highestBid?.userId?.request() ,
							liveData?.highestBid?.productId?.request() ,
							liveData?.highestBid?.bidAmount?.request()
						)
					}
					return
				}

				log("TIME DIFFERENCE : $bidCounter")

				FireRef.LIVE_SESSIONS.child(roomID).updateChildren(
					mapOf(
						"bidCountDown" to bidCounter.toString()
					)
				)
				bidTimerHandler.postDelayed(this , 1000)
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

	fun shopSheet() {
		val shopSheetBind = ShopSheetBinding.bind(layoutInflater.inflate(R.layout.shop_sheet , null , false))
		val shopSheet = Alerts.appBottomSheet(this , true , shopSheetBind)

		val productList = mutableListOf<GetMyInventoryResponse.Data?>()

		val categoryList = mutableListOf("Auction" , "Buy Now" , "Freebie" , "Sold")

		categoryList.forEach {
			it
			shopSheetBind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = this , text = it , selected = false
				)
			)
		}

		shopSheetBind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}

		val shopAdapter = ShopSheetAdapter(productList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {

				productList.forEachIndexed { index , item ->

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

		viewModel.getMyInventory("active".request() , "1".request())

		viewModel.getMyInventoryRepo.observe(this) {
			when (it) {
				is Resource.Success -> {

					shopSheetBind.loader.isVisible = false

					val mData = it.value.data

					productList.clear()

					mData?.forEach { product ->
						productList.add(product)
					}

					shopAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					shopSheetBind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

		shopSheetBind.close.setHapticClickListener {
			shopSheet.dismiss()
		}

		shopSheet.show()
	}

	fun showMoreSheet() {
		val moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu , null , false))
		val moreSheet = Alerts.appBottomSheet(this , true , moreSheetBind)

		moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu , object : RecyclerClicks {

			override fun itemClick(pos : Int , status : String?) {

				when (pos) {
					0 ->{
						if (chatManager != null) {
							endShowSheet()
							moreSheet.dismiss()
						} else {
							finishAfterTransition()
						}
					}
					else -> {

					}
				}

			}
		})

		moreSheetBind.allowVerifiedUser.setOnCheckedChangeListener { view , isChecked ->

			FireRef.LIVE_SESSIONS.child(roomID).updateChildren(mapOf("allowBidForAll" to ! isChecked))

		}

		log(liveData?.allowBidForAll.toString())

		moreSheetBind.allowVerifiedUser.isChecked = liveData?.allowBidForAll == false

		if (streamingManager?.isMicrophoneMuted() == false) {
			moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
		} else {
			moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
		}

		moreSheetBind.zoomInLayout.setHapticClickListener {
			streamingManager?.zoomIn()
			moreSheet.dismiss()
		}

		moreSheetBind.micLayout.setHapticClickListener {
			val isMuted = streamingManager?.isMicrophoneMuted() ?: false
			streamingManager?.muteMicrophone(! isMuted)

			if (! isMuted) {
				moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
			} else {
				moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
			}
		}

		moreSheetBind.zoomOut.setHapticClickListener {
			streamingManager?.zoomOut()
			moreSheet.dismiss()
		}

		moreSheetBind.close.setHapticClickListener {
			moreSheet.dismiss()
		}

		moreSheet.show()
	}

	fun showPromoteSheet() {
		val promoteSheetBind = PromoteShowSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.promote_show_sheet ,
				null ,
				false
			)
		)

		val promoteSheet = Alerts.appBottomSheet(this , true , promoteSheetBind)
		val mList = mutableListOf<PromoteShowModel>(
			PromoteShowModel(
				"15 Minute Boost" ,
				"Quick visibility boost" ,
				"Get featured in the top shows for 15 minutes" ,
				"$3.99" ,
				listOf(R.color.boost_15_start , R.color.boost_15_end) ,
				R.drawable.ic_flash
			) ,
			PromoteShowModel(
				"Full Show Promote" ,
				"Extended visibility" ,
				"Stay featured for your entire show duration" ,
				"$7.99" ,
				listOf(R.color.boost_full_start , R.color.boost_full_end) ,
				R.drawable.ic_star
			) ,
			PromoteShowModel(
				"Community Boost" ,
				"Power of the crowd" ,
				"Rally your community for massive exposure" ,
				"$12.99" ,
				listOf(R.color.boost_community_start , R.color.boost_community_end) ,
				R.drawable.ic_people
			)
		)

		promoteSheetBind.optionList.adapter = PromoteSheetAdapter(mList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {

			}
		})

		promoteSheetBind.close.setHapticClickListener {
			promoteSheet.dismiss()
		}

		promoteSheet.show()
	}

	fun sendTipSheet() {
		val sendTipSheetBind = SendTipSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.send_tip_sheet ,
				null ,
				false
			)
		)

		val sendTipSheet = Alerts.appBottomSheet(this , true , sendTipSheetBind)
		
		sendTipSheetBind.close.setHapticClickListener {
			sendTipSheet.dismiss()
		}

		sendTipSheet.show()
	}

	fun createClipSheet() {
		val clipSheetBind = CreateClipSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.create_clip_sheet ,
				null ,
				false
			)
		)
		val clipSheet = Alerts.appBottomSheet(this , true , clipSheetBind)
		val mList = mutableListOf<String?>()

		repeat(3) {
			mList.add("")
		}

		clipSheetBind.close.setHapticClickListener {
			clipSheet.dismiss()
		}

		clipSheet.show()
	}

	fun shareSheet() {
		val shareSheetBind = ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet , null , false))
		val shareSheet = Alerts.appBottomSheet(this , true , shareSheetBind)
		val mList = mutableListOf<String?>()

		repeat(2) {
			mList.add("")
		}

//		shareSheetBind.optionList.adapter = ShareSheetAdapter(mList , object : RecyclerClicks {
//			override fun itemClick(pos : Int , status : String?) {}
//		})

//		shareSheetBind.close.setHapticClickListener {
//			shareSheet.dismiss()
//		}

		shareSheet.show()
	}

	fun endShowSheet() {
		val endShowSheetBind = EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet , null , false))
		val endShowSheet = Alerts.appBottomSheet(this , true , endShowSheetBind)

		endShowSheetBind.close.setHapticClickListener {
			endShowSheet.dismiss()
		}

		endShowSheetBind.endBtn.setHapticClickListener {
			endShowSheet.dismiss()
			stopUpdatingFirebase()
			liveStatus = false
			bind.loader.isVisible = true
			viewModel.updateLiveStatus(showId.request() , "false".request())
		}

		endShowSheet.show()
	}

	fun showProductSheet() {

		val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet , null , false))
		val productSheet = Alerts.appBottomSheet(this , true , productSheetBind)

		val productList = mutableListOf<LiveShowModelOld.Product?>()

		var selectedPos = - 1

		FireRef.LIVE_SESSIONS.child(roomID).addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot : DataSnapshot) {

				val data = LiveShowModelOld().fromMap(snapshot)

				if (data.products != null) {
					productList.clear()
					productList.addAll(
						data.products !!
					)
				}

				log("LIVE ADDED PRODUCTS : $data")

				val productAdapter = FirebaseProductAdapter(productList , object : RecyclerClicks {
					override fun itemClick(pos : Int , status : String?) {

						if (productList[pos]?.status == "sold") {

							Alerts.error(this@LiveShowActivity , "This product is already sold")

						} else {
							productList.forEachIndexed { index , item ->

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

			override fun onCancelled(error : DatabaseError) {
			}

		})

		productSheetBind.close.setHapticClickListener {
			productSheet.dismiss()
		}

		productSheetBind.addBtn.setHapticClickListener {

			if (selectedPos == - 1) {
				Alerts.error(this@LiveShowActivity , "Please select a product")
				return@setHapticClickListener
			}

			val isAnyProductLive = liveData?.products?.any { it?.isCurrent == true } == true
			if (isAnyProductLive) {
				Alerts.error(this@LiveShowActivity , "One Product is Already Live")
				return@setHapticClickListener
			}

			val productRef = FireRef.LIVE_SESSIONS.child(roomID).child("products").child(selectedPos.toString())
			val sessionRef = FireRef.LIVE_SESSIONS.child(roomID)

			val updates = mapOf(
				"highestBid" to null ,
				"bidCountDown" to null
			)

			productRef.updateChildren(mapOf("isCurrent" to true))
				.addOnSuccessListener {
					sessionRef.updateChildren(updates)
					bidCounter = 30
					productSheet.dismiss()
				}
				.addOnFailureListener {
					Alerts.error(this@LiveShowActivity , "Failed to set product live")
				}

		}

	}

	fun initPip() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val visibleRect = Rect()
			bind.root.getGlobalVisibleRect(visibleRect)

			pipParams = PictureInPictureParams.Builder().apply {
				setAspectRatio(Rational(100 , 200))
				setSourceRectHint(visibleRect)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					setAutoEnterEnabled(true)
				}
			}.build()

			setPictureInPictureParams(pipParams)
		}
	}

	override fun onPictureInPictureModeChanged(
		isInPictureInPictureMode : Boolean ,
		newConfig : Configuration ,
	) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode , newConfig)

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
		if (! isInPictureInPictureMode) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				setPictureInPictureParams(pipParams)
				enterPictureInPictureMode(pipParams)
			}
			Alerts.log(javaClass.simpleName , "STARTED IN PIP MODE")
		} else {
			Alerts.log(javaClass.simpleName , "ALREADY IN PIP MODE")
		}
	}

	fun showConfirmationAlert() {

		val showConfirmationSheetBind = ShowConfirmationAlertBinding.bind(layoutInflater.inflate(R.layout.show_confirmation_alert , null , false))
		val showConfirmationSheet = Alerts.appAlert(this , true , showConfirmationSheetBind)

		showConfirmationSheetBind.timing.text = "Show Starts at ${Utils.getFormattedDateTime("HH:mm:ss" , "hh:mm a" , showTime)}"

		showConfirmationSheetBind.startBtn.setHapticClickListener {
			showConfirmationSheet.dismiss()
			bind.loader.isVisible = true
			viewModel.updateLiveStatus(showId.request() , "true".request())
		}

		showConfirmationSheet.show()

	}

}

