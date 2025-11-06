package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import io.agora.rtc2.Constants
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.LiveSellerAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.databinding.ActivityAgoraPublisherBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveSellerSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.ProductSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.ShowConfirmationAlertBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.ui.scheduleShow.TipSettingActivity
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
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

@SuppressLint("NotifyDataSetChanged")
class AgoraPublisherActivity : BaseActivity() {

	private val bind by bind(ActivityAgoraPublisherBinding::inflate)

	private val viewModel by viewModels<DashViewModel>()

	private var liveShowData: LiveShowModel? = null
	private var showId = ""
	private var showTime = ""
	private var roomID = ""
	private var agoraToken = ""
	private var channelName = ""
	private var socketUrl: String = ""
	private lateinit var commentAdapter: CommentAdapter
	private var commentList = mutableListOf<LiveChatModel?>()
	private var productList = mutableListOf<LiveShowModel.Product?>()
	private lateinit var productAdapter: FirebaseProductAdapter
	private var isShowLive = false
	private var updateStatusRunnable: Runnable? = null
	private val updateStatusHandler = Handler(Looper.getMainLooper())
	private lateinit var pipParams: PictureInPictureParams
	private var promotePlans = mutableListOf<GetPromotePlansResponse.Data?>()
	private lateinit var sellerAdapter: LiveSellerAdapter
	private var userList = mutableListOf<GetLiveSellerResponse.Data?>()
	private var zoomLevel = 1.0f
	private var socketManager: SocketManager? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		immersionBar {
			transparentBar()
			supportActionBar(false)
			keyboardEnable(true)
		}

		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.profileLayout.setMargins(
				top = system.top,
				left = resources.dpToPx(16),
				right = resources.dpToPx(16)
			)
			bind.startBtn.setMargins(
				resources.dpToPx(16),
				resources.dpToPx(0),
				resources.dpToPx(16),
				system.bottom
			)
			insets
		}

		runSafe {
			liveShowData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getSerializableExtra("showData", LiveShowModel::class.java) as LiveShowModel
			} else {
				intent.getSerializableExtra("showData") as LiveShowModel
			}
		}

		showId = liveShowData?.showId ?: ""
		showTime = intent.getStringExtra("time") ?: ""

		roomID = "live_room_${userId}_${showId}"

		bind.hostName.text = userName.asCapital()
		bind.hostImage.loadUrl(this, userImage)

		initPip()

		App.manager = AgoraManager(this, Const.APP_ID_AGORA)

		bind.loader.isVisible = true

		viewModel.getAgoraToken(roomID.request())

		commentAdapter = CommentAdapter(commentList)
		bind.recycler.adapter = commentAdapter

		socketUrl = Const.SOCKET_URL
		initializeSocket()

		bind.startBtn.setHapticClickListener {
			showConfirmationAlert()
		}

		bind.cameraSwitch.setHapticClickListener {
			App.manager.switchCamera {
			}
		}

		bind.controls.setHapticClickListener {
			hideKeyboard()
		}

		bind.clip.isVisible = App.profileResponse.value?.preferences?.enableClips == true

		bind.message.setEndIconOnClickListener {

			if (!isShowLive) {
				Alerts.error(this, "Please start live show to send message")
				return@setEndIconOnClickListener
			}

			if (bind.text.value().isNotEmpty()) {
				socketManager?.sendMessage(roomID, bind.text.value(), userId, userName, userImage)
				bind.text.text.clear()
			}
		}

		bind.more.setHapticClickListener {
			showMoreSheet()
		}

		bind.promote.setHapticClickListener {
			if (isShowLive && promotePlans.isNotEmpty()) {
				showPromoteSheet()
			}
		}

		bind.clip.setHapticClickListener {
			createClipSheet()
		}

		bind.share.setHapticClickListener {
			val shareText = buildString {
				append(Const.BASE_URL)
				append("/live-show?roomId=$roomID")
			}

			val shareIntent = Intent().apply {
				action = Intent.ACTION_SEND
				putExtra(Intent.EXTRA_TEXT, shareText)
				type = "text/plain"
			}

			val chooserIntent = Intent.createChooser(shareIntent, "Share via")

			if (shareIntent.resolveActivity(packageManager) != null) {
				startActivity(chooserIntent)
			} else {
				errorToast("No sharing apps available")
			}
		}

		bind.cutButton.setHapticClickListener {

			if (isShowLive) {
				endShowSheet()
			} else {
				App.manager.destroyEngine()
				finishAfterTransition()
			}
		}


		bind.shop.setHapticClickListener {
			if (isShowLive) {
				showProductSheet()
			} else {
				Alerts.error(this, "Please start live show to access this feature")
			}
		}

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (isShowLive) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						enterPictureInPictureMode(pipParams)
					}
				} else {
					finishAfterTransition()
				}
			}
		})

		viewModel.getPromoteShowList()
		viewModel.getPromoteShowListRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.getPromoteShowListRepo.value = null
					promotePlans.clear()
					promotePlans.addAll(it.value.data ?: mutableListOf())
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getPromoteShowListRepo.value = null
				}

				else -> {}

			}
		}

		viewModel.promoteShowRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.promoteShowRepo.value = null

					AppBottomSheet(
						this,
						R.drawable.ic_success,
						"Show Promoted",
						it.value.message ?: "",
						primaryBtnText = "Okay",
						secondaryBtnText = "Cancel",
						canCancel = true,
						showSecondary = false,
						iconPadding = 16,
						alertType = AlertType.SUCCESS,
						clicks = object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						}
					).show()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.promoteShowRepo.value = null
				}

				else -> {}

			}
		}

		viewModel.getLiveSellerRepo.observe(this) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.getLiveSellerRepo.value = null

					val dataList = it.value.data ?: mutableListOf()

					if (dataList.isEmpty()) {
						Toast.makeText(this, "No sellers found currently", Toast.LENGTH_SHORT).show()
					} else {
						userList.clear()
						userList.addAll(dataList)
						showSellerSheet()
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getLiveSellerRepo.value = null

					it.parse(this, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})

				}

				else -> {}
			}
		}

		viewModel.getAgoraTokenRepo.observe(this) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data

					log("TOKEN: ${mData?.token}")
					log("CHANNEL: ${mData?.channel}")

					agoraToken = mData?.token ?: ""
					channelName = mData?.channel ?: ""

					requestPerms(Const.PERMISSIONS) {
						if (it) {
							App.manager.initializeAgoraSDK(Constants.CLIENT_ROLE_BROADCASTER)
							App.manager.setupPublisherView(bind.publisherView)
						} else {
							errorToast("Permissions not granted!")
						}
					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getLiveSellerRepo.value = null

					it.parse(this, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})

				}

				else -> {}
			}
		}

	}

	override fun onDestroy() {
		App.manager.destroyEngine()

		isShowLive = false
		// Socket cleanup
		runSafe {
			socketManager?.emitEndRoom(roomID)
			socketManager?.leaveRoom(roomID, userId)
			socketManager?.disconnect()
		}

		updateStatusRunnable?.let { updateStatusHandler.removeCallbacks(it) }

		super.onDestroy()
	}

	fun showConfirmationAlert() {

		val showConfirmationSheetBind = ShowConfirmationAlertBinding.bind(
			layoutInflater.inflate(
				R.layout.show_confirmation_alert,
				null,
				false
			)
		)

		val showConfirmationSheet = Alerts.appBottomSheet(this, true, showConfirmationSheetBind)

		showConfirmationSheetBind.timing.text =
			buildString {
				append("Show Starts at ")
				append(Utils.getFormattedDateTime("HH:mm:ss", "hh:mm a", showTime))
			}

		showConfirmationSheetBind.startBtn.setHapticClickListener {
			showConfirmationSheet.dismiss()

			App.manager.joinChannel(agoraToken, channelName)


			/*val joinAction = {
				App.manager.joinChannel(userId.toInt(), agoraToken, channelName)
			}

			if (App.manager.isReady()) {
				joinAction.invoke()
			} else {
				App.manager.onReady(joinAction)
			}*/

			bind.startBtn.isVisible = false

			bind.message.setMargins(
				resources.dpToPx(16),
				resources.dpToPx(16),
				resources.dpToPx(16),
				navigationBarHeight
			)

			addShowData(liveShowData!!)

			bind.shop.strokeWidth = 4
			bind.countBadge.isVisible = true
			bind.countBadge.text = (liveShowData?.products?.size ?: 0).toString()

			socketManager?.sendMessage(
				roomID,
				"Joined \uD83D\uDC4B",
				userId,
				userName,
				userImage
			)

			updatePublisherState()

		}

		showConfirmationSheet.show()

	}

	fun addShowData(data: LiveShowModel) {

		isShowLive = true
		socketManager?.createRoom(roomID, data)

		socketManager?.onRoomCreated { obj ->
			runSafe {

				if (obj.optString("room_id") == roomID) {
					val showData = LiveShowModel.fromJson(obj)

					productList.clear()

					productList.addAll(showData.products)

					val liveProduct = showData.products.find { it?.isCurrent == true }

					updateProductUI(liveProduct)

					log("ROOM CREATED : $showData")
				}

			}
//			startLiveDurationTimer()
		}

		socketManager?.onDurationUpdate { obj ->
			runOnUiThread {

				if (roomID == obj.optString("room_id")) {
					val elapsedSeconds = obj.optString("elapsed").toLongOrNull() ?: 0L
					val formattedTime = "%02d:%02d:%02d".format(
						elapsedSeconds / 3600,
						(elapsedSeconds / 60) % 60,
						elapsedSeconds % 60
					)

					bind.duration.text = buildString {
						append("Show Time: ")
						append(formattedTime)
					}
				}

			}

		}

		socketManager?.onAllowBidForAllUpdate { obj ->
			if (roomID == obj.optString("room_id")) {
				val allowBidForAll = obj.optBoolean("allow_bid_for_all")
				liveShowData?.allowBidForAll = allowBidForAll
			}
		}

	}

	private fun initializeSocket() {
		log("SOCKET URL $socketUrl")
		if (socketUrl.isEmpty()) return

		socketManager = SocketManager.getInstance(this)
		socketManager?.initialize(socketUrl, mapOf("uid" to userId))
		socketManager?.connect(onConnected = {
			socketManager?.joinRoom(roomID, userId) {


			}
//			socketManager?.emitViewerJoin(roomID)
		}) { err ->
			log("Socket connect error: $err")
		}

		socketManager?.onViewerCount { args ->
			runSafe {
				if (args.optString("room_id") == roomID) {
					runOnUiThread {
						bind.liveCount.text = args.optString("count")
					}
				}

			}
		}

		socketManager?.onMessage { msg ->

			log("MESSAGE : $msg")
			if (msg.optString("room_id") == roomID) {
				runOnUiThread {
					commentList.add(
						LiveChatModel(
							msg.optString("user_image"),
							msg.optString("user_name"),
							msg.optString("user_id"),
							msg.optString("message")
						)
					)
					commentAdapter.notifyItemInserted(commentList.size - 1)
					bind.recycler.scrollToPosition(commentList.size - 1)
				}
			}
		}

		socketManager?.getBidFinalize { json ->
			runSafe {

				runOnUiThread {

					if (roomID == json.optString("room_id")) {
						val winner = json.getJSONObject("winner")
						val product = productList.find { it?.id == winner.optString("product_id") }

						product?.status = "sold"
						product?.isCurrent = false

//						log("UPDATED PRODUCT LIST : ${productList} ")

						showProductSheet()

					}

				}

			}
		}

		socketManager?.getUpdatedProduct { json ->
			runSafe {
				runOnUiThread {

					if (roomID == json.optString("room_id")) {

						val product = LiveShowModel.fromJson(json)

						productList.clear()

						productList.addAll(product.products)

						/*productList.find { it?.id == product.id }.let {
						val index = productList.indexOf(it)
						if (index != -1) {
							productList[index] = product
						}
					}*/

						val products = LiveShowModel.fromJson(json)
						updateProductUI(products.products.find { it?.isCurrent == true })

						productAdapter.notifyDataSetChanged()

					}

				}
			}

		}

		socketManager?.getBidTimerUpdate { json ->
			updateCountdown(json)
		}

		socketManager?.getHighestBid { json ->
			handleBidUpdate(json)
		}

	}


	// Product selection (simplified socket mirroring)
	private fun showProductSheet() {
		val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet, null, false))
		val productSheet = Alerts.appBottomSheet(this, true, productSheetBind)

		var selectedPos = -1

		productAdapter = FirebaseProductAdapter(productList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				if (productList[pos]?.status == "sold") {

					Alerts.error(this@AgoraPublisherActivity, "This product is already sold")

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

		productSheetBind.close.setHapticClickListener {
			productSheet.dismiss()
		}

		productSheetBind.addBtn.setHapticClickListener {

			if (selectedPos == -1) {
				Alerts.error(this@AgoraPublisherActivity, "Please select a product")
				return@setHapticClickListener
			}

			val isAnyProductLive = productList.any { it?.isCurrent == true }

			if (isAnyProductLive) {
				Alerts.error(this@AgoraPublisherActivity, "One Product is Already Live")
				return@setHapticClickListener
			}

			val selectedProduct = productList[selectedPos]

			socketManager?.setNextProduct(roomID, selectedProduct?.id)
			productSheet.dismiss()

		}
	}

	fun updateProductUI(liveProduct: LiveShowModel.Product?) {

		runOnUiThread {
			log("updateProductUI : $liveProduct")
			bind.product.isVisible = true
			bind.productLayout.isVisible = true
			bind.productName.text = liveProduct?.name?.asCapital()
			bind.productCategory.text = liveProduct?.category?.asCapital()
			bind.quantity.text = buildString {
				append("Quantity: ")
				append(liveProduct?.quantity ?: 0)
			}
			bind.productImage.loadUrl(this, liveProduct?.image ?: "")

			val price = liveProduct?.price
			bind.bidPrice.text = price?.asMoney()
		}


	}

	private fun updateCountdown(json: JSONObject) {
		val value = json.optString("remaining")
		runSafe {
			this.runOnUiThread {
				if (json.optString("room_id") == roomID) {
					log("BID TIMER UPDATE : $value")
					bind.bidTime.isVisible = true
					bind.bidTime.text = buildString {
						append("Ends in ")
						append(value)
					}
				} else {
					bind.bidTime.isVisible = false
				}
			}
		}
	}

	private fun handleBidUpdate(json: JSONObject) {
		runSafe {
			runOnUiThread {
				if (json.optString("room_id") == roomID) {
					val highestBid = json.getJSONObject("get_highest_bid")
					val bidAmount = highestBid.optString("bid_amount")
					log("BID UPDATE: $bidAmount")
					bind.bidPrice.text = bidAmount.asMoney()
				}
			}

		}
	}

	private fun updatePublisherState() {

		updateStatusRunnable = object : Runnable {
			override fun run() {
				socketManager?.updateLiveShowStatus(roomID)
				log("Updating status 4 minutes")
				updateStatusHandler.postDelayed(this, 4 * 60 * 1000)
			}
		}

		updateStatusRunnable?.let { updateStatusHandler.post(it) }

	}

	private fun initPip() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val visibleRect = Rect()
			bind.root.getGlobalVisibleRect(visibleRect)
			pipParams = PictureInPictureParams.Builder().apply {
				setAspectRatio(Rational(100, 200))
//                setAspectRatio(Rational(2, 5))
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
		newConfig: Configuration,
	) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

		if (isInPictureInPictureMode) {
			bind.profileLayout.isVisible = false
			bind.rehearsalLayout.isVisible = false
			bind.recycler.isVisible = false
			bind.menuLayout.isVisible = false
			bind.message.isVisible = false
			bind.product.isVisible = false
			App.PIPMode = true
		} else {
			bind.profileLayout.isVisible = true
			bind.rehearsalLayout.isVisible = true
			bind.recycler.isVisible = true
			bind.menuLayout.isVisible = true
			bind.message.isVisible = true
			bind.product.isVisible = false
			App.PIPMode = false
		}
	}

	override fun onUserLeaveHint() {
		super.onUserLeaveHint()

		log("USER LEAVE HINT")

		if (!isInPictureInPictureMode) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				setPictureInPictureParams(pipParams)
				enterPictureInPictureMode(pipParams)
			}
			log("STARTED IN PIP MODE")
		} else {
			log("ALREADY IN PIP MODE")
		}
	}

	fun showMoreSheet() {
		val moreSheetBind = LiveShowMoreMenuBinding.bind(
			layoutInflater.inflate(
				R.layout.live_show_more_menu,
				null,
				false
			)
		)
		val moreSheet = Alerts.appBottomSheet(this, true, moreSheetBind)

		moreSheetBind.optionList.adapter =
			LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {

					when (pos) {
						0 -> {
							moreSheet.dismiss()
							if (isShowLive) {
								endShowSheet()
								moreSheet.dismiss()
							} else {
								finishAfterTransition()
							}
						}

						1 -> {
							moreSheet.dismiss()
							/*	if (isShowLive) {
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
										enterPictureInPictureMode(pipParams)
									}
								}*/

							startActivity(Intent(this@AgoraPublisherActivity, TipSettingActivity::class.java))
						}

						2 -> {
							moreSheet.dismiss()
							bind.loader.isVisible = true
							viewModel.getLiveSeller()
						}

						else -> {

						}
					}

				}
			})

		moreSheetBind.allowVerifiedUser.setOnCheckedChangeListener { view, isChecked ->

			socketManager?.updateAllowBidForAll(roomID, !isChecked)

		}

		if (!App.manager.isMuted) {
			moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
		} else {
			moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
		}

		log(liveShowData?.allowBidForAll.toString())

		moreSheetBind.allowVerifiedUser.isChecked = liveShowData?.allowBidForAll == false

		moreSheetBind.zoomInLayout.setHapticClickListener {
			if (zoomLevel < 10) {
				zoomLevel += 0.5f
				App.manager.zoomCamera(zoomLevel) {

				}
			}

		}

		moreSheetBind.zoomOut.setHapticClickListener {
			if (zoomLevel > 1) {
				zoomLevel -= 0.5f
				App.manager.zoomCamera(zoomLevel) {
				}
			}
		}

		moreSheetBind.close.setHapticClickListener {
			moreSheet.dismiss()
		}

		moreSheetBind.micLayout.setHapticClickListener {

			//NEED TO IMPLEMENT MUTE UNMUTE LOGIC

			App.manager.muteAudio {
				if (it) {
					moreSheetBind.muteIcon.setImageResource(R.drawable.ic_mute)
				} else {
					moreSheetBind.muteIcon.setImageResource(R.drawable.ic_mic)
				}
			}

//			moreSheet.dismiss()
		}



		moreSheetBind.close.setHapticClickListener {
			moreSheet.dismiss()
		}

		moreSheet.show()
	}

	private fun endShowSheet() {
		val endShowSheetBind =
			EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet, null, false))
		val sheet = Alerts.appBottomSheet(this, true, endShowSheetBind)
		endShowSheetBind.close.setHapticClickListener { sheet.dismiss() }
		endShowSheetBind.endBtn.setHapticClickListener {
			sheet.dismiss()
			socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
			App.manager.destroyEngine()
			finishAfterTransition()
		}
		sheet.show()
	}

	private fun showSellerSheet() {
		val liveSellerSheetBind = LiveSellerSheetBinding.bind(
			layoutInflater.inflate(R.layout.live_seller_sheet, null, false)
		)
		val liveSellerSheet = Alerts.appBottomSheet(this, true, liveSellerSheetBind)

		var selectedItem: GetLiveSellerResponse.Data? = null

		sellerAdapter = LiveSellerAdapter(userList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				selectedItem = userList[pos]
			}
		})

		liveSellerSheetBind.recycler.adapter = sellerAdapter

		liveSellerSheetBind.close.setHapticClickListener {
			liveSellerSheet.dismiss()
		}

		liveSellerSheetBind.addBtn.setHapticClickListener {
			if (selectedItem != null) {
				log("Selected seller: ${selectedItem?.name}")
				socketManager?.createRaid(roomID, selectedItem?.roomId.toString(), selectedItem?.id.toString(), userId)
				liveSellerSheet.dismiss()
				App.manager.destroyEngine()
				finishAfterTransition()
			} else {
				Alerts.error(this@AgoraPublisherActivity, "Please select a seller")
			}
		}

		liveSellerSheet.show()
	}

	fun showPromoteSheet() {
		val promoteSheetBind = PromoteShowSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.promote_show_sheet,
				null,
				false
			)
		)

		val promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)

		promoteSheetBind.optionList.adapter = PromoteSheetAdapter(promotePlans, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				promoteSheet.dismiss()
				bind.loader.isVisible = true
				viewModel.promoteShow(
					showId.request(),
					promotePlans[pos]?.id.toString().request()
				)
			}
		})

		promoteSheetBind.close.setHapticClickListener {
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

		clipSheetBind.close.setHapticClickListener {
			clipSheet.dismiss()
		}

		clipSheet.show()
	}

}