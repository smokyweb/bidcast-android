package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
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
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.caneryilmaz.apps.luckywheel.constant.ArrowPosition
import com.caneryilmaz.apps.luckywheel.constant.TextOrientation
import com.caneryilmaz.apps.luckywheel.data.WheelData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.gyf.immersionbar.ktx.statusBarHeight
import io.agora.rtc2.Constants
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.LivePollOptionAdapter
import io.bidswipe.app.controller.LiveSellerAdapter
import io.bidswipe.app.controller.PollOptionAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.RandomizerEntriesAdapter
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
import io.bidswipe.app.databinding.ShowConfirmationAlertBinding
import io.bidswipe.app.databinding.ShowNotesSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.PollModel
import io.bidswipe.app.model.PollOptionModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.socket.AuctionStartedResponse
import io.bidswipe.app.network.response.socket.GetFreebieObject
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.PriceFormatter
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
import io.bidswipe.app.utils.share.ShareHelper
import io.bidswipe.app.utils.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private var promotePlans = mutableListOf<GetPromotePlansResponse.Data?>()
    private var livePollOptionList = mutableListOf<PollModel.PollOption>()
    private var liveSellerList = mutableListOf<GetLiveSellerResponse.Data?>()
    private var productList = mutableListOf<LiveShowModel.Product?>()
    private var pollOptionList = mutableListOf<PollOptionModel?>()
    private var commentList = mutableListOf<LiveChatModel?>()

    private lateinit var livePollAdapter: LivePollOptionAdapter
    private lateinit var pollOptionAdapter: PollOptionAdapter
    private lateinit var sellerAdapter: LiveSellerAdapter
    private lateinit var commentAdapter: CommentAdapter

    private val updateStatusHandler = Handler(Looper.getMainLooper())
    private var pollSheetBinding: PollDetailsSheetBinding? = null
    private lateinit var pipParams: PictureInPictureParams
    private var updateStatusRunnable: Runnable? = null
    private var socketManager: SocketManager? = null
    private var liveShowData: LiveShowModel? = null
    private var currentPoll: PollModel? = null

    private var showThumbnail: String? = null
    private var showTitle: String? = null
    private var channelName = ""
    private var agoraToken = ""
    private var socketUrl = ""
    private var showTime = ""
    private var showId = ""
    private var roomID = ""

    private var isShowLive = false
    private var isAuctionStarted = false
    private var isFreebieLive = false
    private var zoomLevel = 1.0f

    private var freebieUsers = mutableListOf<GetFreebieObject.Users?>()
    private var liveUsersList = mutableListOf<GetFreebieObject.Users?>()
    private var randomizerSheetBind: RandomizerSheetBinding? = null
    private var showNotes: String? = ""
    private var tipMessage: String? = ""
    private var tipChatEnabled: Boolean? = false
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var clipSheetBind: CreateClipSheetBinding
    private lateinit var clipSheet: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            supportActionBar(false)
            keyboardEnable(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                insets
            }
        } else {
            // For Android 14 and below
//			window.statusBarColor = color
        }

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->

            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            /*  bind.profileLayout.setMargins(
                  left = resources.dpToPx(16),
                  right = resources.dpToPx(16)
              )
  */
            bind.controlsView.setPadding(
                resources.dpToPx(0),
                system.top,
                resources.dpToPx(0),
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
        log("SHOW DATA ON PUBLISH : $liveShowData")

        exoPlayer = ExoPlayer.Builder(this).build()

        clipSheetBind = CreateClipSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.create_clip_sheet,
                null,
                false
            )
        )
        clipSheet = Alerts.appBottomSheet(this, true, clipSheetBind)

        showId = liveShowData?.showId ?: ""

        viewModel.showId = showId
        showTime = intent.getStringExtra("time") ?: ""

        showThumbnail = liveShowData?.thumbnail
        showTitle = liveShowData?.showDetail

