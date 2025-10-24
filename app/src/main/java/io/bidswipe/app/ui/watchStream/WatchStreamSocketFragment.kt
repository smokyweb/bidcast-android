package io.bidswipe.app.ui.watchStream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.millicast.Core
import com.millicast.Media
import com.millicast.Subscriber
import com.millicast.clients.ConnectionOptions
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.SubscriberConnectionState
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.databinding.InputBottomSheetBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.databinding.SendTipSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.math.abs

@SuppressLint("NotifyDataSetChanged")
class WatchStreamSocketFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {

	override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentWatchStreamBinding.inflate(inflater, view, false)

	private lateinit var roomID: String
	private lateinit var socketUrl: String

	private var socketManager: SocketManager? = null
	private var highestBidAmount: String? = ""
	private var bidProductId: String? = null
	private var sellerId: String? = ""
	private var inputSheet: BottomSheetDialog? = null
	private var isAllowBidForAll = true
	private var commentList = mutableListOf<LiveChatModel?>()
	private lateinit var commentAdapter: CommentAdapter
	private lateinit var subscriber: Subscriber
	private lateinit var eglBase: EglBase
	private var subscriberStateJob: Job? = null
	val sourceVideoTracks: ArrayList<RemoteVideoTrack> = arrayListOf()
	var audioTrack: RemoteAudioTrack? = null

	companion object {
		fun newInstance(roomID: String, streamID: String) = WatchStreamSocketFragment().apply {
			arguments = Bundle().apply {
				putString("roomID", roomID)
				putString("streamID", streamID)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		roomID = requireArguments().getString("roomID") ?: ""
		Log.d("TAG", "onCreate: ROOM ID: $roomID ")
		socketUrl = Const.SOCKET_URL// requireArguments().getString("socketUrl") ?: ""
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setUpSwipe()

		ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.profileLayout.setMargins(
				top = system.top,
				left = resources.dpToPx(16),
				right = resources.dpToPx(16),
			)
			bind.bidLayout.setMargins(resources.dpToPx(16), 0, resources.dpToPx(16), system.bottom)
			insets
		}

		bind.cutButton.setHapticClickListener {
			finish()
		}

		bind.recycler.setOnTouchListener { view, event ->
			hideKeyboard(view)
			return@setOnTouchListener false
		}

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		subscriber = Core.createSubscriber()

		initRenderer()

		startSubscription()

		// Initialize sockets
		if (socketUrl.isNotEmpty()) {
			socketManager = SocketManager.getInstance(requireContext())
			socketManager?.initialize(socketUrl, mapOf("uid" to userId))
			socketManager?.connect(onConnected = {
				socketManager?.joinRoom(roomID, userId) {
				}
			}) { err -> log("Socket connect error: $err") }

			socketManager?.onViewerCount { args ->

				runSafe {
					if (args.optString("room_id") == roomID) {
						requireActivity().runOnUiThread {
							bind.liveCount.text = args.optString("count")
						}
					}

				}
			}

			socketManager?.getHighestBid { json ->
				handleBidUpdate(json)
			}

			socketManager?.getUpdatedProduct { json ->
				runSafe {
					requireActivity().runOnUiThread {

						if (json.optString("room_id") == roomID) {
							val products = LiveShowModel.fromJson(json)

							updateProductUI(products.products.find { it?.isCurrent == true })

							bind.bid.text = "Swipe to Bid ${
								newBidAmount(
									products.products.find { it?.isCurrent == true }?.price?.toDoubleOrNull()?.toInt() ?: 0
								).toString().asMoney()
							}"
						}

					}
				}

			}

			socketManager?.getBidFinalize { json ->
				runSafe {

					requireActivity().runOnUiThread {

						bind.bidTime.isVisible = false

						val winner = json.getJSONObject("winner")

						log("WINNER: $winner")

						if (roomID == json.optString("room_id")) {
							bind.soldLayout.isVisible = true
							bind.bidLayout.isVisible = false
							bind.productLayout.isVisible = false
						}

						if (userId == winner.optString("user_id")) {
							bind.soldOutText.text = "You won the bid"
						} else {
							bind.soldOutText.text = "Bidder ${winner.optString("user_name")} won the bid"
						}

					}

				}
			}

			// Optional room/session updates (current product, sold, allow flags, countdown)
			socketManager?.onMessage { msg ->
				log("${roomID}  MESSAGES $msg")

				viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
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

			socketManager?.onRoomEnded { json ->
				runSafe {
					if (json.optString("room_end") == roomID) {
						finish()
					}
				}
			}
		}

		socketManager?.onRoomCreated { obj ->
			requireActivity().runOnUiThread {
				if (obj.optString("room_id") == roomID){
					updateSessionUI(obj)
				}
			}
		}

		socketManager?.receiveRaid {obj ->
			requireActivity().runOnUiThread {
				if (obj.optString("source_room_id") == roomID){
					val targetRoomId = obj.optString("target_room_id")
					onRaid(targetRoomId)
				}
			}
		}

		bind.message.setEndIconOnClickListener {
			if (bind.text.value().isNotEmpty()) {
				if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
					socketManager?.sendMessage(
						roomID,
						bind.text.value(),
						userId,
						userName,
						userImage
					)
					bind.text.text.clear()
				} else {
					verificationDialog()
				}
			}
		}

		bind.wallet.setHapticClickListener {
			showPaymentAndAddressSheet()
		}

		bind.gift.setHapticClickListener {
			sendTipSheet()
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

			if (shareIntent.resolveActivity(requireActivity().packageManager) != null) {
				startActivity(chooserIntent)
			} else {
				errorToast("No sharing apps available")
			}
		}

		if (App.profileResponse.value?.buyerIdentityStatus != "verified") {
			verificationDialog()
		}

		viewModel.sendTipAmountRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					it.value.data

					Alerts.success(mCtx, "Tip sent successfully")


				}

