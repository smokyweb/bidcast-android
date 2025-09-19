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
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.databinding.InputBottomSheetBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.MoreActivity
import io.bidswipe.app.ui.dashboard.more.TrustedBuyerActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.value
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.math.abs

class WatchStreamSocketFragment : BaseFragment<StreamViewModel , FragmentWatchStreamBinding>() {

    override fun getModel() : Class<StreamViewModel> = StreamViewModel::class.java

    override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentWatchStreamBinding.inflate(inflater , view , false)

    private lateinit var roomID : String
    private lateinit var socketUrl : String
//    private var chatManager : ChatManager? = null
    private var socketManager : SocketManager? = null
    private var highestBidAmount : String? = "0"
    private var bidProductId : String? = null
    private var inputSheet : BottomSheetDialog? = null
    private var isAllowBidForAll = true
    private var commentList = mutableListOf<LiveChatModel?>()
    private lateinit var commentAdapter : CommentAdapter
    private var product : LiveShowModelOld.Product? = null
    
    private lateinit var subscriber: Subscriber
    private lateinit var eglBase: EglBase
    private var subscriberStateJob: Job? = null
    val sourceVideoTracks: ArrayList<RemoteVideoTrack> = arrayListOf()
    var audioTrack: RemoteAudioTrack? = null
    private var streamName: String = ""

    companion object {
        fun newInstance(roomID : String , streamID : String) = WatchStreamSocketFragment().apply {
            arguments = Bundle().apply {
                putString("roomID" , roomID)
                putString("streamID" , streamID)
            }
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        roomID = requireArguments().getString("roomID") ?: ""
        socketUrl = Const.SOCKET_URL// requireArguments().getString("socketUrl") ?: ""
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)

        setUpSwipe()
        
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView){ v, insets  ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            bind.controlsView.setMargins(top = system.top)
            bind.bidLayout.setMargins(resources.dpToPx(16) , 0 , resources.dpToPx(16) , system.bottom)
            
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

        // Streaming playback still via Zego engine
//        StreamingManager.getInstance(requireContext()).startPlayingStream(roomID , bind.hostView)
        subscriber = Core.createSubscriber()
        
        initRenderer()
        startSubscription()
        
        // Initialize sockets
        if (socketUrl.isNotEmpty()) {
            socketManager = SocketManager.getInstance(requireContext())
            socketManager?.initialize(socketUrl , mapOf("uid" to userId))
            socketManager?.connect(onConnected = {
                socketManager?.joinRoom(roomID){
                    log("ROOM JOINED success")
                }
                socketManager?.emitViewerJoin(roomID)
            }) { err -> log("Socket connect error: $err") }

            socketManager?.onViewerCount { count ->
                runSafe { bind.liveCount.text = count.toString() }
            }

            socketManager?.onBidUpdate { json ->
                handleBidUpdate(json)
            }

            // Optional room/session updates (current product, sold, allow flags, countdown)
            socketManager?.onMessage { msg ->
                log("${roomID}  MESGSA E $msg")
                val type = msg.optString("type")
                when (type) {
                    "session_update" -> updateSessionUI(msg)
                    "countdown" -> updateCountdown(msg)
                    else->{
                        if(msg.optString("roomId")==roomID){
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                                commentList.add(LiveChatModel(
                                    msg.optString("userImage"),
                                    msg.optString("userName"),
                                    msg.optString("userId"),
                                    msg.optString("content")
                                ))
                                commentAdapter.notifyItemInserted(commentList.size - 1)
                                bind.recycler.scrollToPosition(commentList.size - 1)
                            }
                        }
                    }
                }
            }
        }

//        initializeChat()

