package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import im.zego.zim.entity.ZIMTextMessage
import io.bidswipe.app.utils.ChatManager
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
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.model.ZIMExtendedData
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.MoreActivity
import io.bidswipe.app.ui.dashboard.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.StreamingManager
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import kotlin.math.abs

class WatchStreamFragment : BaseFragment<StreamViewModel , FragmentWatchStreamBinding>() {

	override fun getModel() : Class<StreamViewModel> = StreamViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentWatchStreamBinding.inflate(inflater , view , false)

	private lateinit var roomID : String
	private lateinit var streamID : String
	private var highestBidAmount : String? = ""
	private var bidProductId : String? = ""
	private var commentList = mutableListOf<LiveChatModel?>()
	private lateinit var commentAdapter : CommentAdapter
	private var product : LiveShowModelOld.Product? = null
	private var inputSheet : BottomSheetDialog? = null
	private var isAllowBidForAll = true
	private var chatManager : ChatManager? = null

	companion object {
		fun newInstance(roomID : String , streamID : String) = WatchStreamFragment().apply {
			arguments = Bundle().apply {
				putString("roomID" , roomID)
				putString("streamID" , streamID)
			}
		}
	}

	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {

			runSafe {
				val data = LiveShowModelOld().fromMap(snapshot)

				// Safely update highestBidAmount

				if (data.highestBid != null) {

					log("HIGHEST BID: ${data.highestBid}")
					highestBidAmount = data.highestBid?.bidAmount ?: highestBidAmount

					bind.bid.text = "Swipe to Bid ${newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString().asMoney()}"

				}

				// Show viewer count or default to 0
				bind.liveCount.text = (data.viewerCount ?: 0).toString()

				// Find the current product once
				val currentProduct = data.products?.find { it?.id == data.highestBid?.productId }

				log("CURRENT PRODUCT Value : $currentProduct")

				// Determine sale status once
				val isSold = currentProduct?.status == "sold"
				bind.soldLayout.isVisible = isSold
				bind.bidLayout.isVisible = ! isSold

				if (isSold) {
					inputSheet?.dismiss()
				}

				bind.productLayout.isVisible = ! isSold

				if (isSold && data.highestBid?.userId == userId) {
					bind.soldOutText.text = "You won the bid"
				}

				// Show bid countdown if available
				val countdown = snapshot.child("bidCountDown").value?.toString()
				if (! countdown.isNullOrEmpty()) {
					bind.bidTime.isVisible = true
					bind.bidTime.text = "Ends in $countdown"
				} else {
					bind.bidTime.isVisible = false
				}

				isAllowBidForAll = data.allowBidForAll ?: true

			}

		}

		override fun onCancelled(error : DatabaseError) {
			log("Firebase cancelled: ${error.message}")
		}

	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		roomID = requireArguments().getString("roomID") ?: ""
		streamID = requireArguments().getString("streamID") ?: ""
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		log("RoomId: $roomID")