				is Resource.Error -> {
					it.parse(mCtx, TAG, object : AlertClicks {
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

		viewModel.followUserShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data

					bind.follow.isVisible = false

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(mCtx, TAG, object : AlertClicks {
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

	override fun onResume() {
		super.onResume()
		socketManager?.joinRoom(roomID, userId) {
			socketManager?.sendMessage(roomID, "Joined \uD83D\uDC4B", userId, userName, userImage)
		}
	}

	override fun onPause() {
		super.onPause()
		socketManager?.leaveRoom(roomID, userId)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		socketManager?.leaveRoom(roomID, userId)
		socketManager?.disconnect()
	}

	private fun handleBidUpdate(json: JSONObject) {
		runSafe {
			requireActivity().runOnUiThread {
				if (json.optString("room_id") == roomID){
					val highestBid = json.getJSONObject("get_highest_bid")
					val bidAmount = highestBid.optString("bid_amount")
					log("BID UPDATE: $bidAmount")
					bind.bidPrice.text = bidAmount.asMoney()
					bind.bid.text = "Swipe to Bid ${newBidAmount(bidAmount.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney()}"
					highestBidAmount = bidAmount
					bidProductId = highestBid.optString("product_id")
				}
			}

		}
	}

	private fun updateSessionUI(json: JSONObject) {
		runSafe {
			val showData = LiveShowModel.fromJson(json)

			log("SESSION UPDATE: $showData")

			val liveProduct = showData.products.find { it?.isCurrent == true }

			if (showData.highestBid.bidAmount?.isNotEmpty() == true) {
				highestBidAmount = showData.highestBid.bidAmount
				log("HIGHEST BID: ${highestBidAmount}")
				bind.bid.text = "Swipe to Bid ${newBidAmount(highestBidAmount?.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney()}"
			} else {
				highestBidAmount = ""
				bind.bid.text = "Swipe to Bid ${newBidAmount(liveProduct?.price?.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney()}"
			}

			updateProductUI(liveProduct)

			isAllowBidForAll = json.optBoolean("allowBidForAll", true)

			// Safely update highestBidAmount

			/*if (showData.highestBid != null) {

				log("HIGHEST BID: ${showData.highestBid}")
				highestBidAmount = showData.highestBid?.bidAmount ?: highestBidAmount

				bind.bid.text = "Swipe to Bid ${
					newBidAmount(
						highestBidAmount?.toDouble()?.toInt() ?: 0
					).toString().asMoney()
				}"

			}*/

			sellerId = showData.seller?.id.toString()

			bind.userName.text = showData.seller?.name?.asCapital()
			bind.userImage.loadUrl(mCtx, showData.seller?.image ?: "")

			bind.liveCount.text = showData.viewerCount

			bind.follow.setHapticClickListener {
				bind.loader.isVisible = true
				viewModel.followUser(sellerId?.request())
			}
			// Determine sale status once

			/*val isSold = liveProduct?.status == "sold"
			bind.soldLayout.isVisible = isSold
			bind.bidLayout.isVisible = !isSold

			if (isSold) {
				inputSheet?.dismiss()
			}

			bind.productLayout.isVisible = !isSold

			if (isSold && showData.highestBid.userId == userId) {
				bind.soldOutText.text = "You won the bid"
			}

			// Show bid countdown if available
			 val countdown = showData.bidCountDown.toString()
			 if (!countdown.isNotEmpty()) {
				 bind.bidTime.isVisible = true
				 bind.bidTime.text = "Ends in $countdown"
			 } else {
				 bind.bidTime.isVisible = false
			 }*/

//			isAllowBidForAll = showData.allowBidForAll ?: true

			log("ALLOW BID FOR ALL: $isAllowBidForAll")

			bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
				override fun onSlideComplete(view: SlideToActView) {

					if (App.profileResponse.value?.hasShippingAddress == true && App.profileResponse.value?.hasCardAdded == true){

						if (isAllowBidForAll) {
							attemptBid()
						} else {
							if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
								attemptBid()
							} else {
								verificationDialog()
							}
						}

					}else{
						showPaymentAndAddressSheet()
					}

				}
			}

			bind.max.setHapticClickListener {
				showInputSheet()
			}

			socketManager?.getBidTimerUpdate { json ->
				updateCountdown(json)
			}

			socketManager?.onAllowBidForAllUpdate { obj ->
				if (roomID == obj.optString("room_id")) {
					val allowBidForAll = obj.optBoolean("allow_bid_for_all")
					isAllowBidForAll = allowBidForAll
				}
			}

		}
	}

	private fun updateCountdown(json: JSONObject) {
		val value = json.optString("remaining")
		runSafe {
			log("BID COUNTDOWN: $value")
			requireActivity().runOnUiThread {
				if (json.optString("room_id") == roomID) {
					bind.bidTime.isVisible = true
					bind.bidTime.text = "Ends in $value"
				}
			}
		}
	}

	private fun showInputSheet() {

		val inputSheetBind = InputBottomSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.input_bottom_sheet,
				null,
				false
			)
		)

		inputSheet = Alerts.appBottomSheet(mCtx, true, inputSheetBind)

		inputSheetBind.submitBtn.setHapticClickListener {
			val priceText = inputSheetBind.price.value()
			val priceVal = priceText.toDoubleOrNull() ?: 0.0
			val current = highestBidAmount?.toDoubleOrNull() ?: 0.0
			if (priceVal <= current) {
				Alerts.error(mCtx, "Bid amount must be greater than the current highest bid.")
			} else {
				socketManager?.emitBid(
					roomId = roomID,
					userId = userId,
					userName = userName,
					userImage = userImage,
					productId = bidProductId,
					bidAmount = priceText
				)
				Alerts.success(mCtx, "Bid placed successfully")

			/*	socketManager?.sendMessage(
					roomID,
					"New high bid: $$priceText",
					userId,
					userName,
					userImage
				)*/
//                sendZimMessage("New high bid: $${priceVal}")
				inputSheet?.dismiss()
			}
		}

		inputSheetBind.close.setHapticClickListener { inputSheet?.dismiss() }
		inputSheet?.show()
	}

	/*
		private fun initializeChat() {
			if (chatManager == null) {
				chatManager = ChatManager(
					application = requireActivity().application ,
					appId = Const.APP_ID.toLong() ,
					appSign = Const.APP_SIGN ,
					userId = userId ,
					userName = userName ,
					userImage = userImage
				)
			}
			chatManager?.initializeAndLogin(roomID) {
				val extended = ZIMExtendedData(userImage , userId , userName).toJson()
				chatManager?.sendTextMessage(roomID , "Joined 👋" , extended)
			}
		}
	*/

	fun newBidAmount(amount: Int): Int {

		log("NEW BID AMOUNT: $amount")

		return when {
			amount in 1..30 -> amount + 1
			amount in 31..50 -> amount + 2
			amount in 51..100 -> amount + 3
			amount in 101..300 -> amount + 5
			amount in 301..1000 -> amount + 10
			amount in 1001..2000 -> amount + 20
			amount >= 2001 -> amount + 50
			else -> 0
		}
	}

	fun attemptBid() {
		runSafe {
			log("SWIPED")

			val bidAmount = newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString()

			socketManager?.emitBid(
				roomId = roomID,
				userId = userId,
				userName = userName,
				userImage = userImage,
				productId = bidProductId,
				bidAmount = bidAmount
			)

//            sendZimMessage("New high bid: $$bidAmount")
			Alerts.success(mCtx, "Bid placed successfully")
		/*	socketManager?.sendMessage(
				roomID,
				"New high bid: $$bidAmount",
				userId,
				userName,
				userImage
			)*/
			bind.bid.setCompleted(completed = false, withAnimation = true)
		}
	}

	fun updateProductUI(liveProduct: LiveShowModel.Product?) {

		activity?.runOnUiThread {
			bind.soldLayout.isVisible = false
			bind.bidLayout.isVisible = true
			bind.productLayout.isVisible = true
			bind.productName.text = liveProduct?.name?.asCapital()
			bind.productCategory.text = liveProduct?.category?.asCapital()
			bind.quantity.text = buildString {
				append("Quantity: ")
				append(liveProduct?.quantity ?:0)
			}
			bind.productImage.loadUrl(mCtx, liveProduct?.image ?: "")
			val price = liveProduct?.price
			bind.bidPrice.text = price?.asMoney()
			highestBidAmount = price
			bidProductId = liveProduct?.id
		}

	}

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

	fun showPaymentAndAddressSheet() {

		val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.payment_and_address_sheet,
				null,
				false
			)
		)