//		val userId = intent.getStringExtra("userId") ?: ""

        roomID = "live_room_${userId}_${showId}"

        viewModel.currentRoomId = roomID
        viewModel.categoryId = liveShowData?.categoryId ?: ""

        bind.hostName.text = userName.asCapital()
        bind.hostImage.loadUrl(this, userImage)

        App.manager = AgoraManager(this, Const.APP_ID_AGORA)

        bind.loader.isVisible = true

        viewModel.getAgoraToken(roomID.request())

        commentAdapter = CommentAdapter(commentList, userId, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

                if (commentList[pos]?.userId == userId) return

                startActivity(
                    Intent(
                        this@AgoraPublisherActivity,
                        SellerProfileActivity::class.java
                    ).putExtra("sellerId", commentList[pos]?.userId)
                )
            }
        })

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

        bind.view2.setHapticClickListener {
            hideKeyboard()
        }

        bind.clip.isVisible = App.profileResponse.value?.preferences?.enableClips == true

        bind.message.setEndIconOnClickListener {
            if (!isShowLive) {
                Alerts.error(this, "Please start live show to send message")
                bind.messageText.setText("")
                return@setEndIconOnClickListener
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
        }

        bind.messageText.setOnEditorActionListener { _, actionId, _ ->
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
            if (imeVisible) {
                bind.product.isVisible = false
                bind.startBtn.isVisible = false
                bind.menuLayout.isVisible = false
            } else {
                bind.startBtn.isVisible = !isShowLive

                bind.product.isVisible = isAuctionStarted
                bind.menuLayout.isVisible = true
            }
            insets
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
            clipSheetBind.loaderView.isVisible = true
            clipSheetBind.videoView.isVisible = false
            viewModel.getClip(roomID.request())
            createClipSheet()
        }

        bind.share.setHapticClickListener {

            val shareText = buildString {
                append(Const.BASE_URL)
                append("/live-show?roomId=$roomID")
            }


            log("THUMBNAIL : $showThumbnail  TITLE : $showTitle")

            ShareHelper.openShareSheet(
                this.supportFragmentManager,
                imageUrl = showThumbnail,
                text = showTitle,
                sellerInfo = null,
                shareText = shareText,
                type = "show",
                isLive = isShowLive
            )
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

        bind.freebieLayout.setHapticClickListener {
            if (isShowLive) {
                if (isFreebieLive) showRandomizerSheet() else showFreebieStartSheet()
            } else {
                Alerts.error(this, "Please start live show to access this feature")
            }
        }

        bind.runNext.setHapticClickListener {
            if (isShowLive) {
                socketManager?.runNextProduct(roomID)
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
                        liveSellerList.clear()
                        liveSellerList.addAll(dataList)
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

        socketListeners()


        viewModel.getClipRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    clipSheetBind.loaderView.isVisible = false
                    viewModel.getClipRepo.value = null
                    val mData = it.value.data
                    log("GOT ${it.value.data}")

                    if (mData != null) {
                        runOnUiThread {
                            clipSheetBind.videoView.player = exoPlayer
                            val mediaItem = MediaItem.fromUri(mData.clipUrl ?: "")

                            exoPlayer.setMediaItem(mediaItem)
                            clipSheetBind.videoView.isVisible = true
                            clipSheetBind.bottomLayout.isVisible = true
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        }

                    } else {
                        errorToast("Something went wrong")
                        clipSheet.dismiss()
                    }
                }

                is Resource.Error -> {
                    clipSheetBind.loaderView.isVisible = false
                    viewModel.getClipRepo.value = null
                    clipSheet.dismiss()
                    errorToast(it.errorResponse?.message ?: "Something went wrong")
                }

                else -> {}

            }
        }

        bind.closeWheel.setOnClickListener {
            Alerts.success(this, "Wheel closed")
            bind.luckyWheelLayout.isVisible = false
            randomizerSheetBind?.hideWheel?.isVisible = false
            randomizerSheetBind?.showSpin?.isVisible = true
            bind.showNotes.isVisible = true
            bind.freebieLayout.isVisible = true
        }
    }

    fun socketListeners() {
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

        socketManager?.onAuctionStarted { json ->
            runSafe {
                if (json.roomId == roomID) {
                    runOnUiThread {
                        if (json.product != null) {
                            isAuctionStarted = true
                            bind.runNext.isVisible = json.status == "sold"
                            updateProductUI(json)
                        } else {
                            isAuctionStarted = false
                            updateProductUI(null)
                        }
                    }
                }
            }
        }

        socketManager?.onAuctionNExtProduct { json ->
            runSafe {
                if (json.optString("room_id") == roomID) {
                    runOnUiThread {
                        if (json.has("product") && json.optJSONObject("product") != null) {
                            val product = LiveShowModel.Product.fromJson(json.optJSONObject("product"))
                            if (liveShowData?.auctionTypeId == AuctionType.BUY_NOW.id) {
                                socketManager?.startAuction(
                                    viewModel.currentRoomId,
                                    listOf(product.id.toString()),
                                    product.price.toString(),
                                    null,
                                    null,
                                    null,
                                    liveShowData?.auctionTypeId
                                )
                            } else {
                                auctionSettingsSheet(product.id.toString(), product.price.toString())
                            }
                        } else {
                            showProductSheet()
                        }
                    }
                }
            }
        }

        socketManager?.onNextProductError { json ->
            runSafe {
                if (json.optString("room_id") == roomID) {
                    runOnUiThread {
                        showProductSheet()
                        log("NEXT PRODUCT ERROR : ${json.optString("message")}")
                    }
                }
            }
        }

        socketManager?.getFreebie { obj ->
            runOnUiThread {
                val res = Gson().fromJson(obj.toString(), GetFreebieObject::class.java)
                if (res.freebie?.roomId == roomID) {
                    isFreebieLive = true
                    bind.freebieEntryCount.text = "${res.usersList?.size ?: 0} Entries"

                    freebieUsers.clear()
                    freebieUsers.addAll(res.usersList ?: mutableListOf())

                    if (freebieUsers.isEmpty()) {
                        randomizerSheetBind?.recycler?.isVisible = false
                        randomizerSheetBind?.noEntries?.isVisible = true
                    } else {
                        randomizerSheetBind?.recycler?.isVisible = true
                        randomizerSheetBind?.noEntries?.isVisible = false
                    }

                    randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()
                }
            }
        }

        socketManager?.getLiveUsers { obj ->
            runOnUiThread {
                if (obj.optString("room_id") == roomID) {

                    val userList = obj.getJSONArray("users").let { array ->
                        (0 until array.length()).map { i ->
                            array.optJSONObject(i)?.let { GetFreebieObject.Users.fromJson(it) }
                        }
                    } ?: emptyList()

                    liveUsersList.clear()

                    liveUsersList.addAll(userList)

                    log("LIVE USERS : ${liveUsersList}")

                }
            }

        }

        socketManager?.getFreebieWinner { obj ->

            runOnUiThread {
                log("winner $obj")

                if (obj.optString("room_id") == roomID) {
                    log("winner2 $obj")
                    isFreebieLive = false

                    // Show wheel and controls
                    bind.luckyWheelLayout.isVisible = true
                    randomizerSheetBind?.hideWheel?.isVisible = true
                    randomizerSheetBind?.showSpin?.isVisible = false

                    bind.showNotes.isVisible = false
                    bind.freebieLayout.isVisible = false


                    val user = GetFreebieObject.Users.fromJson(obj.optJSONObject("user"))

                    val index = freebieUsers.indexOf(freebieUsers.find { it?.id == user.id })
                    log("winner4 $freebieUsers \n $user \n $index")

                    if (index != -1) {
                        log("winner3 $obj")
                        bind.luckyWheel.setTarget(index)
                        bind.luckyWheel.rotateWheel()
                    }
                }

            }

        }

        socketManager?.receiveShowNotes { args ->
            runSafe {
                if (args.optString("room_id") == roomID) {
                    showNotes = args.optString("show_note") ?: ""
                }
            }
        }

        socketManager?.onSaveTipSettingResult { obj ->
            runOnUiThread {
                log("Message : ${obj} ")
                if (obj.optString("show_id") == viewModel.showId) {
                    tipMessage = obj.optString("tip_message")
                    tipChatEnabled = obj.optBoolean("show_in_live_chat")
                }
            }
        }
    }

    override fun onDestroy() {
        App.manager.destroyEngine()

        isShowLive = false

        // Socket cleanup
        runSafe {

            viewModel.pinnedProducts.clear()

            if (currentPoll != null) {
                socketManager?.endPoll(roomID, currentPoll?.pollId.toString())
            }

            socketManager?.emitEndRoom(roomID)
            socketManager?.leaveRoom(roomID, userId)
//            socketManager?.disconnect()
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
            initPip()
            App.manager.joinChannel(agoraToken, channelName)

            bind.startBtn.isVisible = false

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

        socketManager?.onRoomCreated { showData ->
            runSafe {
                if (showData.roomId == roomID) {
                    productList.clear()

                    productList.addAll(showData.products)

                    showData.products.find { it?.isCurrent == true }

//					updateProductUI(liveProduct, liveProduct?.price)

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

                        val bidderName = winner.optString("user_name") ?: ""
                        val bidderImage = winner.optString("user_image")

                        if (bidderName.isNotEmpty()) {

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

                            socketManager?.sendMessage(
                                roomID,
                                "We have a winner! ${bidderName}",
                                userId,
                                userName,
                                userImage
                            )

                            product?.status = "sold"
                            product?.isCurrent = false

                            bind.status.isVisible = true
                            bind.bidPrice.isVisible = false
                            bind.runNext.isVisible = true

                        } else {
                            bind.winningLayout.isVisible = false
                            bind.status.isVisible = false
                            bind.runNext.isVisible = true
                        }

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
                        products.products.find { it?.isCurrent == true }
//						updateProductUI(currentProduct, currentProduct?.price )
//						productAdapter.notifyDataSetChanged()
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
        val bottomSheetFragment = ProductsForLiveShowFragment().apply {
            arguments = bundleOf("from" to "live_show", "auction_type_id" to liveShowData?.auctionTypeId)
        }
        bottomSheetFragment.show(supportFragmentManager, "BOTTOM_SHEET_TAG")
    }

    private fun showFreebieStartSheet() {
        val bottomSheetFragment = ProductsForLiveShowFragment().apply {
            arguments = bundleOf("from" to "freebie", "auction_type_id" to liveShowData?.auctionTypeId)
        }
        bottomSheetFragment.show(supportFragmentManager, "BOTTOM_SHEET_TAG")
    }

    fun updateProductUI(auctionData: AuctionStartedResponse?) {

        runOnUiThread {
            val liveProduct = auctionData?.product
            if (liveProduct != null) {
                log("updateProductUI : $liveProduct")
                bind.product.isVisible = true
                bind.productLayout.isVisible = true
                bind.productName.text = liveProduct.title?.asCapital()
                bind.productCategory.text = liveProduct.category?.name?.asCapital()
                bind.quantity.text = buildString {
                    append("Quantity: ")
                    append(liveProduct.quantity ?: 0)
                }

                val image = liveProduct.images?.firstOrNull() ?: ""
                if (image.isNotEmpty()) {
                    if (image.contains(Const.BASE_URL)) {
                        bind.productImage.loadUrl(this, image)
                        bind.productImageShop.loadUrl(this, image)
                    } else {
                        bind.productImage.loadUrl(this, "${Const.BASE_URL + "/"}${image}")
                        bind.productImageShop.loadUrl(this, "${Const.BASE_URL + "/"}${image}")
                    }
                }

                if (auctionData.auctionTypeId == AuctionType.LIVE.id) {
                    if (auctionData.suddenDeath == true) {
                        bind.bidTime.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.skull,
                            0,
                            0,
                            0
                        )
                    } else {
                        bind.bidTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                }

                bind.price.text = (liveProduct.pricing ?: "0.0").asMoney() + " + Shipping + Taxes"

                val price = auctionData.startingBidAmount
                bind.bidPrice.isVisible = auctionData.auctionTypeId == AuctionType.LIVE.id
                bind.bidPrice.text = price?.asMoney()
                bind.status.isVisible = auctionData.status=="sold"

            } else {
                bind.product.isVisible = false
                bind.productLayout.isVisible = false
            }
        }
    }

    private fun updateCountdown(json: JSONObject) {
        val value = json.optString("remaining")
        runSafe {
            this.runOnUiThread {
                if (json.optString("room_id") == roomID) {
                    bind.bidTime.isVisible = value.toInt() != 0
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
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            bind.profileLayout.isVisible = false
            bind.rehearsalLayout.isVisible = false
            bind.recycler.isVisible = false
            bind.menuLayout.isVisible = false
            bind.message.isVisible = false
            bind.product.isVisible = false
            bind.showNotes.isVisible = false
            bind.freebieLayout.isVisible = false
            bind.controls.isVisible = false
            App.PIPMode = true
        } else {
            bind.profileLayout.isVisible = true
            bind.rehearsalLayout.isVisible = true
            bind.recycler.isVisible = true
            bind.menuLayout.isVisible = true
            bind.message.isVisible = true
            bind.product.isVisible = false
            bind.showNotes.isVisible = true
            bind.freebieLayout.isVisible = true
            bind.controls.isVisible = true
            App.PIPMode = false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        log("USER LEAVE HINT")

        if (!isInPictureInPictureMode && this::pipParams.isInitialized) {
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
        if (isFreebieLive) {

            AppBottomSheet(
                this,
                R.drawable.ic_info,
                "Freebie Live",
                "A freebie is currently running. Ending the show will stop the freebie. Are you sure you want to continue?",
                primaryBtnText = "Okay",
                secondaryBtnText = "Cancel",
                canCancel = true,
                showSecondary = true,
                iconPadding = 16,
                alertType = AlertType.INFO,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
                        App.manager.destroyEngine()
                        dialog.dismiss()
                        finishAfterTransition()
                    }

                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }
                }
            ).show()
        } else {

            val endShowSheetBind = EndShowSheetBinding.bind(
                layoutInflater.inflate(
                    R.layout.end_show_sheet,
                    null,
                    false
                )
            )

            val sheet = Alerts.appBottomSheet(this, true, endShowSheetBind)

            endShowSheetBind.close.setHapticClickListener { sheet.dismiss() }

            endShowSheetBind.endBtn.setHapticClickListener {
                if (isFreebieLive) {
                    errorToast("Show cannot be ended until freebie is over")
                } else {
                    socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
                    App.manager.destroyEngine()
                    sheet.dismiss()

                    finishAfterTransition()
                }
            }
            sheet.show()
        }
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
        tipSettingsSheetBind.showLiveChat.isChecked = tipChatEnabled == true
        tipSettingsSheetBind.tipMessage.setText(tipMessage)

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

    private fun auctionSettingsSheet(productId: String, price: String) {
        var selectedCounterTimer = 5
        var selectedRequiredTime = 30

        val auctionSettingsSheetBind = AuctionSettingsSheetBinding.bind(
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

        auctionSettingsSheetBind.startingBid.addTextChangedListener(
            PriceFormatter(
                auctionSettingsSheetBind.startingBid
            )
        )

        val requiredTimeList = listOf(15, 30, 45)
        val requiredTimeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            requiredTimeList
        )

        auctionSettingsSheetBind.requiredTime.setAdapter(requiredTimeAdapter)

        auctionSettingsSheetBind.requiredTime.setText("30s", false)

        auctionSettingsSheetBind.requiredTime.setOnItemClickListener { _, _, position, _ ->
            selectedRequiredTime = requiredTimeList[position]
            auctionSettingsSheetBind.requiredTime.setText("${requiredTimeList[position]}s", false)
        }

        auctionSettingsSheetBind.requiredTime.setHapticClickListener {
            auctionSettingsSheetBind.requiredTime.showDropDown()
        }

        auctionSettingsSheetBind.startingBid.setText(price)

        auctionSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }
        auctionSettingsSheetBind.start.setHapticClickListener {

            when {
                selectedRequiredTime == 0 -> {
                    Alerts.error(this, "Please select required time")
                    return@setHapticClickListener
                }

                selectedCounterTimer == 0 -> {
                    Alerts.error(this, "Please select counter timer")
                    return@setHapticClickListener
                }

                auctionSettingsSheetBind.startingBid.value().isEmpty() -> {
                    Alerts.error(this, "Please enter starting bid")
                    return@setHapticClickListener
                }

                else -> {
                    val productIds = mutableListOf<String>()
                    productIds.add(productId)

                    socketManager?.startAuction(
                        viewModel.currentRoomId,
                        productIds,
                        auctionSettingsSheetBind.startingBid.value(),
                        selectedRequiredTime,
                        selectedCounterTimer,
                        auctionSettingsSheetBind.suddenDeath.isChecked,
                        liveShowData?.auctionTypeId
                    )
                    sheet.dismiss()

                }

            }

//             auctionSettingsSheetBind.startingBid.value()
//             selectedRequiredTime
//             selectedCounterTimer
//             auctionSettingsSheetBind.suddenDeath.isChecked
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

        showNotesSheetBind.showNotes.fromHtml(showNotes ?: "")

        showNotesSheetBind.close.setHapticClickListener {
            sheet.dismiss()
            bind.showNotes.isVisible = true
        }

        showNotesSheetBind.post.setHapticClickListener {
            val notes = showNotesSheetBind.showNotes.toFormattedHtml()
            if (notes.isEmpty()) {
                errorToast("Please enter some notes")
            } else {
                showNotes = notes
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

        sellerAdapter = LiveSellerAdapter(liveSellerList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                selectedItem = liveSellerList[pos]
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
                    socketManager?.setPromotionData(
                        userId,
                        showId,
                        promotePlans[pos]?.id.toString()
                    )
                }
            })

        promoteSheetBind.close.setHapticClickListener {
            promoteSheet.dismiss()
        }

        promoteSheet.show()
    }

    fun createClipSheet() {

        clipSheetBind.close.setHapticClickListener {
            clipSheet.dismiss()
        }

        clipSheet.show()

        clipSheet.setOnDismissListener {
            exoPlayer.release()
        }

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
            if (pollOptionList.size < 5) {
                pollOptionList.add(
                    PollOptionModel(
                        title = "Option ${pollOptionList.size + 1}",
                        hint = "Enter your option"
                    )
                )
                pollSheetBind.pollOptions.adapter?.notifyItemInserted(pollOptionList.size - 1)
            } else {
                errorToast("Maximum 5 options allowed")
            }
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
                Alerts.error(this, "Please add at least 3 options")
                return@setHapticClickListener
            }

            if (options.size < 3) {
                Alerts.error(this, "Please add at least 3 options")
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

    fun setUpWheel(options: List<GetFreebieObject.Users?>) {
        if (options.isEmpty()) return

        val colors = listOf(
            "#B28704".toColorInt(), // Amber
            "#388E3C".toColorInt(), // Green
            "#1976D2".toColorInt(), // Blue
            "#6A1B9A".toColorInt(), // Purple
            "#D32F2F".toColorInt(), // Red
            "#0097A7".toColorInt(), // Cyan
            "#C2185B".toColorInt(), // Pink
            "#F57C00".toColorInt(), // Orange
            "#303F9F".toColorInt(), // Indigo
            "#689F38".toColorInt(), // Light Green
            "#00796B".toColorInt(), // Teal
            "#512DA8".toColorInt()  // Deep Purple
        )

        val wheelData = ArrayList(
            options.mapIndexed { index, rawText ->
                WheelData(
                    text = rawText?.userName?.trim().toString(),
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
            setTextOrientation(TextOrientation.VERTICAL_TO_CENTER)
            setRotationCompleteListener {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(2000)
                    runOnUiThread {
                        bind.luckyWheelLayout.isVisible = false
                        bind.freebieEntryCount.text = "0 Entries"
                        bind.showNotes.isVisible = true
                        bind.freebieLayout.isVisible = true
                        freebieUsers.clear()
                        randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        bind.centerOfWheel.setOnClickListener {
//            bind.luckyWheel.rotateWheel()
            if (freebieUsers.isEmpty()) {
                Alerts.error(this, "Please enter entries to spin wheel")
                return@setOnClickListener
            }

            socketManager?.finalizeFreebie(roomID)
        }
    }

    fun showRandomizerSheet() {

        randomizerSheetBind = RandomizerSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.randomizer_sheet,
                null,
                false
            )
        )

        val randomSheet = Alerts.appBottomSheet(this, true, randomizerSheetBind!!)

        if (bind.luckyWheelLayout.isVisible) {
            randomizerSheetBind?.hideWheel?.isVisible = true
            randomizerSheetBind?.showSpin?.isVisible = false
        } else {
            randomizerSheetBind?.hideWheel?.visibility = View.GONE
            randomizerSheetBind?.showSpin?.isVisible = true
        }

        /*var currentEntries = mutableListOf<String>()

        currentEntries.addAll(freebieUsers.map { it?.name ?: "" })*/

        randomizerSheetBind?.recycler?.adapter =
            RandomizerEntriesAdapter(freebieUsers, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {

                    socketManager?.removeFreebieUser(roomID, freebieUsers[pos]?.id.toString())

                    /*freebieUsers.removeAt(pos)
                    randomizerSheetBind?.recycler?.adapter?.notifyItemRemoved(pos)*/
                }
            })

        if (freebieUsers.isEmpty()) {
            randomizerSheetBind?.recycler?.isVisible = false
            randomizerSheetBind?.noEntries?.isVisible = true
        } else {
            randomizerSheetBind?.recycler?.isVisible = true
            randomizerSheetBind?.noEntries?.isVisible = false
        }

        if (freebieUsers.isNotEmpty()) {
            setUpWheel(freebieUsers)
        }

        randomizerSheetBind?.close?.setHapticClickListener {
            randomSheet.dismiss()
        }

        randomSheet.show()

        randomizerSheetBind?.showSpin?.setHapticClickListener {

            if (freebieUsers.isEmpty()) {
                errorToast("Please enter entries to show spin wheel")
                return@setHapticClickListener
            }

            // Show wheel and controls
            bind.luckyWheelLayout.isVisible = true
            randomizerSheetBind?.hideWheel?.isVisible = true
            randomizerSheetBind?.showSpin?.isVisible = false

            bind.showNotes.isVisible = false
            bind.freebieLayout.isVisible = false
        }

        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            liveUsersList.map { it?.userName })

        randomizerSheetBind?.manualEntry?.setAdapter(arrayAdapter)

        randomizerSheetBind?.manualEntry?.setOnItemClickListener { parent, _, position, _ ->

            val selectedUser = parent.getItemAtPosition(position)

            val uId = liveUsersList.find { it?.userName == selectedUser }?.id

            log("SELECTED USER: $uId")

            socketManager?.enterInFreebie(roomID, uId.toString())

            randomizerSheetBind?.manualEntry?.setText("", false)

            Toast.makeText(this, "Selected: ${uId}", Toast.LENGTH_SHORT).show()

        }

        // Hide wheel
        randomizerSheetBind?.hideWheel?.setHapticClickListener {
            bind.luckyWheelLayout.isVisible = false
            randomizerSheetBind?.hideWheel?.visibility = View.GONE
            randomizerSheetBind?.showSpin?.visibility = View.VISIBLE
            bind.showNotes.isVisible = true
            bind.freebieLayout.isVisible = true
        }

        randomizerSheetBind?.shuffleEntries?.setHapticClickListener {
            if (freebieUsers.isEmpty()) {
                Alerts.error(this, "Please enter entries to shuffle")
                return@setHapticClickListener
            }

            freebieUsers.shuffle()

//			currentEntries.shuffle()

            randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()

//			randomizerSheetBind.manualEntry.setText(currentEntries.joinToString("\n"))

            setUpWheel(freebieUsers)
        }

        randomizerSheetBind?.removeAll?.setOnClickListener {
            freebieUsers.clear()
            randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()
            setUpWheel(freebieUsers)
        }

        // Spin the wheel
        randomizerSheetBind?.spinWheel?.setOnClickListener {
            if (freebieUsers.isEmpty()) {
                Alerts.error(this, "Please enter entries to spin wheel")
                return@setOnClickListener
            }

            socketManager?.finalizeFreebie(roomID)

            randomSheet.dismiss()
//			bind.luckyWheel.rotateWheel()
        }


    }

}