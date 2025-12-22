package io.bidswipe.app.ui.watchStream

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.gyf.immersionbar.ktx.statusBarHeight
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import com.zerobranch.layout.SwipeLayout
import com.zerobranch.layout.SwipeLayout.SwipeActionsListener
import io.agora.rtc2.video.VideoCanvas
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.LivePollOptionAdapter
import io.bidswipe.app.controller.SellerMenuInfoAdapter
import io.bidswipe.app.databinding.AppReportViewBinding
import io.bidswipe.app.databinding.FollowInfoSheetBinding
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.databinding.InputBottomSheetBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.databinding.PollDetailsSheetBinding
import io.bidswipe.app.databinding.SellerInfoSheetBinding
import io.bidswipe.app.databinding.SendTipSheetBinding
import io.bidswipe.app.databinding.ViewerShowNotesSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.model.PollModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetReportCategoriesResponse
import io.bidswipe.app.network.response.SellerInfoResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
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
import io.bidswipe.app.utils.share.ShareHelper
import io.bidswipe.app.utils.value
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

@SuppressLint("NotifyDataSetChanged", "InflateParams", "ClickableViewAccessibility")
class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {

    override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentWatchStreamBinding.inflate(inflater, view, false)

    private lateinit var roomID: String
    private lateinit var streamID: String
    private lateinit var thumbnail: String

    private var showId: String? = null
    private var showTitle: String? = null
    private var highestBidAmount: String? = ""
    private var bidProductId: String? = ""
    private lateinit var socketUrl: String
    private var commentList = mutableListOf<LiveChatModel?>()
    private lateinit var commentAdapter: CommentAdapter
    private var inputSheet: BottomSheetDialog? = null
    private var sellerId: String? = ""
    private var sellerName: String? = ""
    private var sellerImage: String? = ""
    private var isAllowBidForAll = true
    private var isFollowing = false
    private var socketManager: SocketManager? = null
    private var productList = mutableListOf<LiveShowModel.Product?>()
    private var currentRemoteUid: Int? = null
    private var currentPoll: PollModel? = null
    private var pollSheet: BottomSheetDialog? = null
    private var pollSheetBinding: PollDetailsSheetBinding? = null
    private var livePollOptionList = mutableListOf<PollModel.PollOption>()
    private lateinit var livePollAdapter: LivePollOptionAdapter
    private var followSheetRunnable: Runnable? = null
    private val followSheetHandler = Handler(Looper.getMainLooper())
    private lateinit var pipParams: PictureInPictureParams
    private var isSocketDataLoaded = false
    private var isHandlerRunning = false
    private var showThumbnail: String? = null

    private var showNotes: String? = ""
    companion object {
        fun newInstance(roomID: String, streamID: String, thumbnail: String? = null) =
            WatchStreamFragment().apply {
                arguments = Bundle().apply {
                    putString("roomID", roomID)
                    putString("streamID", streamID)
                    putString("thumbnail", thumbnail)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomID = requireArguments().getString("roomID") ?: ""
        streamID = requireArguments().getString("streamID") ?: ""
        thumbnail = requireArguments().getString("thumbnail") ?: ""
        socketUrl = Const.SOCKET_URL
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log("RoomId: $roomID")
        log("StreamToken: $streamID")

        setUpSwipe()

        initPip()

        // Initialize thumbnail view - show it initially
        bind.thumbnailView.loadUrl(mCtx, thumbnail, R.drawable.placeholder_rect)

//		showThumbnail()

        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { v, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            bind.profileLayout.setMargins(
                top = system.top,
                left = resources.dpToPx(16),
                right = resources.dpToPx(16)
            )
            bind.bidLayout.setMargins(resources.dpToPx(16), 0, resources.dpToPx(16), system.bottom)

            insets
        }

        bind.cutButton.setHapticClickListener {
            finish()
        }

        bind.iconCard.setHapticClickListener {
            if (sellerId?.isNotEmpty() == true) {
                bind.loader.isVisible = true
                viewModel.getSellerInfo(sellerId!!)
            }
        }

        bind.userName.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.getSellerInfo(sellerId!!)
        }

        bind.recycler.setOnTouchListener { view, event ->
            hideKeyboard(view)
            return@setOnTouchListener false
        }

        bind.showNotes.setHapticClickListener {
            showNotesSheet()
            bind.showNotes.isVisible = false
        }

        bind.giveawayLayout.setOnClickListener {
            successToast("Coming Soon..")
        }

        commentAdapter = CommentAdapter(commentList, roomID.split("_")[2])

        livePollAdapter = LivePollOptionAdapter(livePollOptionList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                voteOnPoll(pos)
            }
        })