		val makeOfferSheet = Alerts.appBottomSheet(mCtx, true, paymentAddressBind)

		with(paymentAddressBind.addressItem) {
			val hasAddress = App.profileResponse.value?.hasShippingAddress == true
			moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
			moreIcon.rotation = 0f

			name.isVisible = hasAddress
			address.isVisible = hasAddress

			if (hasAddress) {
				val addressData = App.profileResponse.value?.defaultShippingAddress
				address.text = addressData?.streetAddress
				name.text = addressData?.name
				type.text = addressData?.type
				defaultAddress.isVisible = addressData?.isDefault == true
			} else {
				type.text = "Address Not Added"
				defaultAddress.isVisible = false
			}

			moreIcon.setHapticClickListener {
				startActivity(
					Intent(mCtx, MoreActivity::class.java).putExtra(
						"slug",
						"paymentShipping"
					)
				)
			}

		}

		with(paymentAddressBind.paymentCard) {
			val hasCard = App.profileResponse.value?.hasCardAdded == true
			iconCard.isVisible = hasCard
			expiryDate.isVisible = hasCard
			moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
			moreIcon.rotation = 0f

			if (hasCard) {
				cardNumber.text = buildString {
					append("•••• •••• •••• ")
					append(App.profileResponse.value?.defaultCard?.last4 ?:"")
				}

				expiryDate.text = buildString {
					append(App.profileResponse.value?.defaultCard?.expDate?:"")
				}
			} else {
				cardNumber.text = "Payment Cards Not Added"
			}

			moreIcon.setHapticClickListener {
				startActivity(
					Intent(mCtx, MoreActivity::class.java).putExtra(
						"slug",
						"paymentShipping"
					)
				)
			}
		}

