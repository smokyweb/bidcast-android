package io.bidswipe.app.ui.watchStream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import io.agora.rtc2.video.VideoCanvas
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.databinding.InputBottomSheetBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.databinding.ProductSheetBinding
import io.bidswipe.app.databinding.SendTipSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.ChatManager
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {

	override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentWatchStreamBinding.inflate(inflater, view, false)

	private lateinit var roomID: String
	private lateinit var streamID: String
	private var highestBidAmount: String? = ""
	private var bidProductId: String? = ""
	private lateinit var socketUrl: String
	private var commentList = mutableListOf<LiveChatModel?>()
	private lateinit var commentAdapter: CommentAdapter
	private var product: LiveShowModelOld.Product? = null
	private var inputSheet: BottomSheetDialog? = null
	private var sellerId: String? = ""
	private var isAllowBidForAll = true
	private var chatManager: ChatManager? = null
	private var socketManager: SocketManager? = null
	private lateinit var productAdapter : FirebaseProductAdapter
	private var productList = mutableListOf<LiveShowModel.Product?>()
	private var currentRemoteUid: Int? = null
	
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
		socketUrl = Const.SOCKET_URL
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		log("RoomId: $roomID")
		log("StreamToken: $streamID")

		setUpSwipe()

		ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())

			bind.profileLayout.setMargins(top = system.top, left = resources.dpToPx(16), right = resources.dpToPx(16))
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

        App.manager.onUserJoin = { uId, _ ->
            currentRemoteUid = uId
			activity?.runOnUiThread {
				setupRemoteVideo(uId)
			}
		}

        App.manager.onUserLeave = { remoteUid, _ ->
            if (currentRemoteUid == remoteUid) {
                currentRemoteUid = null
            }
            activity?.runOnUiThread {
                clearRemoteVideo()
            }
		}

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

				activity?.runOnUiThread {
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
			activity?.runOnUiThread {
				if (obj.optString("room_id") == roomID){
					updateSessionUI(obj)
				}
			}
		}

		socketManager?.onFollowSellerStatus { obj ->
			activity?.runOnUiThread {
				if (obj.optString("room_id") == roomID && obj.optString("user_id") == userId){

					log("IS FOLLOWING : ${obj.optString("is_followed")}")

					bind.follow.isVisible = !obj.optBoolean("is_followed")
				}
			}
		}

		socketManager?.receiveRaid {obj ->
			requireActivity().runOnUiThread {
				if (obj.optString("source_room_id") == roomID){
					val targetRoomId = obj.optString("target_room_id")
					val rtcToken = obj.optString("rtcToken")
					onRaid(targetRoomId, rtcToken)
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

		bind.shop.setHapticClickListener {
			showProductSheet()
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

		if (streamID.isBlank()) {
			log("Stream token missing – unable to join channel")
			return
		}

        if (App.manager.isReady()) {
            App.manager.joinSubscriberChannel(userId.toInt(), streamID, roomID)
            currentRemoteUid?.let { uid ->
                setupRemoteVideo(uid)
            }
        } else {
            App.manager.onReady {
                App.manager.joinSubscriberChannel(userId.toInt(), streamID, roomID)
                currentRemoteUid?.let { uid ->
                    setupRemoteVideo(uid)
                }
            }
        }
//		loginAndPlay()
	}

	override fun onPause() {
		super.onPause()
		socketManager?.leaveRoom(roomID, userId)
		App.manager.leaveChannel()
//		stopStream()
	}

	override fun onDestroy() {
		super.onDestroy()
		socketManager?.leaveRoom(roomID, userId)
		socketManager?.disconnect()
		App.manager.destroyEngine()
	}

	private fun setupRemoteVideo(uid: Int) {
		bind.hostView.removeAllViews()
		val surfaceView = SurfaceView(mCtx)
		val videoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
		bind.hostView.addView(surfaceView)
		log("CHILD : ${bind.hostView.childCount}")
		App.manager.mRtcEngine?.setupRemoteVideo(videoCanvas)
        bind.soldLayout.isVisible = false
        bind.productLayout.isVisible = true
    }

    private fun clearRemoteVideo() {
        bind.hostView.removeAllViews()
        bind.productLayout.isVisible = false
        bind.soldLayout.isVisible = true
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

	private fun updateSessionUI(json: JSONObject) {
		runSafe {
			val showData = LiveShowModel.fromJson(json)

			log("SESSION UPDATE: $showData")

			productList.clear()
			productList.addAll(showData.products)
			bind.countBadge.isVisible = true
			bind.countBadge.text = productList.size.toString()

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


			sellerId = showData.seller?.id.toString()

			bind.userName.text = showData.seller?.name?.asCapital()
			bind.userImage.loadUrl(mCtx, showData.seller?.image ?: "")

			bind.liveCount.text = showData.viewerCount

			bind.follow.setHapticClickListener {
				socketManager?.followSeller( userId,sellerId ?:"")
			}

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

	private fun showProductSheet() {
		val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet , null , false))
		val productSheet = Alerts.appBottomSheet(mCtx , true , productSheetBind)

		productAdapter = FirebaseProductAdapter(productList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {

			}

		})

		productSheetBind.title.text = buildString {
			append("Seller Products")
		}

		productSheetBind.recycler.adapter = productAdapter

		productSheet.show()

		productSheetBind.close.setHapticClickListener {
			productSheet.dismiss()
		}
		productSheetBind.addBtn.isVisible = false
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

	private fun onRaid(targetRoomId: String, rtcToken: String){
		viewModel.viewModelScope.launch {
			try {
				socketManager?.leaveRoom(roomID, userId)
				commentList.clear()
				commentAdapter.notifyDataSetChanged()
                currentRemoteUid = null
                clearRemoteVideo()
				roomID = targetRoomId
				streamID = rtcToken

				App.manager.joinSubscriberChannel(userId.toInt(), streamID, roomID)

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