        viewModel.selectedStream.observe(viewLifecycleOwner) { stream ->
            if (stream == roomID) {

               /* bind.userImage.loadUrl(
                    mCtx ,
                    stream.seller?.image.toString() ,
                    placeHolder = draw.user_image
                )*/

//                product = stream.products?.find { it?.isCurrent == true }

                bidProductId = product?.id.toString()

                highestBidAmount = product?.price.toString()

//                bind.userName.text = stream.seller?.name.toString()

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

//                if (stream.seller?.isFollowed == true) {
//                    bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.outline))
//                    bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.onSurface))
//                    bind.follow.text = "Unfollow"
//                } else {
//                    bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx , R.color.primary))
//                    bind.follow.setTextColor(ContextCompat.getColor(mCtx , R.color.background))
//                    bind.follow.text = "Follow"
//                }

                bind.follow.setHapticClickListener {
//                    viewModel.followUser(stream.seller?.id?.request())
                }

                runSafe {
                    bind.bid.text = "Swipe to Bid ${newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString().asMoney()}"
                }

                bind.bid.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
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

        bind.message.setEndIconOnClickListener {
            if (bind.text.value().isNotEmpty()) {
                if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                    socketManager?.sendMessage(roomID,bind.text.value(),userId , userName,userImage)
                    bind.text.text.clear()
                } else {
                    verificationDialog()
                }
            }
        }

        bind.wallet.setHapticClickListener {
            showPaymentAndAddressSheet()
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
        socketManager?.emitViewerJoin(roomID)
    }

    override fun onPause() {
        super.onPause()
        socketManager?.emitViewerLeave(roomID)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        socketManager?.leaveRoom(roomID)
        socketManager?.disconnect()
    }

    private fun handleBidUpdate(json : JSONObject) {
        runSafe {
            val bidAmount = json.optString("bidAmount" , "0")
            bind.bid.text = "Swipe to Bid ${bidAmount.toDoubleOrNull()?.toInt()?.plus(2)?.toString()?.asMoney()}"
            highestBidAmount = bidAmount
            bidProductId = json.optString("productId" , bidProductId)
        }
    }

    private fun updateSessionUI(json : JSONObject) {
        runSafe {
            // minimal fields: product image/name/price, seller, allowBidForAll
            json.optJSONObject("seller")?.let { seller ->
                bind.userName.text = seller.optString("name")
                bind.userImage.loadUrl(mCtx , seller.optString("image"))
            }
            json.optJSONObject("product")?.let { product ->
                bind.productName.text = product.optString("name")
                bind.productImage.loadUrl(mCtx , product.optString("image"))
                val price = product.optString("price" , "0")
                bind.bidPrice.text = price.asMoney()
                highestBidAmount = price
                bidProductId = product.optString("id" , bidProductId)
                bind.bid.text = "Swipe to Bid ${newBidAmount(price.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney()}"
            }
            isAllowBidForAll = json.optBoolean("allowBidForAll" , true)
            val isSold = json.optBoolean("isSold" , false)
            bind.soldLayout.isVisible = isSold
            bind.bidLayout.isVisible = ! isSold
            bind.productLayout.isVisible = ! isSold
        }
    }

    private fun updateCountdown(json : JSONObject) {
        val value = json.optString("bidCountDown" , "")
        runSafe {
            bind.bidTime.isVisible = value.isNotEmpty()
            if (value.isNotEmpty()) bind.bidTime.text = "Ends in $value"
        }
    }

    private fun showInputSheet() {
        val inputSheetBind = InputBottomSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.input_bottom_sheet ,
                null ,
                false
            )
        )
        inputSheet = Alerts.appBottomSheet(mCtx , true , inputSheetBind)

        inputSheetBind.submitBtn.setHapticClickListener {
            val priceText = inputSheetBind.price.value()
            val priceVal = priceText.toDoubleOrNull() ?: 0.0
            val current = highestBidAmount?.toDoubleOrNull() ?: 0.0
            if (priceVal <= current) {
                Alerts.error(mCtx , "Bid amount must be greater than the current highest bid.")
            } else {
                socketManager?.emitBid(
                    roomId = roomID ,
                    userId = userId ,
                    userName = userName ,
                    userImage = userImage ,
                    productId = bidProductId ,
                    bidAmount = priceVal.toString()
                )
                Alerts.success(mCtx , "Bid placed successfully")
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

    fun attemptBid() {
        runSafe {
            log("SWIPED")

            val bidAmount = newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString()

            socketManager?.emitBid(
                roomId = roomID ,
                userId = userId ,
                userName = userName ,
                userImage = userImage ,
                productId = bidProductId ,
                bidAmount = bidAmount
            )

//            sendZimMessage("New high bid: $$bidAmount")
            Alerts.success(mCtx , "Bid placed successfully")
            bind.bid.setCompleted(completed = false , withAnimation = true)
        }
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

  /*  fun sendZimMessage(content : String) {
        val extended = ZIMExtendedData(userImage , userId , userName).toJson()
        chatManager?.sendTextMessage(roomID , content , extended)
        bind.text.setText("")
    }*/

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
                    streamName =roomID,// streamName.ifBlank { Const.ACCOUNT_ID },
                    accountId = Const.ACCOUNT_ID,
                    apiUrl = "https://director.millicast.com/api/director/subscribe"
                )
                
                subscriber.setCredentials(credentials)
                log("Credentials set. Connecting (autoReconnect=true)...")
                subscriber.connect(ConnectionOptions(autoReconnect = true))
                
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
                    subscriber.state.map { it.websocketConnectionState }.distinctUntilChanged().collect { ws ->
                        log("WebSocket state: $ws")
                    }
                }
                viewModel.viewModelScope.launch {
                    subscriber.state.map { it.peerConnectionState }.distinctUntilChanged().collect { pc ->
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
                                        if (!trackState.isActive) holder.disableAsync() else holder.enableAsync(videoSink = bind.hostView)
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
        try {
            subscriberStateJob?.cancel()
        } catch (_: Throwable) {
        }
        try {
            sourceVideoTracks.forEach { it.disableAsync() }
        } catch (_: Throwable) {
        }
        try {
            audioTrack?.disableAsync()
        } catch (_: Throwable) {
        }
//		try {
//			bind.hostView.clearImage()
//		} catch (_: Throwable) {
//		}
        try {
            viewModel.viewModelScope.launch { subscriber.unsubscribe(); subscriber.disconnect() }
        } catch (_: Throwable) {
        }
        try {
            bind.hostView.release()
        } catch (_: Throwable) {
        }
        try {
            eglBase.release()
        } catch (_: Throwable) {
        }
        log("Subscriber cleanup finished")
    }
}