		setUpSwipe()
		
		ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView){ v, insets  ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			
			bind.profileLayout.setMargins(top = system.top, left = resources.dpToPx(16) , right =  resources.dpToPx(16) ,)
			bind.bidLayout.setMargins(resources.dpToPx(16) , 0 , resources.dpToPx(16) , system.bottom)
			
			insets
		}
		
		bind.cutButton.setHapticClickListener {
			finish()
		}

		bind.recycler.setOnTouchListener { view , event ->
			hideKeyboard(view)
			return@setOnTouchListener false
		}

		commentAdapter = CommentAdapter(commentList)

		bind.recycler.adapter = commentAdapter

		FireRef.LIVE_SESSIONS.child(roomID).addValueEventListener(eventListener)

		FireRef.LIVE_SESSIONS.child(roomID).addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot : DataSnapshot , previousChildName : String?) {

			}

			override fun onChildChanged(snapshot : DataSnapshot , previousChildName : String?) {
			}

			override fun onChildRemoved(snapshot : DataSnapshot) {
				if (snapshot.key == "highestBid") {

					FireRef.LIVE_SESSIONS.child(roomID).addListenerForSingleValueEvent(object : ValueEventListener {
						override fun onDataChange(snapshot : DataSnapshot) {

							runSafe {
								val data = LiveShowModelOld().fromMap(snapshot)

								val currentProduct = data.products?.find { it?.isCurrent == true }

								log("CURRENT PRODUCT : $currentProduct")

								if (currentProduct != null) {
									bind.productName.text = currentProduct.name
									bidProductId = currentProduct.id
									bind.productImage.loadUrl(
										mCtx ,
										currentProduct.image ?: "" ,
										placeHolder = draw.product_img
									)
									bind.bidPrice.text = currentProduct.price.toString().asMoney()

									highestBidAmount = currentProduct.price.toString()

									bind.quantity.text = buildString {
										append("Price: ")
										append(currentProduct.price.toString().asMoney())
									}

									bind.bid.text = "Swipe to Bid ${newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString().asMoney()}"

									bind.bid.setCompleted(completed = false , withAnimation = true)

								}
							}

						}

						override fun onCancelled(error : DatabaseError) {

						}
					})

				}
			}

			override fun onChildMoved(snapshot : DataSnapshot , previousChildName : String?) {
			}

			override fun onCancelled(error : DatabaseError) {
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

		bind.gift.setHapticClickListener {
			sendTipSheet()
		}

		bind.wallet.setHapticClickListener {

			showPaymentAndAddressSheet()

		}

		viewModel.selectedStream.observe(viewLifecycleOwner) { stream ->
			if (stream == roomID) {

				/*bind.userImage.loadUrl(
					mCtx ,
					stream.seller?.image.toString() ,
					placeHolder = draw.user_image
				)*/

//				product = stream.products?.find { it?.isCurrent == true }

				bidProductId = product?.id.toString()

				highestBidAmount = product?.price.toString()

//				bind.userName.text = stream.seller?.name.toString()

				bind.productName.text = product?.name

				bind.productImage.loadUrl(
					mCtx ,
					product?.image.toString() ,
					placeHolder = draw.product_img
				)

				bind.bidPrice.text = (product?.price ?: "0").asMoney()

				try {
					bind.quantity.text = buildString {
						append("Price: ")
						append(product?.price.toString().asMoney())
					}
				} catch (e : Exception) {
					e.printStackTrace()
				}

				/*if (stream.seller?.isFollowed == true) {
					bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.outline))
					bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.onSurface))
					bind.follow.text = "Unfollow"
				} else {
					bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.primary))
					bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.background))
					bind.follow.text = "Follow"
				}*/

				bind.follow.setHapticClickListener {
//					viewModel.followUser(stream.seller?.id?.request())
				}

				runSafe {
					bind.bid.text = "Swipe to Bid ${newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString().asMoney()}"
				}

				bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
					override fun onSlideComplete(view : SlideToActView) {

						if (isAllowBidForAll) {
							attemptBid()
						} else {

							if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
								attemptBid()
							} else {
								verificationDialog()
							}
						}
					}
				}

				bind.max.setHapticClickListener {

					showInputSheet()

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
						it.parse(mCtx , TAG , object : AlertClicks {
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

		viewModel.followUserShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					val mData = it.value.data
					if (mData?.status == true) {
						bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.outline))
						bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.onSurface))
						bind.follow.text = "Unfollow"
					} else {
						bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.primary))
						bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.background))
						bind.follow.text = "Follow"
					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
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

	override fun onDestroy() {
		super.onDestroy()
		chatManager?.shutdown()
	}

	private fun loginAndPlay() {
		val manager = StreamingManager.getInstance(requireContext())
		manager.loginRoom(
			roomId = roomID ,
			userId = userId ,
			userName = userName ,
			userImage = userImage
		) { _ , _ ->
			manager.startPlayingStream(roomID , bind.hostView)
		}
		initializeChat()
	}

	private fun stopStream() {
		val manager = StreamingManager.getInstance(requireContext())
		manager.stopPlayingStream(roomID)
		manager.logoutRoom(roomID)
	}

	private fun destroyEngine() {
		val manager = StreamingManager.getInstance(requireContext())
		manager.destroyEngine()
	}

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

		chatManager?.setListener(object : ChatManager.Listener {
			override fun onMessageReceived(message : ZIMTextMessage) {
				runSafe {
					commentList.add(LiveChatModel.fromZIMMessage(message))
					commentAdapter.notifyItemInserted(commentList.size - 1)
					bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
				}
			}

			override fun onRoomStateChanged(state : String) {
				log("ROOM STATE CHANGED: $state")
			}
		})

		chatManager?.initializeAndLogin(roomID) {
			if (App.profileResponse.value?.preferences?.enablePrivateEntry == false) sendZimMessage("Joined \uD83D\uDC4B")
		}

	}

	// ZIM event handler moved into ChatManager

	fun sendZimMessage(content : String) {
		val extended = ZIMExtendedData(userImage , userId , userName).toJson()
		chatManager?.sendTextMessage(roomID , content , extended)
		bind.text.setText("")
	}

	private fun verificationDialog() {
		AppBottomSheet(
			mCtx ,
			R.drawable.ic_info ,
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
			} ,
			"Before you interact with lives shows, You need to become a Verified Buyer." ,
			primaryBtnText = "Okay" ,
			secondaryBtnText = "Cancel" ,
			canCancel = true ,
			showSecondary = false ,
			iconPadding = 16 ,
			alertType = AlertType.INFO ,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog : AppBottomSheet) {
					dialog.dismiss()
					startActivity(Intent(mCtx , TrustedBuyerActivity::class.java).putExtra("slug" , "buyer"))
				}

				override fun secondaryClick(dialog : AppBottomSheet) {
					dialog.dismiss()
				}
			}
		).show()
	}

	fun showInputSheet() {
		val inputSheetBind = InputBottomSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.input_bottom_sheet ,
				null ,
				false
			)
		)
		inputSheet = Alerts.appBottomSheet(mCtx , true , inputSheetBind)

		inputSheetBind.submitBtn.setHapticClickListener {
			val ref = FireRef.LIVE_SESSIONS.child(roomID).child("highestBid")

			ref.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snapshot : DataSnapshot) {

					runSafe {
						val bidAmount = inputSheetBind.price.value().toDouble().toString()

						if (inputSheetBind.price.value().isEmpty() || inputSheetBind.price.value().toDouble() < (highestBidAmount?.toDouble()
								?: 0.0)
						) {
							Alerts.error(mCtx , "Bid amount must be greater than the current highest bid.")
						} else {
							val bidData = mutableMapOf<String , Any?>(
								"bidAmount" to bidAmount ,
								"userName" to userName ,
								"userImage" to userImage ,
								"userId" to userId ,
								"productId" to bidProductId
							)

							// If startTime doesn't exist, it's a new bid; otherwise, update existing
							if (! snapshot.hasChild("startTime")) {
								bidData["startTime"] = Utils.timestamp().toString()
								bidData["productStatus"] = "processed"
							}

							ref.updateChildren(bidData)

							Alerts.success(mCtx , "Bid placed successfully")
							sendZimMessage("New high bid: $$bidAmount")

							inputSheet?.dismiss()
						}
					}

				}

				override fun onCancelled(error : DatabaseError) {
					log("Firebase Error: ${error.message}")
				}

			})
		}

		inputSheetBind.close.setHapticClickListener {
			inputSheet?.dismiss()
		}

		inputSheet?.show()
	}

	@SuppressLint("ClickableViewAccessibility")
	fun setUpSwipe() {

		var downX = 0f

		bind.viewFlipper.setOnTouchListener { _ , event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					downX = event.x
					true
				}

				MotionEvent.ACTION_UP -> {
					val deltaX = event.x - downX

					if (abs(deltaX) > 100) {
						if (deltaX > 0) {
							bind.viewFlipper.setInAnimation(mCtx , R.anim.slide_in_left)
							bind.viewFlipper.setOutAnimation(mCtx , R.anim.slide_out_right)
							bind.viewFlipper.showPrevious()
						} else {
							bind.viewFlipper.setInAnimation(mCtx , R.anim.slide_in_right)
							bind.viewFlipper.setOutAnimation(mCtx , R.anim.slide_out_left)
							bind.viewFlipper.showNext()
						}
					}
					true
				}

				else -> false
			}
		}

		bind.controls.setOnTouchListener { _ , event ->
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					downX = event.x
					true
				}

				MotionEvent.ACTION_UP -> {
					val deltaX = event.x - downX

					if (abs(deltaX) > 100) {
						if (deltaX > 0) {
							bind.viewFlipper.setInAnimation(mCtx , R.anim.slide_in_left)
							bind.viewFlipper.setOutAnimation(mCtx , R.anim.slide_out_right)
							bind.viewFlipper.showPrevious()
						} else {

							bind.viewFlipper.setInAnimation(mCtx , R.anim.slide_in_right)
							bind.viewFlipper.setOutAnimation(mCtx , R.anim.slide_out_left)
							bind.viewFlipper.showNext()
						}
					}
					true
				}

				else -> false
			}
		}

	}

	fun showPaymentAndAddressSheet() {

		val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.payment_and_address_sheet ,
				null ,
				false
			)
		)

		val makeOfferSheet = Alerts.appBottomSheet(mCtx , true , paymentAddressBind)

		with(paymentAddressBind.addressItem) {
			val hasAddress = App.profileResponse.value?.hasShippingAddress == true
			moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx , draw.ic_pencil))
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
					Intent(mCtx , MoreActivity::class.java).putExtra(
						"slug" ,
						"paymentShipping"
					)
				)
			}

		}

		with(paymentAddressBind.paymentCard) {
			val hasCard = App.profileResponse.value?.hasCardAdded == true
			iconCard.isVisible = hasCard
			expiryDate.isVisible = hasCard
			moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx , draw.ic_pencil))
			moreIcon.rotation = 0f

			if (hasCard) {
				cardNumber.text = buildString {
					append("•••• •••• •••• ")
					append(App.profileResponse.value?.defaultCard?.last4)
				}

				expiryDate.text = buildString {
					append(App.profileResponse.value?.defaultCard?.expDate)
				}
			} else {
				cardNumber.text = "Payment Cards Not Added"
			}

			moreIcon.setHapticClickListener {
				startActivity(
					Intent(mCtx , MoreActivity::class.java).putExtra(
						"slug" ,
						"paymentShipping"
					)
				)
			}
		}

		paymentAddressBind.close.setHapticClickListener {
			makeOfferSheet.dismiss()
		}

		makeOfferSheet.show()

	}

	fun attemptBid() {
		runSafe {
			log("SWIPED")

			val ref = FireRef.LIVE_SESSIONS.child(roomID).child("highestBid")

			ref.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snapshot : DataSnapshot) {
					val bidAmount = newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString()

					val bidData = mutableMapOf<String , Any?>(
						"bidAmount" to bidAmount ,
						"userName" to userName ,
						"userImage" to userImage ,
						"userId" to userId ,
						"productId" to bidProductId
					)

					if (! snapshot.hasChild("startTime")) {
						bidData["startTime"] = Utils.timestamp().toString()
						bidData["productStatus"] = "processed"
					}

					ref.updateChildren(bidData)
					sendZimMessage("New high bid: $$bidAmount")
					Alerts.success(mCtx , "Bid placed successfully")
					bind.bid.setCompleted(completed = false , withAnimation = true)
				}

				override fun onCancelled(error : DatabaseError) {
					log("Firebase Error: ${error.message}")
				}
			})
		}
	}

	fun newBidAmount(amount : Int) : Int {
		return when {
			amount in 1 .. 30 -> amount + 1
			amount in 31 .. 50 -> amount + 2
			amount in 51 .. 100 -> amount + 3
			amount in 101 .. 300 -> amount + 5
			amount in 301 .. 1000 -> amount + 10
			amount in 1001 .. 2000 -> amount + 20
			amount >= 2001 -> 50
			else -> 0
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

		sendTipSheetBind.close.setHapticClickListener {
			sendTipSheet.dismiss()
		}

		sendTipSheet.show()
	}

}