        bind.recycler.adapter = commentAdapter

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

            socketManager?.onAuctionStarted { json ->
                runSafe {
                    if (json.optString("room_id") == roomID) {
                        requireActivity().runOnUiThread  {
                            if (json.has("product") && json.optJSONObject("product") != null) {
                                val product =  LiveShowModel.Product.fromJson(json.optJSONObject("product"))
                                val startingBidAmount = json.optString("starting_bid_amount") ?:"0"
                                log("LIVE PRODUCT : $product")
                                bind.productLayout.isVisible = true
                                val status  = json.optString("status")

                                log("STATUS : $status")
                                if (status == "sold"){
                                    bind.bidLayout.isVisible = false
                                    bind.soldLayout.isVisible = true
                                    bind.productLayout.isVisible = false
                                }else{
                                    bind.bidLayout.isVisible = true
                                    bind.soldLayout.isVisible = false
                                    bind.productLayout.isVisible = true
                                }

                                updateProductUI(product, startingBidAmount)
                            }else{
                                updateProductUI(null, "0")
                            }

                        }
                    }
                }
            }

            socketManager?.getUpdatedProduct { json ->
                runSafe {
                    requireActivity().runOnUiThread {

                        if (json.optString("room_id") == roomID) {
                            val products = LiveShowModel.fromJson(json)
                            val currentProduct = products.products.find { it?.isCurrent == true }
//                            updateProductUI(currentProduct , currentProduct?.price)
//                            setBidText(currentProduct?.price)
                        }

                    }
                }

            }

            socketManager?.getBidFinalize { json ->
                runSafe {

                    requireActivity().runOnUiThread {

                        val winner = json.getJSONObject("winner")
                        log("WINNER: $winner")

                        if (roomID == json.optString("room_id")) {
                            bind.bidTime.isVisible = false
//							bind.soldLayout.isVisible = true
                            bind.bidLayout.isVisible = false
//							bind.productLayout.isVisible = false
                        }

                        val bidderName = winner.optString("user_name")
                        val bidderImage = winner.optString("user_image")

                        bind.winningLayout.isVisible = true

                        bind.userImage.loadUrl(mCtx, bidderImage)

                        if (userId == winner.optString("user_id")) {
                            bind.winning.text = buildSpannedString {
                                color(ContextCompat.getColor(mCtx, R.color.primary)) {
                                    bold { append(" you won!") }
                                }
                            }
                        } else {
                            bind.winning.text = buildSpannedString {
                                append(bidderName)
                                color(ContextCompat.getColor(mCtx, R.color.primary)) {
                                    bold { append(" has won!") }
                                }
                            }
                        }

                    }

                }
            }

            // Optional room/session updates (current product, sold, allow flags, countdown)
            socketManager?.onMessage { msg ->
                log("$roomID  MESSAGES $msg")

                if (msg.optString("room_id") == roomID) {
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

            }

            socketManager?.onRoomEnded { json ->
                runSafe {
                    if (json.optString("room_end") == roomID) {
                        App.manager.destroyEngine()
                        finish()
                    }
                }
            }
        }

        socketManager?.onRoomCreated { obj ->
            activity?.runOnUiThread {
                if (obj.optString("room_id") == roomID) {
                    updateSessionUI(obj)
                }
            }
        }

        socketManager?.onFollowSellerStatus { obj ->
            activity?.runOnUiThread {
                if (obj.optString("room_id") == roomID && obj.optString("user_id") == userId) {

                    log("IS FOLLOWING : ${obj.optString("is_followed")}")

                    isFollowing = obj.optBoolean("is_followed")

                    bind.follow.isVisible = !obj.optBoolean("is_followed")

                    followSheetRunnable = Runnable { followSheet() }

                    if (!isFollowing  && !isHandlerRunning) {
                        followSheetRunnable?.let { followSheetHandler.postDelayed(it, 30000) }
                        isHandlerRunning = true
                    }
                }
            }
        }

        socketManager?.receiveRaid { obj ->
            requireActivity().runOnUiThread {
                if (obj.optString("source_room_id") == roomID) {
                    val targetRoomId = obj.optString("target_room_id")
                    val rtcToken = obj.optString("rtcToken")
                    onRaid(targetRoomId, rtcToken)
                }
            }
        }

