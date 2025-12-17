package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.caneryilmaz.apps.luckywheel.constant.ArrowPosition
import com.caneryilmaz.apps.luckywheel.data.WheelData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.gyf.immersionbar.ktx.statusBarHeight
import io.agora.rtc2.Constants
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.LivePollOptionAdapter
import io.bidswipe.app.controller.LiveSellerAdapter
import io.bidswipe.app.controller.PollOptionAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShareTarget
import io.bidswipe.app.controller.ShareTargetAdapter
import io.bidswipe.app.databinding.ActivityAgoraPublisherBinding
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.CreatePollSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveSellerSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.PollDetailsSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.RandomizerSheetBinding
import io.bidswipe.app.databinding.SellerTipSettingsSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.databinding.ShowConfirmationAlertBinding
import io.bidswipe.app.databinding.ShowNotesSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.PollModel
import io.bidswipe.app.model.PollOptionModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
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
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import java.io.File
import java.io.FileOutputStream

@SuppressLint("NotifyDataSetChanged")
class AgoraPublisherActivity : BaseActivity() {

    private val bind by bind(ActivityAgoraPublisherBinding::inflate)

    private val viewModel by viewModels<DashViewModel>()

    private var liveShowData: LiveShowModel? = null
    private var showId = ""
    private var showTitle: String? = null
    private var showThumbnail: String? = null
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
    private var pollOptionList = mutableListOf<PollOptionModel?>()
    private var livePollOptionList = mutableListOf<PollModel.PollOption>()
    private lateinit var pollOptionAdapter: PollOptionAdapter
    private lateinit var livePollAdapter: LivePollOptionAdapter
    private var currentPoll: PollModel? = null
    private var pollSheetBinding: PollDetailsSheetBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            supportActionBar(false)
            keyboardEnable(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->

                insets
            }
        } else {
            // For Android 14 and below
//			window.statusBarColor = color
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

        setUpWheel(listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5"))

        showId = liveShowData?.showId ?: ""
        showTime = intent.getStringExtra("time") ?: ""

        roomID = "live_room_${userId}_${showId}"

        viewModel.categoryId = liveShowData?.categoryId ?:""

        bind.hostName.text = userName.asCapital()
        bind.hostImage.loadUrl(this, userImage)

        initPip()

        App.manager = AgoraManager(this, Const.APP_ID_AGORA)

        bind.loader.isVisible = true

        viewModel.getAgoraToken(roomID.request())

        commentAdapter = CommentAdapter(commentList, userId)
        bind.recycler.adapter = commentAdapter

        pollOptionList.add(
            PollOptionModel(
                title = "Option 1",
                hint = "Enter your option"
            )
        )

        pollOptionAdapter = PollOptionAdapter(pollOptionList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
            }
        })

        livePollAdapter = LivePollOptionAdapter(livePollOptionList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })


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
            }

            if (bind.messageText.value().isNotEmpty()) {
                socketManager?.sendMessage(
                    roomID,
                    bind.messageText.value(),
                    userId,
                    userName,
                    userImage
                )
                bind.messageText.setText("")
            }
            true
        }

        bind.messageText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (!isShowLive) {
                    Alerts.error(this, "Please start live show to send message")
                }

                if (bind.messageText.value().isNotEmpty()) {
                    socketManager?.sendMessage(
                        roomID,
                        bind.messageText.value(),
                        userId,
                        userName,
                        userImage
                    )
                    bind.messageText.setText("")
                }
                true
            } else {
                false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isShowLive) {
                if (imeVisible) {
                    bind.product.isVisible = false
                    bind.startBtn.isVisible = false
                    bind.menuLayout.isVisible = false
                } else {
                    bind.product.isVisible = true
                    bind.menuLayout.isVisible = true
                }
            }

            insets
        }

        bind.more.setHapticClickListener {
            showMoreSheet()
        }

        bind.promote.setHapticClickListener {
//            if (isShowLive && promotePlans.isNotEmpty()) {
                showPromoteSheet()
//            }
        }

        bind.clip.setHapticClickListener {
            createClipSheet()
        }

        bind.share.setHapticClickListener {
//            showShareBottomSheet()


            val shareText = buildString {
                append(Const.BASE_URL)
                append("/live-show?roomId=$roomID")
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share via")

            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
            } else {
                errorToast("No sharing apps available")
            }

        }

        bind.showNotes.setHapticClickListener {
            if (isShowLive) {
                showNotesSheet()
                bind.showNotes.isVisible = false
            } else {
                errorToast("Please start live show to access this feature")
            }
        }

        bind.poll.setHapticClickListener {
            if (isShowLive) {
                pollDetailSheet()
            } else {
                Alerts.error(this, "Please start live show to access this feature")
            }
        }

        /*val animator = ObjectAnimator.ofFloat(bind.poll, "alpha", 1f, 0f).apply {
            duration = 500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }
        animator.start()*/

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

        viewModel.getLiveSellerRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.getLiveSellerRepo.value = null

                    val dataList = it.value.data ?: mutableListOf()

                    if (dataList.isEmpty()) {
                        Toast.makeText(this, "No sellers found currently", Toast.LENGTH_SHORT)
                            .show()
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

        socketManager?.onPollCreated { json ->
            runSafe {
                runOnUiThread {
                    if (json.optString("roomId") == roomID) {
                        currentPoll = PollModel.fromJson(json)
                        showPollCard()
                        updatePollUI()
                        updatePollSheet()
                    }
                }

            }
        }

        socketManager?.onPollUpdate { json ->
            runSafe {
                runOnUiThread {
                    if (json.optString("roomId") == roomID) {
                        currentPoll = PollModel.fromJson(json)
                        showPollCard()
                        updatePollUI() // Update poll card preview
                        updatePollSheet() // Update poll details sheet if open
                    }
                }

            }
        }

        // Listen for poll ended
        socketManager?.onPollEnded { json ->
            runSafe {
                if (json.optString("roomId") == roomID) {
                    runOnUiThread {
                        currentPoll = null
                        hidePollCard()
                        pollSheetBinding = null
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        App.manager.destroyEngine()

        isShowLive = false

        // Socket cleanup
        runSafe {

            if (currentPoll != null) {
                socketManager?.endPoll(roomID, currentPoll?.pollId.toString())
            }

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

        showConfirmationSheetBind.timing.text = buildString {
                append("Show Starts at ")
                append(Utils.getFormattedDateTime("HH:mm:ss", "hh:mm a", showTime))
            }

        showConfirmationSheetBind.startBtn.setHapticClickListener {
            showConfirmationSheet.dismiss()

            App.manager.joinChannel(agoraToken, channelName)

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
        socketManager?.createRoom(data)

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

                        val bidderName = winner.optString("user_name")
                        val bidderImage = winner.optString("user_image")

                        bind.winningLayout.isVisible = true

                        bind.userImage.loadUrl(this, bidderImage)
                        bind.winning.text = buildSpannedString {
                            append(bidderName)
                            color(
                                ContextCompat.getColor(
                                    this@AgoraPublisherActivity,
                                    R.color.primary
                                )
                            ) {
                                bold { append(" has won!") }
                            }
                        }

                        product?.status = "sold"
                        product?.isCurrent = false

                        bind.status.isVisible = true

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

    private fun showProductSheet() {
        val bottomSheetFragment = ProductsForLiveShowFragment()
        bottomSheetFragment.show(supportFragmentManager, "BOTTOM_SHEET_TAG")

/*        val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet, null, false))

        val newHeight = window?.decorView?.measuredHeight
        val viewGroupLayoutParams = productSheetBind.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        viewGroupLayoutParams.height = (newHeight ?: 0) - (statusBarHeight + navigationBarHeight)
        productSheetBind.root.layoutParams = viewGroupLayoutParams

        val productSheet = Alerts.appBottomSheet(this, true, productSheetBind)

        var selectedPos = -1

   productAdapter = FirebaseProductAdapter("live_show", productList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

                if (productList[pos]?.status == "sold") {
                    Alerts.error(this@AgoraPublisherActivity, "This product is already sold")
                } else if (status == "start_auction") {
                    auctionSettingsSheet()
                } else if (status == "set_next") {

                } else {
//                    productList.forEachIndexed { index, item ->
//
//                        item?.selected = index == pos
//                        productSheetBind.recycler.adapter?.notifyDataSetChanged()
//
//                    }
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

        }*/
    }

    fun updateProductUI(liveProduct: LiveShowModel.Product?) {

        runOnUiThread {

            if (liveProduct != null) {
                log("updateProductUI : $liveProduct")
                bind.product.isVisible = true
                bind.productLayout.isVisible = true
                bind.productName.text = liveProduct.name?.asCapital()
                bind.productCategory.text = liveProduct.category?.asCapital()
                bind.quantity.text = buildString {
                    append("Quantity: ")
                    append(liveProduct.quantity ?: 0)
                }
                bind.productImage.loadUrl(this, liveProduct.image ?: "")
                bind.productImageShop.loadUrl(this, liveProduct.image ?: "")
                val price = liveProduct.price
                bind.bidPrice.text = price?.asMoney()
                bind.status.isVisible = false
            }

        }

    }

    private fun updateCountdown(json: JSONObject) {
        val value = json.optString("remaining")
        runSafe {
            this.runOnUiThread {
                if (json.optString("room_id") == roomID) {
                    bind.bidTime.isVisible = true
                    log("BID TIMER UPDATE : $value")
                    val color = if (value.toInt() <= 10) {
                        ContextCompat.getColor(this@AgoraPublisherActivity, R.color.error)
                    } else {
                        ContextCompat.getColor(this@AgoraPublisherActivity, R.color.background)
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

    private fun handleBidUpdate(json: JSONObject) {
        runSafe {
            runOnUiThread {
                if (json.optString("room_id") == roomID) {
                    val highestBid = json.getJSONObject("get_highest_bid")
                    val bidAmount = highestBid.optString("bid_amount")
                    val bidderName = highestBid.optString("user_name")
                    val bidderImage = highestBid.optString("user_image")

                    log("BID UPDATE: $bidAmount")

                    bind.winningLayout.isVisible = true

                    bind.userImage.loadUrl(this, bidderImage)
                    bind.winning.text = buildSpannedString {
                        append(bidderName)
                        color(
                            ContextCompat.getColor(
                                this@AgoraPublisherActivity,
                                R.color.primary
                            )
                        ) {
                            bold { append(" is winning!") }
                        }
                    }

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
                            if (isShowLive) {
                                tipSettingsSheet()
                            } else {
                                errorToast("Please start live show to access this feature")
                            }
                        }

                        2 -> {
                            moreSheet.dismiss()
                            bind.loader.isVisible = true
                            viewModel.getLiveSeller()
                        }

                        3 -> {
                            moreSheet.dismiss()
                            if (isShowLive) {
                                if (currentPoll == null) {
                                    createPollSheet()
                                } else {
                                    Alerts.error(
                                        this@AgoraPublisherActivity,
                                        "Poll already created"
                                    )
                                }
                            } else {
                                Alerts.error(
                                    this@AgoraPublisherActivity,
                                    "Please start live show to create a poll"
                                )
                            }
                        }

                        4 -> {
                            moreSheet.dismiss()
                            showRandomizerSheet()
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

            socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
            App.manager.destroyEngine()
            finishAfterTransition()
        }
        sheet.show()
    }

    private fun tipSettingsSheet() {
        val tipSettingsSheetBind =
            SellerTipSettingsSheetBinding.bind(
                layoutInflater.inflate(
                    R.layout.seller_tip_settings_sheet,
                    null,
                    false
                )
            )
        val sheet = Alerts.appBottomSheet(this, true, tipSettingsSheetBind)
        tipSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }

        tipSettingsSheetBind.save.setHapticClickListener {

            when {
                tipSettingsSheetBind.tipMessage.value().isEmpty() -> {
                    errorToast("Please enter a tip message")
                }

                else -> {
                    socketManager?.saveTipSetting(
                        liveShowData?.showId.toString(),
                        tipSettingsSheetBind.tipMessage.value(),
                        tipSettingsSheetBind.showLiveChat.isChecked
                    )
                    sheet.dismiss()
                }

            }
        }
        sheet.show()
    }

    private fun auctionSettingsSheet() {
        var selectedCounterTimer = 0
        var selectedRequiredTime = 0

        val auctionSettingsSheetBind =
            AuctionSettingsSheetBinding.bind(
                layoutInflater.inflate(
                    R.layout.auction_settings_sheet,
                    null,
                    false
                )
            )

        val sheet = Alerts.appBottomSheet(this, true, auctionSettingsSheetBind)

        val extraTimer = listOf(5, 7, 10)
        extraTimer.forEachIndexed { index, time ->
            val chip = Utils.makeAChip(
                mCtx = this,
                text = "${time}s",
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                auctionSettingsSheetBind.timerChips.check(chip.id)
                selectedCounterTimer = time
            }
            auctionSettingsSheetBind.timerChips.addView(chip)
        }

        val requiredTimeList = listOf(15, 30, 45)
        val requiredTimeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            requiredTimeList
        )

        auctionSettingsSheetBind.requiredTime.setAdapter(requiredTimeAdapter)

        auctionSettingsSheetBind.requiredTime.setOnItemClickListener { _, _, position, _ ->
            selectedRequiredTime = requiredTimeList[position]
            auctionSettingsSheetBind.requiredTime.setText("${requiredTimeList[position]}s", false)
        }

        auctionSettingsSheetBind.requiredTime.setHapticClickListener {
            auctionSettingsSheetBind.requiredTime.showDropDown()
        }

        auctionSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }
        auctionSettingsSheetBind.start.setHapticClickListener {
            /* auctionSettingsSheetBind.startingBid.value()
             selectedRequiredTime
             selectedCounterTimer
             auctionSettingsSheetBind.suddenDeath.isChecked*/
        }

        sheet.show()

    }

    private fun showNotesSheet() {
        val showNotesSheetBind = ShowNotesSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.show_notes_sheet,
                null,
                false
            )
        )

        val newHeight = window?.decorView?.measuredHeight
        val viewGroupLayoutParams = showNotesSheetBind.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        viewGroupLayoutParams.height = (newHeight ?: 0) - (statusBarHeight)
        showNotesSheetBind.root.layoutParams = viewGroupLayoutParams

        showNotesSheetBind.post.setMargins(
            resources.dpToPx(16),
            resources.dpToPx(16),
            resources.dpToPx(16),
            navigationBarHeight + resources.dpToPx(32)
        )

        val sheet = Alerts.appBottomSheet(this, true, showNotesSheetBind)

        sheet.setOnDismissListener {
            bind.showNotes.isVisible = true
        }

        Aztec.with(
            showNotesSheetBind.showNotes,
            showNotesSheetBind.formattingToolbar,
            editorListener
        )
        showNotesSheetBind.close.setHapticClickListener {
            sheet.dismiss()
            bind.showNotes.isVisible = true
        }

        showNotesSheetBind.post.setHapticClickListener {
            val notes =showNotesSheetBind.showNotes.toFormattedHtml()
            if (notes.isEmpty()) {
                errorToast("Please enter some notes")
            } else {
                socketManager?.addShowNotes(
                    roomID,
                    notes
                )
                sheet.dismiss()
                bind.showNotes.isVisible = true
            }
        }

        sheet.show()
    }

    private val editorListener = object : IAztecToolbarClickListener {
        override fun onToolbarCollapseButtonClicked() {

        }

        override fun onToolbarExpandButtonClicked() {

        }

        override fun onToolbarFormatButtonClicked(
            format: ITextFormat,
            isKeyboardShortcut: Boolean
        ) {

        }

        override fun onToolbarHeadingButtonClicked() {

        }

        override fun onToolbarHtmlButtonClicked() {

        }

        override fun onToolbarListButtonClicked() {

        }

        override fun onToolbarMediaButtonClicked(): Boolean {
            return true
        }

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
                socketManager?.createRaid(
                    roomID,
                    selectedItem?.roomId.toString(),
                    selectedItem?.id.toString(),
                    userId
                )
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

        val newHeight = window?.decorView?.measuredHeight
        val viewGroupLayoutParams = promoteSheetBind.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        viewGroupLayoutParams.height = (newHeight ?: 0) - (statusBarHeight)
        promoteSheetBind.root.layoutParams = viewGroupLayoutParams

        promoteSheetBind.bottomText.setMargins(
            0,
            0,
           0,
            navigationBarHeight + resources.dpToPx(32)
        )


        val promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)

        promoteSheetBind.optionList.adapter =
            PromoteSheetAdapter(promotePlans, object : RecyclerClicks {
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

    private fun createPollSheet() {
        val pollSheetBind = CreatePollSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.create_poll_sheet,
                null,
                false
            )
        )
        val pollSheet = Alerts.appBottomSheet(this, true, pollSheetBind)

        // Duration options in minutes
        val durationOptions = listOf("1", "2", "3", "5", "10", "15", "30")
        var selectedDuration = 5 // Default 5 minutes

        pollSheetBind.pollOptions.adapter = pollOptionAdapter

        pollOptionAdapter.holderList.clear()

        pollOptionAdapter.notifyDataSetChanged()

        // Set default duration text
        pollSheetBind.pollDuration.setText("${selectedDuration} minutes", false)

        pollSheetBind.addOption.setOnClickListener {
            pollOptionList.add(
                PollOptionModel(
                    title = "Option ${pollOptionList.size + 1}",
                    hint = "Enter your option"
                )
            )
            pollSheetBind.pollOptions.adapter?.notifyItemInserted(pollOptionList.size - 1)
        }

        val durationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            durationOptions
        )
        pollSheetBind.pollDuration.setAdapter(durationAdapter)

        pollSheetBind.pollDuration.setOnItemClickListener { _, _, position, _ ->
            selectedDuration = durationOptions[position].toInt()
            pollSheetBind.pollDuration.setText("${selectedDuration} minutes", false)
        }


        pollSheetBind.pollDuration.setHapticClickListener {
            pollSheetBind.pollDuration.showDropDown()
        }

        pollSheetBind.close.setHapticClickListener {
            pollSheet.dismiss()
        }

        pollSheetBind.createPollBtn.setHapticClickListener {
            val question = pollSheetBind.pollQuestion.text?.toString()?.trim() ?: ""
            val options = getVariantData()

            // Validation
            if (question.isEmpty()) {
                Alerts.error(this, "Please enter a poll question")
                return@setHapticClickListener
            }

            if (options.isEmpty()) {
                Alerts.error(this, "Please add at least 2 options")
                return@setHapticClickListener
            }

            if (options.size < 2) {
                Alerts.error(this, "Please add at least 2 options")
                return@setHapticClickListener
            }

            val duration = selectedDuration * 60

            // Emit poll creation via socket
            socketManager?.createPoll(roomID, question, options, duration)

            // Show success message
            successToast("Poll created successfully!")

            // Dismiss the sheet
            pollSheet.dismiss()
        }

        pollSheet.show()
    }

    private fun pollDetailSheet() {

        pollSheetBinding = PollDetailsSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.poll_details_sheet,
                null,
                false
            )
        )

        val pollSheet = Alerts.appBottomSheet(this, true, pollSheetBinding!!)

        livePollOptionList.clear()
        livePollOptionList.addAll(currentPoll?.options ?: mutableListOf())

        pollSheetBinding?.optionRecycler?.adapter = livePollAdapter

        pollSheetBinding?.close?.setHapticClickListener {
            pollSheet.dismiss()
        }

        pollSheetBinding?.endPollBtn?.setHapticClickListener {
            socketManager?.endPoll(roomID, currentPoll?.pollId.toString())
            pollSheet.dismiss()
        }

        pollSheet.show()
    }

    fun shareLiveShow(context: Context, showTitle: String, showUrl: String, imageUrl: String) {
        Thread {
            try {
                // 1️⃣ Download image from URL as Bitmap
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()

                // 2️⃣ Save it as a temporary file
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val imageFile = File(cachePath, "thumb.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // 3️⃣ Get content URI using FileProvider
                val imageUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                // 4️⃣ Create share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, "$showTitle\n$showUrl")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // 5️⃣ Start share sheet on main thread
                (context as? android.app.Activity)?.runOnUiThread {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Live Show via"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun getVariantData(): List<String> {
        return pollOptionAdapter.getAllVariantData()
    }

    private fun showPollCard() {
        runOnUiThread {
            bind.poll.isVisible = true
            updatePollUI()
        }
    }

    private fun hidePollCard() {
        runOnUiThread {
            bind.poll.isVisible = false
        }
    }

    private fun updatePollUI() {
        currentPoll?.let { poll ->
            if (poll.roomId == roomID) {
                bind.pollQuestionPreview.text = poll.question ?: "Poll Question"
                bind.pollTimerPreview.text = poll.remainingTime ?: "00:00 remaining"
                bind.pollTotalVotesPreview.text =
                    "${poll.totalVotes} ${if (poll.totalVotes == 1) "vote" else "votes"}"

            }
        }
    }

    private fun updatePollSheet() {
        val poll = currentPoll
        if (poll?.roomId == roomID) {
            pollSheetBinding?.let { binding ->
                binding.pollQuestionDetail.text = poll.question ?: "No question"
                binding.pollTimerDetail.text = poll.remainingTime ?: "00:00 remaining"
                binding.pollTotalVotesDetail.text = "${poll.totalVotes} total votes"
                if (::livePollAdapter.isInitialized) {
                    livePollOptionList.clear()
                    livePollOptionList.addAll(poll.options)
                    livePollAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("UseKtx")
    fun setUpWheel(options: List<String>) {
        if (options.isEmpty()) return

        val colors = listOf(
            Color.parseColor("#B28704"), // Amber
            Color.parseColor("#388E3C"), // Green
            Color.parseColor("#1976D2"), // Blue
            Color.parseColor("#6A1B9A"), // Purple
            Color.parseColor("#D32F2F"), // Red
            Color.parseColor("#0097A7"), // Cyan
            Color.parseColor("#C2185B"), // Pink
            Color.parseColor("#F57C00"), // Orange
            Color.parseColor("#303F9F"), // Indigo
            Color.parseColor("#689F38"), // Light Green
            Color.parseColor("#00796B"), // Teal
            Color.parseColor("#512DA8")  // Deep Purple
        )

        val wheelData = ArrayList(
            options.mapIndexed { index, rawText ->
                val text = rawText.trim()
                WheelData(
                    text = text,
                    textColor = intArrayOf(Color.BLACK),
                    backgroundColor = intArrayOf(colors[index % colors.size])
                )
            }
        )

        bind.luckyWheel.apply {
            setCenterPointRadius(50f)
            setWheelData(wheelData = wheelData)
            setWheelCenterTextColor(intArrayOf(clr.secondary))
            setCornerPointsRadius(20f)
            setRotationCompleteListener {

            }

            setArrowPosition(ArrowPosition.CENTER)

            setWheelCenterArrow(
                ContextCompat.getDrawable(this@AgoraPublisherActivity, R.drawable.wheel_center)!!,
                44f, 44f,
                ContextCompat.getColor(this@AgoraPublisherActivity, clr.primary),
                0f, 0f
            )

            val drawable =
                ContextCompat.getDrawable(this@AgoraPublisherActivity, R.drawable.ic_dollar)!!
            val color = ContextCompat.getColor(this@AgoraPublisherActivity, R.color.onPrimary)
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            setWheelCenterImage(drawable, 12f, 12f)
        }
    }

    fun showRandomizerSheet() {
        val randomizerSheetBind = RandomizerSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.randomizer_sheet,
                null,
                false
            )
        )

        val randomSheet = Alerts.appBottomSheet(this, true, randomizerSheetBind)

        if (bind.luckyWheelLayout.isVisible) {
            randomizerSheetBind.spinControllersView.isVisible = true
            randomizerSheetBind.showSpin.isVisible = false
        } else {
            randomizerSheetBind.spinControllersView.visibility = View.INVISIBLE
            randomizerSheetBind.showSpin.isVisible = true
        }

        var currentEntries = mutableListOf<String>()

        randomizerSheetBind.close.setHapticClickListener {
            randomSheet.dismiss()
        }

        randomSheet.show()

        randomizerSheetBind.showSpin.setHapticClickListener {

            // Show wheel and controls
            bind.luckyWheelLayout.isVisible = true
            randomizerSheetBind.spinControllersView.isVisible = true
            randomizerSheetBind.showSpin.isVisible = false
        }

        randomizerSheetBind.manualEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // Handle text changes as the user types
                val rawText = randomizerSheetBind.manualEntry.text?.toString().orEmpty()

                // Split text into lines
                currentEntries = rawText
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()

                if (currentEntries.isEmpty()) {
                    Alerts.error(this@AgoraPublisherActivity, "Please enter at least one entry")
                } else {
                    setUpWheel(currentEntries)
                }
            }

            override fun afterTextChanged(editable: Editable?) {

            }
        })

        // Hide wheel
        randomizerSheetBind.hideWheel.setHapticClickListener {
            bind.luckyWheelLayout.isVisible = false
            randomizerSheetBind.spinControllersView.visibility = View.INVISIBLE
            randomizerSheetBind.showSpin.isVisible = true
        }

        randomizerSheetBind.shuffleEntries.setHapticClickListener {
            if (currentEntries.isEmpty()) {
                val rawText = randomizerSheetBind.manualEntry.text?.toString().orEmpty()
                currentEntries = rawText
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
            }

            if (currentEntries.isEmpty()) {
                Alerts.error(this, "Please enter entries to shuffle")
                return@setHapticClickListener
            }

            currentEntries.shuffle()
            randomizerSheetBind.manualEntry.setText(currentEntries.joinToString("\n"))
            setUpWheel(currentEntries)
        }

        randomizerSheetBind.removeAll.setOnClickListener {
            currentEntries.clear()
            randomizerSheetBind.manualEntry.setText("")
            setUpWheel(currentEntries)
        }

        // Spin the wheel
        randomizerSheetBind.spinWheel.setOnClickListener {
            bind.luckyWheel.rotateWheel()
        }

    }

    private fun showShareBottomSheet() {

        if (showId == null) {
            errorToast("Show information not available")
            return
        }

        val shareUrl = buildString {
            append(Const.BASE_URL)
            append("/live-show?showId=$showId")
        }

        val sheet = BottomSheetDialog(this)
        val binding = ShareSheetBinding.inflate(layoutInflater)
        sheet.setContentView(binding.root)

        // Populate preview card
        binding.showTitle.text = showTitle ?: "Live Show"
        binding.showSubtitle.text = "Shop Live Now!"

        if (!showThumbnail.isNullOrEmpty()) {
            binding.showImg.loadUrl(this, showThumbnail!!)
        }

        // Setup share targets
        val shareTargets = mutableListOf<ShareTarget>().apply {
            add(ShareTarget(R.drawable.placeholder_square, "Search", "search"))
            add(ShareTarget(R.drawable.placeholder_square, "Messages", "sms"))
            add(ShareTarget(R.drawable.placeholder_square, "Copy Link", "copy"))
            add(ShareTarget(R.drawable.placeholder_square, "IG Stories", "ig_stories"))
            add(ShareTarget(R.drawable.placeholder_square, "Instagram", "instagram"))
            add(ShareTarget(R.drawable.placeholder_square, "Messenger", "messenger"))
            add(ShareTarget(R.drawable.placeholder_square, "WhatsApp", "whatsapp"))
            add(ShareTarget(R.drawable.placeholder_square, "More", "more"))
        }

        binding.shareTargetsRecycler.layoutManager = GridLayoutManager(this, 4)
        val adapter = ShareTargetAdapter(shareTargets, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                handleShareTarget(status ?: "", shareUrl, showTitle ?: "Live Show")
                sheet.dismiss()
            }
        })
        binding.shareTargetsRecycler.adapter = adapter

        binding.close.setHapticClickListener {
            sheet.dismiss()
        }

        sheet.show()
    }

    private fun handleShareTarget(type: String, shareUrl: String, showTitle: String) {
        when (type) {
            "search" -> {
                // Navigate to search or show search dialog
                // Implementation depends on your app's search functionality
                errorToast("Search functionality to be implemented")
            }

            "sms" -> {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")
                    putExtra("sms_body", "$showTitle\n$shareUrl")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    errorToast("SMS app not available")
                }
            }

            "copy" -> {
                val clipboard = this?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Share Link", shareUrl)
                clipboard.setPrimaryClip(clip)
                successToast("Link copied to clipboard")
            }

            "ig_stories" -> {
                shareToInstagramStories(showTitle, shareUrl)
            }

            "instagram" -> {
                shareToInstagram(showTitle, shareUrl)
            }

            "messenger" -> {
                shareToMessenger(showTitle, shareUrl)
            }

            "whatsapp" -> {
                shareToWhatsApp(showTitle, shareUrl)
            }

            "more" -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "$showTitle\n$shareUrl")
                    setType("text/plain")
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Share via")
                if (shareIntent.resolveActivity(packageManager) != null) {
                    startActivity(chooserIntent)
                } else {
                    errorToast("No sharing apps available")
                }
            }
        }
    }

    private fun shareToInstagramStories(showTitle: String, shareUrl: String) {
        if (showThumbnail.isNullOrEmpty()) {
            shareToInstagram(showTitle, shareUrl)
            return
        }

        Thread {
            try {
                val bitmap = Glide.with(this)
                    .asBitmap()
                    .load(showThumbnail)
                    .submit()
                    .get()

                val cachePath = File(this.cacheDir, "images")
                cachePath.mkdirs()
                val imageFile = File(cachePath, "share_thumb.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val imageUri: Uri = FileProvider.getUriForFile(
                    this,
                    "${this.packageName}.fileprovider",
                    imageFile
                )

                val shareIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                    setDataAndType(imageUri, "image/*")
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra("content_url", shareUrl)
                    putExtra("top_background_color", "#000000")
                    putExtra("bottom_background_color", "#000000")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                runOnUiThread {
                    if (shareIntent.resolveActivity(packageManager) != null) {
                        startActivity(shareIntent)
                    } else {
                        shareToInstagram(showTitle, shareUrl)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    shareToInstagram(showTitle, shareUrl)
                }
            }
        }.start()
    }

    private fun shareToInstagram(showTitle: String, shareUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.instagram.android")
            putExtra(Intent.EXTRA_TEXT, "$showTitle\n$shareUrl")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            errorToast("Instagram not installed")
        }
    }

    private fun shareToMessenger(showTitle: String, shareUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.facebook.orca")
            putExtra(Intent.EXTRA_TEXT, "$showTitle\n$shareUrl")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            errorToast("Messenger not installed")
        }
    }

    private fun shareToWhatsApp(showTitle: String, shareUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, "$showTitle\n$shareUrl")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            errorToast("WhatsApp not installed")
        }
    }

}