		paymentAddressBind.close.setHapticClickListener {
			makeOfferSheet.dismiss()
		}

		bind.bid.setCompleted(completed = false, withAnimation = true)

		makeOfferSheet.show()

	}

	private fun initRenderer() {
		// Prefer SDK-provided EGL context for subscriber per docs
		eglBase = EglBase.create()
		bind.hostView.init(Media.eglBaseContext, null)
		bind.hostView.setMirror(false)
		bind.hostView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
		log("Renderer initialized")
	}

	private fun startSubscription() {
		viewModel.viewModelScope.launch {
			try {
				log("Starting subscription flow...")
				val credentials = Credential(
					streamName = roomID,
					accountId = Const.ACCOUNT_ID,
					apiUrl = "https://director.millicast.com/api/director/subscribe"
				)

				subscriber.setCredentials(credentials)
				log("Credentials set. Connecting (autoReconnect=true)...")
				subscriber.connect(ConnectionOptions(autoReconnect = false))

				subscriberStateJob?.cancel()
				subscriberStateJob = viewModel.viewModelScope.launch {
					// Connection state
					subscriber.state
						.map { it.connectionState }
						.distinctUntilChanged()
						.collect { state ->
							log("Subscriber state: $state")
							when (state) {
								SubscriberConnectionState.Connected -> {
									log("Connected. Subscribing now...")
									subscriber.subscribe(Option())
									log("Subscribe invoked; awaiting remote tracks...")
								}

								SubscriberConnectionState.Subscribed -> {
									log("Subscriber state: Subscribed (media should start)")
								}

								else -> {}
							}
						}
				}

				// Log websocket and peer connection states
				viewModel.viewModelScope.launch {
					subscriber.state.map { it.websocketConnectionState }.distinctUntilChanged()
						.collect { ws ->
							log("WebSocket state: $ws")
						}
				}
				viewModel.viewModelScope.launch {
					subscriber.state.map { it.peerConnectionState }.distinctUntilChanged()
						.collect { pc ->
							log("PeerConnection state: $pc")
						}
				}
				// Log signaling errors if any
				viewModel.viewModelScope.launch {
					subscriber.signalingError.collect { err ->
						log("Signaling error: $err")
					}
				}

				// Collect remote video/audio tracks
				viewModel.viewModelScope.launch {
					subscriber.onRemoteTrack.collect { holder ->
						when (holder) {
							is RemoteVideoTrack -> {
								log("RemoteVideoTrack: sourceId=${holder.sourceId}")
								sourceVideoTracks.add(holder)
								holder.enableAsync(videoSink = bind.hostView)
								viewModel.viewModelScope.launch {
									holder.onState.collect { trackState ->
										log("VideoTrack state mid=${trackState.mid} active=${trackState.isActive}")
										if (!trackState.isActive) holder.disableAsync() else holder.enableAsync(
											videoSink = bind.hostView
										)
									}
								}
								// Add a tiny post-frame confirmation
								bind.hostView.postDelayed({
									log("Renderer (TextureViewRenderer) ready; awaiting frames...")
								}, 300)
							}

							is RemoteAudioTrack -> {
								log("RemoteAudioTrack: sourceId=${holder.sourceId}")
								audioTrack = holder
								holder.enableAsync()
							}
						}
					}
				}
			} catch (e: Throwable) {
				log("SUBSCRIBE ERROR: ${e.localizedMessage}")
				e.printStackTrace()
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		log("Subscriber cleanup starting")

		// Cancel job first - safest operation
		runSafe {
			subscriberStateJob?.cancel()
			subscriberStateJob = null
		}

		// Cleanup video tracks safely
		runSafe {
			sourceVideoTracks.forEach { track ->
				try {
					track.disableAsync()
				} catch (e: Exception) {
					log("Error disabling video track: ${e.message}")
				}
			}
			sourceVideoTracks.clear()
		}

		// Cleanup audio track safely
		runSafe {
			audioTrack?.let { track ->
				try {
					track.disableAsync()
				} catch (e: Exception) {
					log("Error disabling audio track: ${e.message}")
				}
			}
			audioTrack = null
		}

		// Unsubscribe and disconnect safely
		runSafe {
			if (::subscriber.isInitialized) {
				viewModel.viewModelScope.launch {
					try {
						subscriber.unsubscribe()
					} catch (e: Exception) {
						log("Error unsubscribing: ${e.message}")
					}
					try {
						subscriber.disconnect()
					} catch (e: Exception) {
						log("Error disconnecting subscriber: ${e.message}")
					}
				}
			}
		}

		// Cleanup video view safely
		runSafe {
			runSafe {
				bind.hostView.release()
			}
		}

		// Cleanup EGL base safely
		runSafe {
			if (::eglBase.isInitialized) {
				try {
					eglBase.release()
				} catch (e: Exception) {
					log("Error releasing EGL base: ${e.message}")
				}
			}
		}

		// Force garbage collection
		System.gc()

		log("Subscriber cleanup finished")
	}

	fun sendTipSheet() {
		val sendTipSheetBind = SendTipSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.send_tip_sheet,
				null,
				false
			)
		)

		val sendTipSheet = Alerts.appBottomSheet(mCtx, true, sendTipSheetBind)

		sendTipSheetBind.root.setOnClickListener {
			hideKeyboard(it)
		}

		sendTipSheetBind.btnTip5.setHapticClickListener {
			sendTipSheetBind.customOffer.setText("5")
		}
		sendTipSheetBind.close.setHapticClickListener {
			sendTipSheet.dismiss()
		}

		sendTipSheetBind.btnTip10.setHapticClickListener {
			sendTipSheetBind.customOffer.setText("10")
		}

		sendTipSheetBind.btnTip25.setHapticClickListener {
			sendTipSheetBind.customOffer.setText("25")
		}

		sendTipSheetBind.btnTip50.setHapticClickListener {
			sendTipSheetBind.customOffer.setText("50")
		}

		sendTipSheetBind.paymentWallet.text = buildString {
			append("Wallet - ")
			append(App.profileResponse.value?.walletAmount ?: 0)
		}

		sendTipSheetBind.walletRadio.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				sendTipSheetBind.cardRadio.isChecked = false
			}
		}

		sendTipSheetBind.cardRadio.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				sendTipSheetBind.walletRadio.isChecked = false
			}
		}

		if (App.profileResponse.value?.defaultCard != null) {
			sendTipSheetBind.paymentCard.text = buildString {
				append("XXXX XXXX XXXX ")
				append(App.profileResponse.value?.defaultCard?.last4 ?: 0)
			}
		} else {
			sendTipSheetBind.cardRadio.isVisible = false
			sendTipSheetBind.paymentCard.text = buildString {
				append("Payment Method Not Added")
			}
		}

		sendTipSheetBind.btnSendTip.setHapticClickListener {

			with(sendTipSheetBind) {

				if (!walletRadio.isChecked && !cardRadio.isChecked) {
					Alerts.error(mCtx, "Please select a payment method")
					return@setHapticClickListener
				}

				if (customOffer.text.toString().isEmpty()) {
					Alerts.error(mCtx, "Please enter an amount")
					return@setHapticClickListener
				}

				if (walletRadio.isChecked && customOffer.text.toString().toDouble() > ((App.profileResponse.value?.walletAmount ?: "0.0").toString()
						.toDouble())
				) {
					Alerts.error(mCtx, "Insufficient balance")
					return@setHapticClickListener
				}
			}

			sendTipSheet.dismiss()
			bind.loader.isVisible = true
			viewModel.sendTipAmount(
				sellerId!!.request(),
				sendTipSheetBind.customOffer.text.toString().request(),
				null
			)

		}

		sendTipSheet.show()
	}

	private fun onRaid(targetRoomId: String){
		viewModel.viewModelScope.launch {
			try {
				socketManager?.leaveRoom(roomID, userId)
				commentList.clear()
				commentAdapter.notifyDataSetChanged()

				roomID = targetRoomId

				subscriber = Core.createSubscriber()

				startSubscription()

				// 8. Join new socket room
				socketManager?.joinRoom(roomID, userId) {
					socketManager?.sendMessage(roomID, "Joined \uD83D\uDC4B", userId, userName, userImage)
				}
			} catch (e: Exception) {
				log("Raid failed: ${e.message}")
				e.printStackTrace()
			}
		}

	}


}