        socketManager?.onVoteErrorResult { obj ->
            requireActivity().runOnUiThread {
                if (obj.optString("roomId") == roomID) {
                    Alerts.error(mCtx, "Vote failed")
                }
            }
        }

        socketManager?.receiveShowNotes { args ->
            runSafe {
                if (args.optString("room_id") == roomID) {
                    requireActivity().runOnUiThread {
                        bind.showNotes.isVisible=true
                     showNotes=   args.optString("show_note")?:""
                    }
                }
            }
        }

        socketManager?.onSaveTipSettingResult { obj ->
            requireActivity().runOnUiThread {
               log("Message : ${obj.optString("tip_message")} ")
            }
        }

        // Poll listeners
        setupPollListeners()

        // Poll card click listener
        bind.poll.setHapticClickListener {
            showPollDetailsSheet()
        }

        bind.message.setEndIconOnClickListener {
            if (bind.messageText.value().isNotEmpty()) {
                if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                    socketManager?.sendMessage(
                        roomID,
                        bind.messageText.value(),
                        userId,
                        userName,
                        userImage
                    )
                    bind.messageText.setText("")
                } else {
                    verificationDialog()
                }
            }
            true
        }

        bind.messageText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (bind.messageText.value().isNotEmpty()) {
                    if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                        socketManager?.sendMessage(
                            roomID,
                            bind.messageText.value(),
                            userId,
                            userName,
                            userImage
                        )
                        bind.messageText.setText("")
                    } else {
                        verificationDialog()
                    }
                }
                true
            } else {
                false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (imeVisible) {
                bind.product.isVisible = false
                bind.bidLayout.isVisible = false
                bind.sideOptions.isVisible = false
            } else {
                bind.product.isVisible = true
                bind.bidLayout.isVisible = true
                bind.sideOptions.isVisible = true
            }
            insets
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

            ShareHelper.openShareSheet(
                parentFragmentManager,
                imageUrl = showThumbnail,
                text =  showTitle,
                sellerInfo = null,
                shareText = shareText,
                type = "show",
                isLive = true
            )
        }

        bind.shop.setHapticClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (requireActivity() as ViewLiveShowActivity).enterPictureInPictureMode(pipParams)
            }

//			requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra("sellerId", sellerId).putExtra("type", "shop"))

            App.isWatchStreamInPIP.value = true
            App.currentSellerId = sellerId

//			requireActivity().finish()


//			startActivity(Intent(mCtx, ProductDetailsActivity::class.java).putExtra("type", "shop").putExtra("sellerId", sellerId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        }

        if (App.profileResponse.value?.buyerIdentityStatus != "verified") {
            verificationDialog()
        }

        viewModel.getSellerInfoRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = it.value.data

                    if (mData != null) {
                        sellerInfoSheet(mData)
                    }

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

        viewModel.sendTipAmountRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    it.value.data

                    Alerts.success(mCtx, "Tip sent successfully")


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

        viewModel.followUserShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    it.value.data

                    isFollowing = true

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

        viewModel.getReportCategoriesRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    val mData = it.value.data

                    if (mData != null) {
                        reportUserDialog(mData)
                    }

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

        viewModel.reportSellerRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    it.value.data

                    Alerts.success(mCtx, "Report sent successfully")

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

        attachAgoraCallbacks()

        socketManager?.joinRoom(roomID, userId) {
            socketManager?.sendMessage(roomID, "Joined \uD83D\uDC4B", userId, userName, userImage)
        }

        if (streamID.isBlank()) {
            log("Stream token missing – unable to join channel")
            return
        }

        if (App.manager.isReady()) {
            App.manager.joinSubscriberChannel(streamID, roomID)
            currentRemoteUid?.let { uid ->
                setupRemoteVideo(uid)
            }
        } else {
            App.manager.joinSubscriberChannel(streamID, roomID)
            currentRemoteUid?.let { uid ->
                setupRemoteVideo(uid)
            }

        }

        log("TOKEN: $streamID")
//		loginAndPlay()
    }

    override fun onPause() {
        super.onPause()
        if (!App.PIPMode) {
            socketManager?.leaveRoom(roomID, userId)
            App.manager.leaveChannel()
        }

        followSheetRunnable?.let { followSheetHandler.removeCallbacks(it) }
//		stopStream()
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager?.leaveRoom(roomID, userId)
        socketManager?.disconnect()
        App.manager.leaveChannel()
    }

    private fun attachAgoraCallbacks() {
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
        // Hide thumbnail when video is ready
        hideThumbnail()
    }

    private fun clearRemoteVideo() {
        bind.hostView.removeAllViews()
        bind.productLayout.isVisible = false
        bind.soldLayout.isVisible = true

        // Show thumbnail again when video is cleared
        /*if (!isSocketDataLoaded) {
            showThumbnail()
        }*/
    }

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
                if (json.optString("room_id") == roomID) {
                    val highestBid = json.getJSONObject("get_highest_bid")
                    val bidAmount = highestBid.optString("bid_amount")
                    log("BID UPDATE: $bidAmount")

                    val bidderName = highestBid.optString("user_name")
                    val bidderImage = highestBid.optString("user_image")

                    log("BID UPDATE: $bidAmount")

                    bind.winningLayout.isVisible = true

                    bind.userImage.loadUrl(mCtx, bidderImage)
                    bind.winning.text = buildSpannedString {
                        append(bidderName)
                        color(ContextCompat.getColor(mCtx, R.color.primary)) {
                            bold { append(" is winning!") }
                        }
                    }

                    setBidText(bidAmount)
                    highestBidAmount = bidAmount
                    bidProductId = highestBid.optString("product_id")
                }
            }

        }
    }

    private fun setBidText(bidAmount: String?) {
        bind.bidPrice.text = bidAmount?.asMoney()
        bind.bidButton.text = buildString {
            append("Bid : ")
            append(newBidAmount(bidAmount?.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney())
            append(" >>")
        }
    }

    private fun newBidAmount(amount: Int): Int {

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

    private fun updateProductUI(liveProduct: LiveShowModel.Product?, bidStartingAmount : String? ) {

        activity?.runOnUiThread {

            log("LIVE PRODUCT : $liveProduct")

            if (liveProduct != null) {
                bind.winningLayout.isVisible = false
                bind.status.isVisible = false
                bind.productName.text = liveProduct.name?.asCapital()
                log("CATEGORY ${liveProduct.category}")
                bind.productCategory.text = liveProduct.category?.name?.asCapital()
                bind.quantity.text = buildString {
                    append("Quantity: ")
                    append(liveProduct.quantity ?: 0)
                }
                bind.productImage.loadUrl(mCtx, liveProduct.image ?: "")
                bind.productImageShop.loadUrl(mCtx, liveProduct.image ?: "")
                val price = liveProduct.price
                bind.price.text = price?.asMoney() ?: ("0.0" + "Shipping + Taxes")

                highestBidAmount = if (bidStartingAmount == "0"){
                    price
                }else{
                    bidStartingAmount
                }

                bidProductId = liveProduct.id
                setBidText(highestBidAmount)

                bind.productLayout.setHapticClickListener {
                    startActivity(
                        Intent(
                            mCtx,
                            ProductDetailsActivity::class.java
                        ).putExtra("productId", liveProduct.id.toString())
                    )
                }

            } else {
                bind.status.isVisible = true
                bind.bidLayout.isVisible = false
                bind.productLayout.isVisible = false
            }
        }

    }

    private fun updateSessionUI(json: JSONObject) {
        runSafe {
            val showData = LiveShowModel.fromJson(json)

            log("SESSION UPDATE: $showData")

            // Mark socket data as loaded and show thumbnail if available
            isSocketDataLoaded = true
            /*	showThumbnail = showData.thumbnail

                // Display thumbnail if available
                if (!showThumbnail.isNullOrEmpty()) {
                    bind.thumbnailView.loadUrl(mCtx, showThumbnail!!)
                    bind.thumbnailView.isVisible = true
                }*/

            productList.clear()
            productList.addAll(showData.products)
            bind.countBadge.isVisible = true
            bind.countBadge.text = productList.size.toString()

            val liveProduct = showData.products.find { it?.isCurrent == true }

            if (showData.highestBid.bidAmount?.isNotEmpty() == true) {
//                highestBidAmount = showData.highestBid.bidAmount
                log("HIGHEST BID: $highestBidAmount")
//                setBidText(highestBidAmount)
            } else {
//                highestBidAmount = ""
//                setBidText((liveProduct?.price?.toDoubleOrNull()?.toInt() ?: 0).toString())
            }

//            updateProductUI(liveProduct , liveProduct?.price)

            isAllowBidForAll = json.optBoolean("allowBidForAll", true)

            sellerId = showData.seller?.id.toString()

            sellerName = showData.seller?.name.toString()
            sellerImage = showData.seller?.image.toString()

            bind.userName.text = showData.seller?.name?.asCapital()
            bind.rating.text = showData.seller?.rating?.ifEmpty { "0.0" }

            bind.userImage.loadUrl(mCtx, showData.seller?.image ?: "")
            log("IMAGE ${showData.seller?.image}")
            bind.liveCount.text = showData.viewerCount

            bind.follow.setHapticClickListener {
                bind.loader.isVisible = true
                viewModel.followUser(sellerId?.request())
            }

            showId = showData.showId
            showTitle = showData.showDetail
            showThumbnail = showData.thumbnail

            log("ALLOW BID FOR ALL: $isAllowBidForAll")

            bind.bidSwipeLayout.setOnActionsListener(object : SwipeActionsListener {
                override fun onOpen(direction: Int, isContinuous: Boolean) {
                    if (direction == SwipeLayout.RIGHT) {
                        if (App.profileResponse.value?.hasShippingAddress == true && App.profileResponse.value?.hasCardAdded == true) {

                            if (isAllowBidForAll) {
                                attemptBid()
                            } else {
                                if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                                    attemptBid()
                                } else {
                                    verificationDialog()
                                }
                            }
                        } else {
                            showPaymentAndAddressSheet()
                        }
                    }
                }

                override fun onClose() {
                    // the main view has returned to the default state
                }
            })

            bind.bid1.onSlideCompleteListener = object : OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {

                    if (App.profileResponse.value?.hasShippingAddress == true && App.profileResponse.value?.hasCardAdded == true) {

                        if (isAllowBidForAll) {
                            attemptBid()
                        } else {
                            if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                                attemptBid()
                            } else {
                                verificationDialog()
                            }
                        }

                    } else {
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
        /*val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet, null, false))
        val productSheet = Alerts.appBottomSheet(mCtx, true, productSheetBind)

        productAdapter = FirebaseProductAdapter(productList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

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
        productSheetBind.addBtn.isVisible = false*/
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
                    startActivity(
                        Intent(mCtx, TrustedBuyerActivity::class.java).putExtra(
                            "slug",
                            "buyer"
                        )
                    )
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
            bind.bid1.setCompleted(completed = false, withAnimation = true)
            bind.bidSwipeLayout.close()

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
                type.text = buildString {
                    append("Address Not Added")
                }
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
                    append(App.profileResponse.value?.defaultCard?.last4 ?: "")
                }

                expiryDate.text = buildString {
                    append(App.profileResponse.value?.defaultCard?.expDate ?: "")
                }
            } else {
                cardNumber.text = buildString {
                    append("Payment Cards Not Added")
                }
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

        bind.bid1.setCompleted(completed = false, withAnimation = true)
        bind.bidSwipeLayout.close()

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
                    val color = if (value.toInt() <= 10) {
                        ContextCompat.getColor(mCtx, R.color.error)
                    } else {
                        ContextCompat.getColor(mCtx, R.color.background)
                    }

                    bind.bidTime.text = buildSpannedString {
                        color(color) {
                            append("Ends in ")
                            append(value)
                        }
                    }
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
            sendTipSheetBind.customOffer.setText(buildString {
                append("10")
            })
        }

        sendTipSheetBind.btnTip25.setHapticClickListener {
            sendTipSheetBind.customOffer.setText(buildString {
                append("25")
            })
        }

        sendTipSheetBind.btnTip50.setHapticClickListener {
            sendTipSheetBind.customOffer.setText(buildString {
                append("50")
            })
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

                if (walletRadio.isChecked && customOffer.text.toString()
                        .toDouble() > ((App.profileResponse.value?.walletAmount ?: "0.0").toString()
                        .toDouble())
                ) {
                    Alerts.error(mCtx, "Insufficient balance")
                    return@setHapticClickListener
                }
            }

            sendTipSheet.dismiss()

            socketManager?.sendTip(
                roomId = roomID,
                showId = showId.toString(),
                userId = userId,
                sellerId = sellerId.toString(),
                amount = sendTipSheetBind.customOffer.text.toString()
            )

            /*bind.loader.isVisible = true
            viewModel.sendTipAmount(
                sellerId!!.request(),
                sendTipSheetBind.customOffer.text.toString().request(),
                null
            )*/

        }

        sendTipSheet.show()
    }

    private fun onRaid(targetRoomId: String, rtcToken: String) {
        viewModel.viewModelScope.launch {
            try {
                socketManager?.leaveRoom(roomID, userId)
                commentList.clear()
                commentAdapter.notifyDataSetChanged()
                currentRemoteUid = null
                clearRemoteVideo()
                roomID = targetRoomId
                streamID = rtcToken

                App.manager.joinSubscriberChannel(streamID, roomID)

                socketManager?.joinRoom(roomID, userId) {
                    socketManager?.sendMessage(
                        roomID,
                        "Joined \uD83D\uDC4B",
                        userId,
                        userName,
                        userImage
                    )
                }
            } catch (e: Exception) {
                log("Raid failed: ${e.message}")
                e.printStackTrace()
            }
        }

    }

    private fun setupPollListeners() {
        // Listen for poll creation
        socketManager?.onPollCreated { json ->
            runSafe {
                if (json.optString("roomId") == roomID) {
                    requireActivity().runOnUiThread {
                        currentPoll = PollModel.fromJson(json)
                        showPollCard()
                        updatePollUI()
                    }
                }
            }
        }

        // Listen for poll updates (vote counts, timer)
        socketManager?.onPollUpdate { json ->
            runSafe {
                requireActivity().runOnUiThread {
                    currentPoll = PollModel.fromJson(json)
                    showPollCard()
                    updatePollUI() // Update poll card preview
                    updatePollSheet() // Update poll details sheet if open
                }

            }
        }

        // Listen for poll ended
        socketManager?.onPollEnded { json ->
            runSafe {
                requireActivity().runOnUiThread {
                    if (json.optString("roomId") == roomID) {
                        currentPoll = null
                        hidePollCard()
                        pollSheet?.dismiss()
                        pollSheetBinding = null
                    }
                }
            }
        }

        // Listen for vote result
        socketManager?.onPollVoteResult { json ->
            runSafe {
                if (json.optString("room_id") == roomID) {
                    requireActivity().runOnUiThread {
                        val success = json.optBoolean("success", false)
                        if (success) {
                            Alerts.success(mCtx, "Vote submitted successfully!")
                            // Poll will be updated via poll_update event
                        } else {
                            val message = json.optString("message", "Failed to submit vote")
                            Alerts.error(mCtx, message)
                        }
                    }
                }
            }
        }
    }

    private fun showPollCard() {
        requireActivity().runOnUiThread {
            bind.poll.isVisible = true
            updatePollUI()
        }
    }

    private fun hidePollCard() {
        requireActivity().runOnUiThread {
            bind.poll.isVisible = false
        }
    }

    private fun updatePollUI() {
        currentPoll?.let { poll ->
            if (poll.roomId == roomID) {
                bind.pollQuestionPreview.text = poll.question ?: "Poll Question"
                bind.pollTimerPreview.text = poll.remainingTime ?: "00:00 remaining"
                bind.pollTotalVotesPreview.text = buildString {
                    append(poll.totalVotes)
                    append(" ")
                    append(if (poll.totalVotes == 1) "vote" else "votes")
                }
            }
        }
    }

    private fun showPollDetailsSheet() {

        log("SHOW POLL DETAILS")

        pollSheetBinding = PollDetailsSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.poll_details_sheet,
                null,
                false
            )
        )

        val pollSheet = Alerts.appBottomSheet(mCtx, true, pollSheetBinding!!)

        livePollOptionList.clear()
        livePollOptionList.addAll(currentPoll?.options ?: mutableListOf())

        pollSheetBinding?.optionRecycler?.adapter = livePollAdapter

        pollSheetBinding?.endPollBtn?.isVisible = false

        pollSheetBinding?.close?.setHapticClickListener {
            pollSheet.dismiss()
        }

        pollSheet.show()
    }

    private fun sellerInfoSheet(data: SellerInfoResponse.Data) {

        val sellerInfoSheetBinding = SellerInfoSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.seller_info_sheet,
                null,
                false
            )
        )

        val sellerInfoSheet = Alerts.appBottomSheet(mCtx, true, sellerInfoSheetBinding)

        val sellerMenuList = mutableListOf<MoreModel>()

        sellerMenuList.add(MoreModel(R.drawable.ic_flying_money, "Tip or Boost", "tip"))
        sellerMenuList.add(MoreModel(R.drawable.ic_rounded_profile, "View Profile", "profile"))
        sellerMenuList.add(MoreModel(R.drawable.ic_outlined_message, "Message", "message"))
//		sellerMenuList.add(MoreModel(R.drawable.ic_mention, "Mention in Chat", "mention"))
        sellerMenuList.add(MoreModel(R.drawable.ic_block, "Block", "block"))
        sellerMenuList.add(MoreModel(R.drawable.ic_report_problem, "Report", "report"))

        sellerInfoSheetBinding.menuRecycler.adapter =
            SellerMenuInfoAdapter(sellerMenuList, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {

                    sellerInfoSheet.dismiss()

                    when (status) {
                        "tip" -> {
                            sendTipSheet()
                        }

                        "profile" -> {
                            startActivity(
                                Intent(
                                    mCtx,
                                    SellerProfileActivity::class.java
                                ).putExtra("userId", sellerId)
                            )
                        }

                        "message" -> {
                            startActivity(
                                Intent(mCtx, ChatActivity::class.java).putExtra("id", sellerId)
                                    .putExtra("name", sellerName)
                                    .putExtra("image", sellerImage)
                            )
                        }

                        "mention" -> {
                        }

                        "block" -> {
                            showBlockConfirmation()
                        }

                        "report" -> {
                            bind.loader.isVisible = true
                            viewModel.getReportCategories()
                        }
                    }
                }
            })

        sellerInfoSheetBinding.follow.isVisible = !isFollowing

        sellerInfoSheetBinding.userName.text = data.sellerDetails?.name
        sellerInfoSheetBinding.rating.text = (data.ratingAvg ?: 0).toString()
        sellerInfoSheetBinding.review.text = (data.review ?: 0).toString()
        sellerInfoSheetBinding.sold.text = (data.soldCount ?: 0).toString()
        sellerInfoSheetBinding.shipping.text = (data.avgShip ?: 0).toString()
        sellerInfoSheetBinding.userImage.loadUrl(mCtx, data.sellerDetails?.profileImage ?: "")

        sellerInfoSheetBinding.follow.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.followUser(sellerId?.request())
            sellerInfoSheet.dismiss()
        }

        sellerInfoSheet.show()
    }

    private fun followSheet() {

        if (App.PIPMode) return

        val ctx = context ?: return
        val inflater = LayoutInflater.from(ctx)
        val followSheetBinding = FollowInfoSheetBinding.bind(
            inflater.inflate(
                R.layout.follow_info_sheet,
                null,
                false
            )
        )

        val followSheet = Alerts.appBottomSheet(ctx, true, followSheetBinding)

        followSheetBinding.image.loadUrl(ctx, sellerImage ?: "")

        followSheetBinding.title.text = buildString {
            append("Follow This Seller!")
        }

        followSheetBinding.message.text = buildSpannedString {
            append("Like what you see? Follow ")
            color(ContextCompat.getColor(ctx, R.color.primary)) {
                append(sellerName)
            }
            append(" to get notifications when they go live!")
        }

        followSheetBinding.primaryBtn.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.followUser(sellerId?.request())
            followSheet.dismiss()
        }

        followSheetBinding.secondaryBtn.setHapticClickListener {
            followSheet.dismiss()
        }

        if (!followSheet.isShowing) {
            followSheet.show()
        }

    }

    private fun updatePollSheet() {
        val poll = currentPoll ?: return
        pollSheetBinding?.let { binding ->
            // Update poll header data

            binding.pollQuestionDetail.text = poll.question ?: "No question"
            binding.pollTimerDetail.text = poll.remainingTime ?: "00:00 remaining"
            binding.pollTotalVotesDetail.text = buildString {
                append(poll.totalVotes)
                append(" total votes")
            }

            if (::livePollAdapter.isInitialized) {
                livePollOptionList.clear()
                livePollOptionList.addAll(poll.options)
                livePollAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun voteOnPoll(optionIndex: Int) {
        val poll = currentPoll ?: return

        // Emit vote
        socketManager?.votePoll(
            roomId = roomID,
            pollId = poll.pollId ?: 0,
            optionIndex = optionIndex,
            userId = userId
        )
    }

    private fun initPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibleRect = Rect()

            (requireActivity() as ViewLiveShowActivity).bind.root.getGlobalVisibleRect(visibleRect)
            pipParams = PictureInPictureParams.Builder().apply {
                setAspectRatio(Rational(100, 200))
//                setAspectRatio(Rational(2, 5))
                setSourceRectHint(visibleRect)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(false)
                }
            }.build()

            activity?.setPictureInPictureParams(pipParams)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            App.PIPMode = true
            bind.profileLayout.isVisible = false
            bind.bottomUI.isVisible = false
            bind.showNotes.isVisible = false
            bind.giveawayLayout.isVisible = false
            log("PIP MODE ON")
        } else {
            App.PIPMode = false
            bind.profileLayout.isVisible = true
            bind.bottomUI.isVisible = true
            bind.showNotes.isVisible = true
            bind.giveawayLayout.isVisible = true

            ProductDetailsActivity.instance?.finish()

            log("PIP MODE OFF")
        }
    }

    private fun showBlockConfirmation() {
        AppBottomSheet(
            mCtx,
            R.drawable.ic_block,
            "Block Seller",
            "Are you sure you want to block this seller?",
            primaryBtnText = "Block",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            iconPadding = 16,
            alertType = AlertType.WARNING,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    blockUser()
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }).show()
    }

    private fun showThumbnail() {
        if (!showThumbnail.isNullOrEmpty()) {
            bind.thumbnailView.isVisible = true
            bind.hostView.isVisible = false
        }
    }

    private fun hideThumbnail() {
        bind.thumbnailView.isVisible = false
        bind.hostView.isVisible = true
    }

    private fun blockUser() {
        bind.loader.isVisible = true
        viewModel.blockUnblockUser(sellerId?.request()!!)

        viewModel.blockUnblockUserRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    it.value.data
                    if (it.value.status == "success") {
                        Alerts.success(mCtx, it.value.message ?: "User blocked successfully")
                        finish()
                    } else {
                        Alerts.error(mCtx, it.value.message ?: "Failed to block user")
                    }
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

    fun reportUserDialog(data: List<GetReportCategoriesResponse.Data?>) {

        val mBind = AppReportViewBinding.bind(
            layoutInflater.inflate(
                R.layout.app_report_view, null, false
            )
        )
        val sheet = Alerts.appBottomSheet(mCtx, true, mBind)

        val reportCategoryAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            data.map { it?.name?.asCapital() }
        )

        mBind.reason.setAdapter(reportCategoryAdapter)
        val reportDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        mBind.reason.setDropDownBackgroundDrawable(reportDrawable)

        mBind.reason.setOnItemClickListener { _, _, position, _ ->
            val selectedProcessingCategory = data[position]
            log("Selected processing category: $selectedProcessingCategory")
        }

        mBind.reason.setHapticClickListener {
            mBind.reason.showDropDown()
        }

        mBind.submitReport.setHapticClickListener {

            if (mBind.reason.text.toString().isEmpty()) {
                Alerts.error(mCtx, "Please select a reason")
                return@setHapticClickListener
            }

            if (mBind.tellMore.text.toString().isEmpty()) {
                Alerts.error(mCtx, "Please tell us more")
                return@setHapticClickListener
            }

            val reasonId =
                data.find { it?.name?.asCapital() == mBind.reason.text.toString() }?.id.toString()

            bind.loader.isVisible = true
            viewModel.reportSeller(
                sellerId?.request()!!,
                reasonId.request(),
                mBind.tellMore.text.toString().request()
            )

            sheet.dismiss()
        }

        sheet.show()
    }

    private fun showNotesSheet() {
        val showNotesSheetBind = ViewerShowNotesSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.viewer_show_notes_sheet,
                null,
                false
            )
        )

        val newHeight = requireActivity().window?.decorView?.measuredHeight
        val viewGroupLayoutParams = showNotesSheetBind.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        viewGroupLayoutParams.height = (newHeight ?: 0) - (statusBarHeight)

        showNotesSheetBind.root.layoutParams = viewGroupLayoutParams

        showNotesSheetBind.notes.setMargins(
            resources.dpToPx(16),
            resources.dpToPx(16),
            resources.dpToPx(16),
            navigationBarHeight
        )

        val sheet = Alerts.appBottomSheet(mCtx, false, showNotesSheetBind)

        showNotesSheetBind.notes.setHtmlFromString(showNotes?.ifEmpty { "No notes added yet." },false)

        showNotesSheetBind.close.setHapticClickListener {
            sheet.dismiss()
            bind.showNotes.isVisible = true
        }

        sheet.show()
    }

}