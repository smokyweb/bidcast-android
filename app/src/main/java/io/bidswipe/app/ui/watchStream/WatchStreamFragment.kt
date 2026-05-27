package io.bidswipe.app.ui.watchStream

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.gyf.immersionbar.ktx.statusBarHeight
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
import io.bidswipe.app.databinding.CreateClipSheetBinding
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
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetReportCategoriesResponse
import io.bidswipe.app.network.response.SellerInfoResponse
import io.bidswipe.app.network.response.socket.AuctionStartedBreakSpotResponse
import io.bidswipe.app.network.response.socket.AuctionStartedResponse
import io.bidswipe.app.network.response.socket.GetFreebieObject
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.product.ProductSetDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.clr
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
import io.bidswipe.app.utils.toEpochMillis
import io.bidswipe.app.utils.value
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit
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

    private var showId: String? = ""
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
    private var surpriseSetAuctionRunning = false
    private var isAuctionStarted = false
    private var showThumbnail: String? = null

    private var showNotes: String? = ""

    // Basecamp #9933402746 (2026-05-27): track the currently-shown show-notes
    // sheet so receiveShowNotes can live-refresh its content when the seller
    // edits notes mid-stream. Previously the sheet read showNotes once on
    // open and never updated.
    private var openShowNotesBinding: io.bidswipe.app.databinding.ViewerShowNotesSheetBinding? = null

    private var freebieUsers = mutableListOf<GetFreebieObject.Users?>()
    private var breakSpotUsers = mutableListOf<Pair<Int, String>?>()

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var clipSheetBind: CreateClipSheetBinding
    private lateinit var clipSheet: BottomSheetDialog
    private var liveEndedSheet: AppBottomSheet? = null

    private var liveShowData: LiveShowModel? = null
    private var breakSpotAuctionData: AuctionStartedBreakSpotResponse? = null

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
        showThumbnail = requireArguments().getString("thumbnail") ?: ""
        socketUrl = Const.SOCKET_URL
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log("RoomId: $roomID")
        log("StreamToken: $streamID")

        setUpSwipe()

        initPip()

        // Initialize thumbnail view - show it initially
        bind.thumbnailView.loadUrl(mCtx, showThumbnail ?: "", R.drawable.placeholder_rect)

        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            bind.profileLayout.setMargins(
                top = system.top,
                left = resources.dpToPx(16),
                right = resources.dpToPx(16)
            )

            bind.bidLayout.setMargins(resources.dpToPx(16), 0, resources.dpToPx(16), system.bottom)

            insets
        }

        exoPlayer = ExoPlayer.Builder(mCtx).build()

        clipSheetBind = CreateClipSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.create_clip_sheet,
                null,
                false
            )
        )

        clipSheet = Alerts.appBottomSheet(mCtx, true, clipSheetBind)

        bind.cutButton.setHapticClickListener {
            finish()
        }

        bind.clip.setHapticClickListener {
            // Basecamp #9929851737 (2026-05-27): show duration picker FIRST,
            // defer the API call until the buyer taps "Create Clip". Per
            // Trey's redux spec, default to 30s and let the user drag to
            // pick anywhere in 1..60s.
            clipSheetBind.durationPickerWrap.isVisible = true
            clipSheetBind.loaderView.isVisible = false
            clipSheetBind.videoView.isVisible = false
            clipSheetBind.bottomLayout.isVisible = false
            clipSheetBind.durationSlider.value = 30f
            clipSheetBind.durationValueText.text = "30s"
            createClipSheet()
        }

        bind.iconCard.setHapticClickListener {
            if (sellerId?.isNotEmpty() == true) {
                bind.loader.isVisible = true
                viewModel.getSellerInfo(sellerId!!)
            }
        }

        bind.userName.setHapticClickListener {
            if (sellerId?.isNotEmpty() == true) {
                bind.loader.isVisible = true
                viewModel.getSellerInfo(sellerId!!)
            }
        }

        bind.recycler.setOnTouchListener { view, _ ->
            hideKeyboard(view)
            return@setOnTouchListener false
        }

        bind.showNotes.setHapticClickListener {
            showNotesSheet()
            bind.showNotes.isVisible = false
        }

        bind.freebieLayout.setHapticClickListener {
            bind.notesFreebieLayout.isVisible = false
            bind.freebieEntryLayout.isVisible = true
            bind.enterFreebie.text = if (isFollowing) "Enter Freebie" else "Follow Host & Enter Freebie"
        }

        bind.enterFreebie.setHapticClickListener {
            if (isFollowing) {
                // Freebie Entry event
                bind.freebieEntryLayout.isVisible = false
                bind.notesFreebieLayout.isVisible = true

                if (freebieUsers.find { it?.id.toString() == userId } != null) {
                    Alerts.error(mCtx, "You have already entered the freebie")

                } else {
                    socketManager?.enterInFreebie(roomID, userId)
                }

            } else {
                bind.loader.isVisible = true
                viewModel.followUser(sellerId?.request(), showId.toString().request())
            }
        }

        bind.closeFreebie.setHapticClickListener {
            bind.notesFreebieLayout.isVisible = true
            bind.freebieEntryLayout.isVisible = false
        }

        commentAdapter = CommentAdapter(commentList, roomID.split("_")[2], object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

                if (commentList[pos]?.userId == userId) return

                startActivity(Intent(mCtx, SellerProfileActivity::class.java).putExtra("sellerId", commentList[pos]?.userId))
            }
        })

        livePollAdapter = LivePollOptionAdapter(livePollOptionList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                voteOnPoll(pos)
            }
        })

        bind.recycler.adapter = commentAdapter

        if (socketUrl.isNotEmpty()) {
            socketManager = App.socketManager
            socketManager?.joinRoom(roomID, userId) {
            }

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

            socketManager?.getHighestBidBreakSpot { json ->
                handleBidUpdate(json)
                runSafe {
                    requireActivity().runOnUiThread {
                        val highestBid = json.getJSONObject("get_highest_bid")
                        val bidderName = highestBid.optString("user_name")
                        if (breakSpotUsers.find { it?.first == highestBid.optInt("user_id") } == null) breakSpotUsers.add(
                            Pair(
                                highestBid.optInt("user_id"),
                                bidderName
                            )
                        )
                    }
                }
            }

            socketManager?.onAuctionStarted { auctionData ->
                runSafe {
                    if (auctionData.roomId == roomID) {
                        requireActivity().runOnUiThread {
                            if (liveEndedSheet?.isShowing == true) {
                                liveEndedSheet?.dismiss()
                                liveEndedSheet = null
                            }
                            // Auction-start event means the bidding area should be available.
                            // Product payload can be delayed/null in some socket emissions.
                            isAuctionStarted = true
                            surpriseSetAuctionRunning = false
                            updateProductUI(auctionData)
                        }
                    }
                }
            }

            socketManager?.onAuctionStartedBreakSpot { auctionData ->
                runSafe {
                    if (auctionData.roomId == roomID) {
                        requireActivity().runOnUiThread {
                            isAuctionStarted = auctionData.surpriseSetDetails != null
                            breakSpotAuctionData = auctionData
                            surpriseSetAuctionRunning = true
                            updateBreakSpotProductUI(auctionData)
                        }
                    }
                }
            }

            socketManager?.getUpdatedProduct { json ->
                runSafe {
                    requireActivity().runOnUiThread {
                        if (json.optString("room_id") == roomID) {
                            updateCurrentProductFromRoomState(LiveShowModel.fromJson(json))
                        }
                    }
                }
            }

            socketManager?.getBidFinalize { json ->
                finalizeBidUpdateUI(json)
            }

            socketManager?.getBidFinalizeBreakSpot { json ->
                finalizeBidUpdateUI(json,false)
                runSafe {
                    requireActivity().runOnUiThread {
                        breakSpotAuctionData?.surpriseSetDetails?.soldQuantity = (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity ?: 0) + 1

                        bind.itemsLeftProgress.isVisible = true
                        bind.itemsLeftProgress.max = breakSpotAuctionData?.surpriseSetDetails?.totalQuantity ?: 0
                        bind.itemsLeftProgress.progress = (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity ?: 0)

                        bind.quantity.text = buildString {
                            append(
                                (breakSpotAuctionData?.surpriseSetDetails?.totalQuantity
                                    ?: 0) - (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity ?: 0)
                            )
                            append("/")
                            append(breakSpotAuctionData?.surpriseSetDetails?.totalQuantity ?: 0)
                            append(" left")
                        }
                        val winner = json.getJSONObject("winner")
                        rotateBreakSpotText(winner.optString("user_id").toInt())
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
                log("END GOT WATCH FRAGMENT $json")
                runSafe {
                    requireActivity().runOnUiThread {
                        if (json.optString("room_end") == roomID) {
                            liveEndedSheet?.dismiss()
                            liveEndedSheet = AppBottomSheet(
                                mCtx,
                                R.drawable.ic_info,
                                "Live show ended!",
                                "The show $showTitle has ended.",
                                primaryBtnText = "Go Back",
                                secondaryBtnText = "Go Back",
                                canCancel = false,
                                showSecondary = false,
                                alertType = AlertType.INFO,
                                clicks = object : AlertClicks {
                                    override fun primaryClick(dialog: AppBottomSheet) {
                                        dialog.dismiss()
                                        App.manager.destroyEngine()
                                        activity?.setResult(Activity.RESULT_OK)
                                        finish()
                                    }

                                    override fun secondaryClick(dialog: AppBottomSheet) {
                                        dialog.dismiss()
                                    }
                                }

                            )
                            liveEndedSheet?.show()
                        }
                    }
                }
            }

            socketManager?.onRoomCreated { showData ->
                activity?.runOnUiThread {
                    liveShowData = showData
                    if (liveShowData?.roomId == roomID) {
                        if (liveEndedSheet?.isShowing == true) {
                            liveEndedSheet?.dismiss()
                            liveEndedSheet = null
                        }
                        if (streamID.isEmpty() || liveShowData?.rtcToken != streamID) {
                            streamID = liveShowData?.rtcToken ?: ""
                            if (streamID.isNotEmpty()) {
                                App.manager.joinSubscriberChannel(streamID, roomID)
                                currentRemoteUid?.let { uid ->
                                    setupRemoteVideo(uid)
                                }
                            }
                        }
                        updateSessionUI()
                    } else if (App.categoryList.filter { it?.isSelected == true }.findLast { it?.id.toString() == showData.categoryId } != null) {
                        viewModel.streamsList.value?.add(
                            StreamModel(
                                liveShowData?.roomId.toString(),
                                liveShowData?.rtcToken ?: "",
                                thumbnail = liveShowData?.thumbnail
                            )
                        )
                    }
                }
            }

            socketManager?.onFollowSellerStatus { obj ->
                activity?.runOnUiThread {
                    if (obj.optString("room_id") == roomID && obj.optString("user_id") == userId) {

                        log("IS FOLLOWING : ${obj.optString("is_followed")}")

                        isFollowing = obj.optBoolean("is_followed")

                        updateFollowUi()

                        followSheetRunnable = Runnable {
                            if (isFollowing) {
                                socketManager?.sustainWatches(userId, roomID)
                            } else {
                                followSheet()
                                socketManager?.sustainWatches(userId, roomID)
                            }
                        }

                        if (!isHandlerRunning) {
                            followSheetHandler.postDelayed(followSheetRunnable!!, 30000)
                            isHandlerRunning = true
                        }

                    }
                }
            }

            socketManager?.receiveRaid { obj ->
                try {
                    requireActivity().runOnUiThread {
                        if (obj.optString("source_room_id") == roomID) {
                            val message = obj.optString("message")
                            Alerts.success(mCtx, message)
                            val targetRoomId = obj.optString("target_room_id")
                            val rtcToken = obj.optString("rtcToken")
                            onRaid(targetRoomId, rtcToken)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            socketManager?.onVoteErrorResult { obj ->
                requireActivity().runOnUiThread {
                    if (obj.optString("room_id") == roomID) {
                    }
                }
            }

            socketManager?.receiveShowNotes { args ->
                runSafe {
                    if (args.optString("room_id") == roomID) {
                        requireActivity().runOnUiThread {
                            bind.showNotes.isVisible = true
                            showNotes = args.optString("show_note") ?: ""
                            // Basecamp #9933402746 (2026-05-27): if the buyer
                            // has the show-notes sheet open right now, push the
                            // new HTML into it so they see updates as the seller
                            // edits without having to close + reopen.
                            openShowNotesBinding?.notes?.setHtmlFromString(
                                showNotes?.ifEmpty { "No notes added yet." }, false
                            )
                        }
                    }
                }
            }

            socketManager?.onSaveTipSettingResult { obj ->
                requireActivity().runOnUiThread {
                    log("Message : ${obj.optString("tip_message")} ")
                }
            }

            socketManager?.getFreebie { obj ->
                requireActivity().runOnUiThread {
                    val res = Gson().fromJson(obj.toString(), GetFreebieObject::class.java)
                    if (res.freebie?.roomId == roomID) {

                        bind.freebieLayout.isVisible = true
                        bind.freebieEntryCount.text = "${res.usersList?.size ?: 0} Entries"
                        bind.freebieCount.text = "${res.usersList?.size ?: 0} Entries"
                        freebieUsers.clear()
                        freebieUsers.addAll(res.usersList ?: mutableListOf())
                        // Pre-populate wheel so it's ready when spinning starts
                        setupBuyerWheel()
                    }

                }
            }

            // freebie-spinning: server fires this before computing winner — start spin animation
            socketManager?.onFreebieSpinning { obj ->
                requireActivity().runOnUiThread {
                    if (obj.optString("room_id") == roomID) {
                        bind.winnerSpotLayout.isVisible = true
                        bind.winnerTitle.text = "Spinning…"
                        if (freebieUsers.isNotEmpty()) {
                            bind.buyerLuckyWheel.isVisible = true
                            bind.textSwitcher.isVisible = false
                            // Spin to a random slot (winner will snap it correctly on arrival)
                            bind.buyerLuckyWheel.setTarget(freebieUsers.indices.random())
                            bind.buyerLuckyWheel.rotateWheel()
                        }
                    }
                }
            }

            socketManager?.getFreebieWinner { obj ->

                requireActivity().runOnUiThread {

                    if (obj.optString("room_id") == roomID) {

                        val user = GetFreebieObject.Users.fromJson(obj.optJSONObject("user"))

                        bind.winnerSpotLayout.isVisible = true

                        if (bind.buyerLuckyWheel.isVisible && freebieUsers.isNotEmpty()) {
                            // Spin wheel to winning slot
                            val idx = freebieUsers.indexOfFirst { it?.id == user.id }
                            bind.winnerTitle.text = "The winner is…"
                            if (idx >= 0) {
                                bind.buyerLuckyWheel.setTarget(idx)
                                bind.buyerLuckyWheel.rotateWheel()
                            }
                            bind.buyerLuckyWheel.setRotationCompleteListener {
                                requireActivity().runOnUiThread {
                                    bind.winnerTitle.text = "🎉 ${user.name} won!"
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        bind.winnerSpotLayout.isVisible = false
                                        bind.buyerLuckyWheel.isVisible = false
                                        bind.textSwitcher.isVisible = true
                                        bind.notesFreebieLayout.isVisible = true
                                        bind.freebieLayout.isVisible = false
                                        freebieUsers.clear()
                                    }, 3000)
                                }
                            }
                        } else {
                            // Fallback: text animation
                            bind.buyerLuckyWheel.isVisible = false
                            bind.textSwitcher.isVisible = true
                            rotateText(user.id)
                        }

                        log("Freebie Winner : ${obj} ")

                    }

                }
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
                // Basecamp #9933877362 (2026-05-27): chat is unrestricted unless
                // the seller has gated this show to verified buyers only.
                if (liveShowData?.isVerifiedOnly != true ||
                    App.profileResponse.value?.buyerIdentityStatus == "verified") {
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
                    // Basecamp #9933877362 (2026-05-27): same relaxation as above.
                    if (liveShowData?.isVerifiedOnly != true ||
                        App.profileResponse.value?.buyerIdentityStatus == "verified") {
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
                bind.productAuctionBidLayout.isVisible = false
                bind.bidLayout.isVisible = false
                bind.sideOptions.isVisible = false
            } else {
                bind.productAuctionBidLayout.isVisible = isAuctionStarted
                bind.bidLayout.isVisible = isAuctionStarted
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
                text = showTitle,
                sellerInfo = null,
                shareText = shareText,
                type = "show",
                isLive = true
            )
        }

        bind.shareShow.setHapticClickListener {
            val shareText = buildString {
                append(Const.BASE_URL)
                append("/live-show?roomId=$roomID")
            }

            ShareHelper.openShareSheet(
                parentFragmentManager,
                imageUrl = showThumbnail,
                text = showTitle,
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

            App.isWatchStreamInPIP.value = true
            App.currentSellerId = sellerId
        }

        // Basecamp #9933877362 (2026-05-27): only block the viewer on join when
        // the show is gated to verified-buyers-only. For open shows (the new
        // default), do not pop the verification modal — the viewer can watch /
        // bid / tip / buy as long as they have a verified payment method, which
        // is checked separately at bid/buy time.
        if (liveShowData?.isVerifiedOnly == true &&
            App.profileResponse.value?.buyerIdentityStatus != "verified") {
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

                    // The follow-unfollow API toggles the relationship.
                    // Prefer the new state from the response; fall back to toggling locally.
                    isFollowing = it.value.data?.status ?: !isFollowing

                    updateFollowUi()

                    Alerts.success(
                        mCtx,
                        if (isFollowing) "Followed successfully" else "Unfollowed successfully"
                    )

                    if (isFollowing &&
                        bind.freebieEntryLayout.isVisible &&
                        freebieUsers.none { user -> user?.id.toString() == userId }
                    ) {
                        bind.freebieEntryLayout.isVisible = false
                        bind.notesFreebieLayout.isVisible = true
                        socketManager?.enterInFreebie(roomID, userId)
                    }

                    if (bind.freebieEntryLayout.isVisible && freebieUsers.none { it?.id.toString() == userId }) {
                        bind.freebieEntryLayout.isVisible = false
                        bind.notesFreebieLayout.isVisible = true
                        socketManager?.enterInFreebie(roomID, userId)
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

        viewModel.getClipRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    clipSheetBind.loaderView.isVisible = false
                    viewModel.getClipRepo.value = null
                    val mData = it.value.data
                    log("GOT ${it.value.data}")

                    if (mData != null) {
                        clipSheetBind.videoView.player = exoPlayer
                        val mediaItem = MediaItem.fromUri(mData.clipUrl ?: "")

                        exoPlayer.setMediaItem(mediaItem)
                        clipSheetBind.videoView.isVisible = true
                        clipSheetBind.bottomLayout.isVisible = true
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
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

    }


    override fun onResume() {
        super.onResume()

        attachAgoraCallbacks()

        socketManager?.joinRoom(roomID, userId) {
            socketManager?.joinShow(userId, roomID)
        }

        if (streamID.isBlank()) {
            log("Stream token missing – unable to join channel")
            return
        }

        App.manager.joinSubscriberChannel(streamID, roomID)
        currentRemoteUid?.let { uid ->
            setupRemoteVideo(uid)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        log("DESTRO CALLED")
        liveEndedSheet?.dismiss()
        liveEndedSheet = null
        socketManager?.leaveRoom(roomID, userId)
        App.manager.leaveChannel()
    }

    private fun attachAgoraCallbacks() {
        App.manager.onUserJoin = { uId, _ ->
            currentRemoteUid = uId
            log("ON USER JOIN $uId")
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
        bind.productLayout.isVisible = false
        // Hide thumbnail when video is ready
        hideThumbnail()
    }

    private fun clearRemoteVideo() {
        bind.hostView.removeAllViews()
        bind.productLayout.isVisible = false
        bind.soldLayout.isVisible = true
    }

    fun setUpSwipe() {

        var downX = 0f

        bind.viewFlipper.setOnTouchListener { _, event ->
            log("TOUCH FLIPPP")
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
            log("TOUCH CONTROLLS")
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

        bind.scrollviewLinear.setOnTouchListener { _, event ->
            log("TOUCH LINEAR")
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
        log("BID AMOUNT 1: $bidAmount")
        if (liveShowData?.auctionTypeId == AuctionType.BUY_NOW.id) {
            bind.bidTime.isVisible = false
            bind.bidPrice.isVisible = false
        } else {
            bind.bidTime.isVisible = true
            bind.bidPrice.isVisible = true
            bind.bidPrice.text = bidAmount?.asMoney()
            bind.bidButton.text = buildString {
                append("Bid : ")
                append(newBidAmount(bidAmount?.toDoubleOrNull()?.toInt() ?: 0).toString().asMoney())
                append(" >>")
            }
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

    private fun updateProductUI(auctionData: AuctionStartedResponse) {

        activity?.runOnUiThread {
            val liveProduct = auctionData.product
            log("LIVE AUCTZION TYPE ${liveShowData?.auctionTypeId}")
            if (liveProduct != null) {
                isAuctionStarted = true
                bind.winningLayout.isVisible = false
                bind.productName.text = liveProduct.title?.asCapital()

                bind.productCategory.text = liveProduct.category?.name?.asCapital()
                bind.quantity.text = buildString {
                    append("Quantity: ")
                    append(liveProduct.quantity ?: 0)
                }
                val image = liveProduct.images?.firstOrNull() ?: ""
                if (image.isNotEmpty()) {
                    if (image.contains(Const.BASE_URL)) {
                        bind.productImage.loadUrl(mCtx, image)
                        bind.productImageShop.loadUrl(mCtx, image)
                    } else {
                        bind.productImage.loadUrl(mCtx, "${Const.BASE_URL + "/"}${image}")
                        bind.productImageShop.loadUrl(mCtx, "${Const.BASE_URL + "/"}${image}")
                    }
                }
                bind.productImageCard.isVisible = true
                bind.itemsLeftProgress.isVisible = false

                val price = liveProduct.pricing ?: "0.0"
                bind.price.text = price.asMoney() + " + Shipping + Taxes"

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

                highestBidAmount = if (auctionData.startingBidAmount == "0" || auctionData.auctionTypeId == AuctionType.BUY_NOW.id) {
                    price
                } else {
                    auctionData.startingBidAmount
                }

                bidProductId = liveProduct.id.toString()

                setBidText(highestBidAmount)

                if (auctionData.status == "sold") {
                    bind.bidLayout.isVisible = false
                    bind.buyNowBtn.isVisible = false
                    bind.soldLayout.isVisible = true
                    bind.productLayout.isVisible = false
                    bind.status.isVisible = true
                } else {
                    bind.status.isVisible = false
                    bind.bidLayout.isVisible = auctionData.auctionTypeId == AuctionType.LIVE.id
                    bind.buyNowBtn.isVisible = auctionData.auctionTypeId == AuctionType.BUY_NOW.id
                    bind.soldLayout.isVisible = false
                    bind.productLayout.isVisible = true
                    bind.productAuctionBidLayout.isVisible = true
                }

                bind.productLayout.setHapticClickListener {
                    startActivity(
                        Intent(
                            mCtx,
                            ProductDetailsActivity::class.java
                        ).putExtra("productId", liveProduct.id.toString())
                    )
                }
            } else {
                isAuctionStarted = false
                bind.status.isVisible = true
                bind.bidLayout.isVisible = false
                bind.productLayout.isVisible = false
            }
        }

    }

    private fun updateCurrentProductFromRoomState(roomState: LiveShowModel) {
        val currentProduct = roomState.products.find { it?.isCurrent == true }
        productList.clear()
        productList.addAll(roomState.products)

        if (currentProduct != null) {
            // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitLab's
            // isAuctionStarted state tracking (used by other watch-stream logic).
            isAuctionStarted = true
            bind.winningLayout.isVisible = false
            bind.soldLayout.isVisible = false
            bind.status.isVisible = false
            bind.productLayout.isVisible = true
            bind.productAuctionBidLayout.isVisible = true
            bind.productName.text = currentProduct.name?.asCapital()
            bind.productCategory.text = currentProduct.category?.name?.asCapital()
            bind.quantity.text = buildString {
                append("Quantity: ")
                append(currentProduct.quantity ?: 0)
            }

            val image = currentProduct.image.orEmpty()
            if (image.isNotEmpty()) {
                if (image.contains(Const.BASE_URL)) {
                    bind.productImage.loadUrl(mCtx, image)
                    bind.productImageShop.loadUrl(mCtx, image)
                } else {
                    bind.productImage.loadUrl(mCtx, "${Const.BASE_URL + "/"}$image")
                    bind.productImageShop.loadUrl(mCtx, "${Const.BASE_URL + "/"}$image")
                }
            }

            bind.productImageCard.isVisible = true
            bind.itemsLeftProgress.isVisible = false

            val price = currentProduct.price ?: "0.0"
            bind.price.text = price.asMoney() + " + Shipping + Taxes"

            val auctionTypeId = roomState.auctionTypeId ?: liveShowData?.auctionTypeId
            val activeBid = roomState.highestBid.bidAmount?.takeIf { it.isNotEmpty() }
                ?: roomState.startingBidAmount?.toString()?.takeIf { it != "0.0" }
                ?: price

            highestBidAmount = activeBid
            bidProductId = currentProduct.id
            setBidText(highestBidAmount)

            bind.bidLayout.isVisible = auctionTypeId == AuctionType.LIVE.id
            bind.buyNowBtn.isVisible = auctionTypeId == AuctionType.BUY_NOW.id

            bind.productLayout.setHapticClickListener {
                startActivity(
                    Intent(
                        mCtx,
                        ProductDetailsActivity::class.java
                    ).putExtra("productId", currentProduct.id.toString())
                )
            }
        } else {
            isAuctionStarted = false
            bind.bidTime.isVisible = false
            bind.bidLayout.isVisible = false
            bind.buyNowBtn.isVisible = false
            bind.productLayout.isVisible = false
            bind.status.isVisible = true
        }
    }

    private fun updateBreakSpotProductUI(auctionData: AuctionStartedBreakSpotResponse) {

        activity?.runOnUiThread {
            val liveProduct = auctionData.surpriseSetDetails
            log("LIVE AUCTZION TYPE ${liveShowData?.auctionTypeId}")
            if (liveProduct != null) {
                bind.winningLayout.isVisible = false
                bind.productName.text = liveProduct.productSet?.name?.asCapital() + " #${auctionData.productSetItemUnitId}"

                bind.productCategory.text = liveProduct.productSet?.description

                bind.itemsLeftProgress.max = liveProduct?.totalQuantity ?: 0
                bind.itemsLeftProgress.progress = (liveProduct?.soldQuantity ?: 0)

                bind.quantity.isVisible = true
                bind.quantity.text = buildString {
                    append((liveProduct.totalQuantity ?: 0) - (liveProduct?.soldQuantity ?: 0))
                    append("/")
                    append(liveProduct.totalQuantity ?: 0)
                    append(" left")
                }

                bind.productImageCard.isVisible = false
                bind.itemsLeftProgress.isVisible = true

                val price =  if( liveProduct.productSet?.type == "buy_it_now"){
                   (liveProduct.productSet?.price ?: 0.0).toString()
                }else{
                    auctionData.startingBidAmount
                }

                 bind.price.text = "$price + Shipping + Taxes"

                log("BID AMOUNT 3 : $price  -- ${liveProduct.productSet?.price}")

                if (liveProduct.productSet?.type == "auction") {
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

                highestBidAmount = if (auctionData.startingBidAmount == "0" || liveProduct.productSet?.type == "buy_it_now") {
                    price
                } else {
                    auctionData.startingBidAmount
                }
                log("BID AMOUNT 2 : $highestBidAmount")

                bidProductId = liveProduct.productSet?.id.toString()

                setBidText(highestBidAmount)

                if (auctionData.status == "sold") {
                    bind.bidLayout.isVisible = false
                    bind.buyNowBtn.isVisible = false
                    bind.soldLayout.isVisible = true
                    bind.productLayout.isVisible = false
                    bind.status.isVisible = true
                } else {
                    bind.status.isVisible = false
                    bind.bidLayout.isVisible = liveProduct.productSet?.type == "auction"
                    bind.buyNowBtn.isVisible = liveProduct.productSet?.type == "buy_it_now"
                    bind.soldLayout.isVisible = false
                    bind.productLayout.isVisible = true
                    bind.productAuctionBidLayout.isVisible = true
                }

                bind.productLayout.setHapticClickListener {
                    log("CLICKED $surpriseSetAuctionRunning")
                    if(surpriseSetAuctionRunning){
                        startActivity(
                            Intent(
                                mCtx,
                                ProductSetDetailsActivity::class.java
                            ).putExtra("productSetId", breakSpotAuctionData?.productSetId.toString())
                        )
                    }
                }
            } else {
                bind.status.isVisible = true
                bind.bidLayout.isVisible = false
                bind.productLayout.isVisible = false
            }
        }
    }

    private fun updateSessionUI() {
        runSafe {
            if (liveShowData?.isLive == false) {
                bind.notLiveLayout.isVisible = true
                bind.bottomUI.isVisible = false
                bind.notesFreebieLayout.isVisible = false
                checkShowTime((liveShowData?.time?.toLong())?.toEpochMillis() ?: Utils.timestamp())
            } else {
                bind.notLiveLayout.isVisible = false
                bind.notesFreebieLayout.isVisible = true
                bind.bottomUI.isVisible = true
            }

            // Mark socket data as loaded and show thumbnail if available
            isSocketDataLoaded = true

            productList.clear()
            productList.addAll(liveShowData?.products ?: emptyList())
            bind.countBadge.isVisible = true
            bind.countBadge.text = productList.size.toString()

            liveShowData?.products?.find { it?.isCurrent == true }

            // Basecamp #9933877362 (2026-05-27): the seller's per-show
            // verified-buyers-only toggle overrides allowBidForAll. When the
            // toggle is ON, bidding / tipping / buying require identity
            // verification; when OFF, the existing allowBidForAll value from the
            // server applies (defaulting to true — the new "open to all viewers
            // with a verified payment method" model).
            isAllowBidForAll = if (liveShowData?.isVerifiedOnly == true) {
                false
            } else {
                liveShowData?.allowBidForAll ?: true
            }

            sellerId = liveShowData?.seller?.id.toString()

            sellerName = liveShowData?.seller?.name.toString()

            sellerImage = if (liveShowData?.seller?.image?.isNotEmpty() == true) {
                if (liveShowData?.seller?.image?.contains(Const.BASE_URL) == true) {
                    liveShowData?.seller?.image ?: ""
                } else {
                    "${Const.BASE_URL + "/"}${liveShowData?.seller?.image ?: ""}"
                }
            } else {
                ""
            }

            if (sellerImage?.isNotEmpty() == true) bind.userImage.loadUrl(mCtx, sellerImage ?: "")

            bind.userName.text = liveShowData?.seller?.name
            bind.rating.text = liveShowData?.seller?.rating?.ifEmpty { "0.0" }

            bind.liveCount.text = liveShowData?.viewerCount

            bind.follow.setHapticClickListener {
                bind.loader.isVisible = true
                viewModel.followUser(sellerId?.request(), showId.toString().request())
            }

            showId = liveShowData?.showId
            showTitle = liveShowData?.showDetail
            if (showThumbnail?.isEmpty() == true) {
                showThumbnail = liveShowData?.thumbnail
                bind.thumbnailView.loadUrl(mCtx, showThumbnail ?: "", R.drawable.placeholder_rect)
            }

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

            bind.buyNowBtn.setHapticClickListener {
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

            bind.max.setHapticClickListener {

                if (App.profileResponse.value?.hasShippingAddress == true && App.profileResponse.value?.hasCardAdded == true) {

                    if (isAllowBidForAll) {
                        showInputSheet()
                    } else {
                        if (App.profileResponse.value?.buyerIdentityStatus == "verified") {
                            showInputSheet()
                        } else {
                            verificationDialog()
                        }
                    }
                } else {
                    showPaymentAndAddressSheet()
                }


            }

            socketManager?.getBidTimerUpdate { json ->
                updateCountdown(json)
            }

            socketManager?.getBidTimerUpdateBreakSpot { json ->
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

    @SuppressLint("SetTextI18n")
    fun checkShowTime(time: Long) {
        val currentTime = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }

        val showTime = Calendar.getInstance().apply {
            timeInMillis = time
        }

        val isToday = showTime.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val isInTheFuture = showTime.after(currentTime)

        if (isToday && isInTheFuture) {
            val timeDiffInMillis = showTime.timeInMillis - currentTime.timeInMillis
            if (timeDiffInMillis <= 900000) {
                startCountdown(timeDiffInMillis, time)
            } else {
                bind.showTime.text = "Today, " + Utils.getTimeFromTimestamp(time, "HH:mm")
            }
        } else {
            setTimeAndTitle(time)
        }
    }

    fun setTimeAndTitle(time: Long) {
        bind.showTimeTitle.text = buildString {
            append("Show Starts at -")
            append(Utils.getSimpleDate(Const.MMM_dd_yyyy_HH_mm).format(time))
        }
        bind.showTime.text = "Waiting for Host..."
    }

    fun startCountdown(timeRemainingInMillis: Long, time: Long) {
        val handler = Handler()
        val countdownRunnable = object : Runnable {
            var timeRemaining = timeRemainingInMillis

            override fun run() {
                if (timeRemaining > 0) {
                    val minutes = (timeRemaining / 1000) / 60
                    val seconds = (timeRemaining / 1000) % 60
                    bind.showTimeTitle.text = "Show starting in"
                    bind.showTime.text = String.format("%02d:%02d", minutes, seconds)
                    timeRemaining -= 1000
                    handler.postDelayed(this, 1000)
                } else {
                    setTimeAndTitle(time)
                }
            }
        }

        handler.post(countdownRunnable)
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
            "Before you interact with live shows, You need to become a Verified Buyer.",
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
            val bidAmount = newBidAmount(highestBidAmount?.toDouble()?.toInt() ?: 0).toString()
            if (surpriseSetAuctionRunning) {
                socketManager?.emitBidBreakSpot(
                    roomId = roomID,
                    userId = userId,
                    userName = userName,
                    userImage = userImage,
                    bidAmount = bidAmount,
                    breakSpotAuctionData
                )
            } else {
                socketManager?.emitBid(
                    roomId = roomID,
                    userId = userId,
                    userName = userName,
                    userImage = userImage,
                    productId = bidProductId,
                    bidAmount = bidAmount,
                    auctionTypeId = liveShowData?.auctionTypeId
                )
            }

            Alerts.success(mCtx, "Bid placed successfully")
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
                    append(App.profileResponse.value?.defaultCard?.expMonth)
                    append("/")
                    append(App.profileResponse.value?.defaultCard?.expYear)
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
                    bidAmount = priceText,
                    auctionTypeId = liveShowData?.auctionTypeId
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
            requireActivity().runOnUiThread {
                if (json.optString("room_id") == roomID) {
                    val remaining = value.toIntOrNull() ?: 0
                    if (remaining <= 0) {
                        // Fallback UI state when timer ends but finalize event is delayed/missed.
                        isAuctionStarted = false
                        bind.bidTime.isVisible = false
                        bind.bidLayout.isVisible = false
                        bind.buyNowBtn.isVisible = false
                        bind.soldLayout.isVisible = true
                        bind.productLayout.isVisible = false
                        bind.status.isVisible = true
                        return@runOnUiThread
                    }

                    bind.bidTime.isVisible = true
                    val color = if (remaining <= 10) {
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
                App.manager.leaveChannel()
                App.manager.joinSubscriberChannel(streamID, roomID)
                currentRemoteUid?.let { uid ->
                    setupRemoteVideo(uid)
                }

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
        sellerMenuList.add(MoreModel(R.drawable.ic_block, "Block", "block"))
        sellerMenuList.add(MoreModel(R.drawable.ic_warning, "Report", "report"))

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
                                ).putExtra("sellerId", sellerId)
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

        sellerInfoSheetBinding.follow.text = if (isFollowing) "Following" else "Follow"

        sellerInfoSheetBinding.userName.text = data.sellerDetails?.username ?: ""
        sellerInfoSheetBinding.rating.text = (data.ratingAvg ?: 0).toString()
        sellerInfoSheetBinding.review.text = (data.review ?: 0).toString()
        sellerInfoSheetBinding.sold.text = (data.soldCount ?: 0).toString()
        sellerInfoSheetBinding.shipping.text = (data.avgShip ?: 0).toString()
        sellerInfoSheetBinding.userImage.loadUrl(mCtx, data.sellerDetails?.profileImage ?: "")

        sellerInfoSheetBinding.follow.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.followUser(sellerId?.request(), showId.toString().request())
            sellerInfoSheet.dismiss()
        }

        sellerInfoSheet.show()
    }

    private fun updateFollowUi() {
        runSafe {
            bind.follow.text = if (isFollowing) "Following" else "Follow"
            bind.enterFreebie.text =
                if (isFollowing) "Enter Freebie" else "Follow Host & Enter Freebie"
        }
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
            viewModel.followUser(sellerId?.request(), showId.toString().request())
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
            bind.notesFreebieLayout.isVisible = false
        } else {
            App.PIPMode = false
            bind.profileLayout.isVisible = true
            bind.bottomUI.isVisible = true
            bind.notesFreebieLayout.isVisible = true

            ProductDetailsActivity.instance?.finish()

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
        val sellerRequest = sellerId?.takeIf { it.isNotBlank() }?.request()
        if (sellerRequest == null) {
            Alerts.error(mCtx, "Seller information is unavailable")
            return
        }

        bind.loader.isVisible = true
        viewModel.blockUnblockUser(sellerRequest)

        viewModel.blockUnblockUserRepo.observe(viewLifecycleOwner) {
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
        var reasonId = ""
        mBind.reason.setOnItemClickListener { _, _, position, _ ->
            reasonId = data[position]?.id.toString()
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

        showNotesSheetBind.notes.setHtmlFromString(showNotes?.ifEmpty { "No notes added yet." }, false)

        // Basecamp #9933402746 (2026-05-27): remember the binding so the socket
        // listener can live-refresh contents when seller edits.
        openShowNotesBinding = showNotesSheetBind
        sheet.setOnDismissListener { openShowNotesBinding = null }

        showNotesSheetBind.close.setHapticClickListener {
            sheet.dismiss()
            bind.showNotes.isVisible = true
        }

        sheet.show()
    }

    val party = Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = 360,
        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
        position = Position.Relative(0.5, 0.3)
    )

    fun showWonView(username: String, userImage: String, desc: String) {
        bind.wonView.root.isVisible = true
        bind.wonView.userName.text = username
        bind.wonView.desc.text = desc
        bind.wonView.userImage.loadUrl(mCtx, userImage, draw.app_icon_dollar)


        bind.wonView.konfettiView.start(party)

        Handler(Looper.getMainLooper()).postDelayed({
            val flip = ObjectAnimator.ofFloat(bind.wonView.imageCard, "rotationY", 0f, 180f)
            flip.duration = 1000

            flip.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    val flipBack = ObjectAnimator.ofFloat(bind.wonView.imageCard, "rotationY", 0f, 180f)
                    flipBack.duration = 1000
                    flipBack.start()
                }
            })
            flip.start()
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            bind.wonView.root.isVisible = false
            bind.wonView.konfettiView.stop(party)
        }, 5000)

    }

    /**
     * Populates the buyer's LuckyWheelView with the current freebie entrant list.
     * Handles all 4 randomizer types:
     *   - product_raffle / blind_product_raffle: slots show usernames (winner takes product)
     *   - buyer_raffle: same (buyer enters for free or paid)
     *   - wheel_bin_auction: purely decorative, winner comes from bid mechanics
     * Colors cycle through the locked 12-swatch palette.
     */
    private fun setupBuyerWheel() {
        if (freebieUsers.isEmpty()) return
        val colors = listOf(
            "#FF6B6B", "#FFA94D", "#FFD43B", "#82C91E",
            "#51CF66", "#20C997", "#22B8CF", "#339AF0",
            "#5C7CFA", "#845EF7", "#CC5DE8", "#F06595"
        ).map { android.graphics.Color.parseColor(it) }

        val wheelData = ArrayList(
            freebieUsers.mapIndexed { idx, u ->
                com.caneryilmaz.apps.luckywheel.data.WheelData(
                    text = u?.userName?.trim() ?: u?.name?.trim() ?: "?",
                    textColor = intArrayOf(android.graphics.Color.WHITE),
                    backgroundColor = intArrayOf(colors[idx % colors.size])
                )
            }
        )

        bind.buyerLuckyWheel.apply {
            setCenterPointRadius(40f)
            setWheelData(wheelData)
            setCornerPointsRadius(16f)
            setArrowPosition(com.caneryilmaz.apps.luckywheel.constant.ArrowPosition.CENTER)
            setTextOrientation(com.caneryilmaz.apps.luckywheel.constant.TextOrientation.VERTICAL_TO_CENTER)
        }
    }

    private fun rotateText(userId: Int?) {
        if (freebieUsers.isEmpty()) {
            bind.winnerSpotLayout.isVisible = false
            return
        }
        var currentIndex = 0
        bind.winnerTitle.text = "Selecting Freebie Winner"
        val handler = Handler()
        if (freebieUsers.size > 1) {
            val finalIndex = freebieUsers.indexOf(freebieUsers.find { it?.id == userId })

            val textSwitcherRunnable = object : Runnable {
                override fun run() {

                    if (currentIndex == finalIndex) {
                        bind.textSwitcher.setText(buildSpannedString {
                            color(ContextCompat.getColor(mCtx, clr.success)) { append("${freebieUsers[currentIndex]?.name} won") }
                        })

                        handler.postDelayed({
                            bind.winnerSpotLayout.isVisible = false
                            bind.notesFreebieLayout.isVisible = true
                            bind.freebieLayout.isVisible = false
                            currentIndex = 0
                        }, 2000)
                        return
                    } else {
                        bind.textSwitcher.setText(freebieUsers[currentIndex]?.name)
                    }

                    if (currentIndex < finalIndex) currentIndex++

                    handler.postDelayed(this, 200)
                }
            }

            // Start after 2 seconds
            bind.textSwitcher.setText(freebieUsers[currentIndex]?.name)
            handler.postDelayed(textSwitcherRunnable, 200)
        } else {
            handler.postDelayed({
                bind.winnerSpotLayout.isVisible = false
                bind.notesFreebieLayout.isVisible = true
                bind.freebieLayout.isVisible = false
                currentIndex = 0
            }, 2000)
            bind.textSwitcher.setText(freebieUsers[currentIndex]?.name)
        }
    }

    private fun rotateBreakSpotText(userId: Int?) {
        var currentIndex = 0
        bind.winnerSpotLayout.isVisible = true
        bind.winnerTitle.text = "Selecting Spot"
        val handler = Handler()
        if (breakSpotUsers.size > 1) {
            val finalIndex = breakSpotUsers.indexOf(breakSpotUsers.find { it?.first == userId })
           val textSwitcherRunnable= object : Runnable {
                override fun run() {

                    if (currentIndex == finalIndex) {
                        bind.winnerTitle.text = "Break Spot \uD83C\uDF89"
                        bind.popperView.isVisible=true

                        bind.textSwitcher.setText(buildSpannedString {
                            color(ContextCompat.getColor(mCtx, clr.success)) { append("${breakSpotUsers[currentIndex]?.second} won") }
                        })

                        handler.postDelayed({
                            bind.winnerSpotLayout.isVisible = false
                            bind.popperView.isVisible=false
                            bind.notesFreebieLayout.isVisible = true
                            bind.freebieLayout.isVisible = false
                            currentIndex = 0
                        }, 2000)
                        return
                    } else {
                        bind.textSwitcher.setText(breakSpotUsers[currentIndex]?.second)
                    }

                    if (currentIndex < finalIndex) currentIndex++

                    handler.postDelayed(this, 200)
                }
            }
            // Start after 2 seconds
            bind.textSwitcher.setText(breakSpotUsers[currentIndex]?.second)
            handler.postDelayed(textSwitcherRunnable, 200)
        } else {
            bind.winnerTitle.text = "Break Spot \uD83C\uDF89"
            bind.popperView.isVisible=true
            bind.textSwitcher.setText(breakSpotUsers[0]?.second)
            
            handler.postDelayed({
                bind.winnerSpotLayout.isVisible = false
                bind.popperView.isVisible=false
                bind.notesFreebieLayout.isVisible = true
                bind.freebieLayout.isVisible = false
                currentIndex = 0
            }, 2000)
        }
    }

    fun createClipSheet() {

        clipSheetBind.close.setHapticClickListener {
            clipSheet.dismiss()
        }

        // Basecamp #9929851737 (2026-05-27): wire the duration slider readout.
        clipSheetBind.durationSlider.clearOnChangeListeners()
        clipSheetBind.durationSlider.addOnChangeListener { _, value, _ ->
            clipSheetBind.durationValueText.text = "${value.toInt()}s"
        }

        // Basecamp #9929851737 (2026-05-27): fire the API call only when the
        // buyer taps Create Clip. Duration is whatever the slider is at
        // (1..60s), default 30s.
        clipSheetBind.createClipBtn.setHapticClickListener {
            val durationSec = clipSheetBind.durationSlider.value.toInt().coerceIn(1, 60)
            clipSheetBind.durationPickerWrap.isVisible = false
            clipSheetBind.loaderView.isVisible = true
            clipSheetBind.videoView.isVisible = false
            clipSheetBind.bottomLayout.isVisible = false
            viewModel.getClip(roomID.request(), durationSec.toString().request())
        }

        // Basecamp #9929851737 (2026-05-27): SAVE = dismiss. The clip is
        // already persisted server-side by /api/make-clip at the moment the
        // preview shows, so Save just acknowledges and closes the sheet.
        clipSheetBind.saveClipBtn.setHapticClickListener {
            successToast("Clip saved")
            clipSheet.dismiss()
        }

        // Basecamp #9929851737 (2026-05-27): CANCEL = dismiss without saving
        // a toast. TODO: when backend exposes a delete-clip endpoint, call it
        // here to remove the just-created clip. Right now the clip remains
        // server-side and the user would need to delete it from their clip
        // list.
        clipSheetBind.cancelClipBtn.setHapticClickListener {
            // TODO(#9929851737): wire DELETE /api/clips/{id} once backend exposes it.
            clipSheet.dismiss()
        }

        clipSheet.show()

        clipSheet.setOnDismissListener {
            exoPlayer.release()
        }

    }

    fun finalizeBidUpdateUI(json: JSONObject,showWon:Boolean=true) {
        runSafe {
            requireActivity().runOnUiThread {

                val winner = json.getJSONObject("winner")
                log("WINNER: $winner")

                if (roomID == json.optString("room_id")) {
                    bind.bidTime.isVisible = false
                    bind.soldLayout.isVisible = true
                    bind.buyNowBtn.isVisible = false
                    bind.bidLayout.isVisible = false

                    val bidderName = winner.optString("user_name") ?: ""
                    val bidderImage = winner.optString("user_image")

                    if (bidderName.isNotEmpty()) {
                        bind.winningLayout.isVisible = true

                        bind.userImage.loadUrl(mCtx, bidderImage)

                        if (userId == winner.optString("user_id")) {
                            bind.winning.text = buildSpannedString {
                                color(ContextCompat.getColor(mCtx, R.color.primary)) {
                                    bold { append(" you won!") }
                                }
                            }
                       if(showWon)     showWonView("You", bidderImage, "won the auction!")
                        } else {
                            bind.winning.text = buildSpannedString {
                                append(bidderName)
                                color(ContextCompat.getColor(mCtx, R.color.primary)) {
                                    bold { append(" has won!") }
                                }
                            }
                            if(showWon)     showWonView(bidderName, bidderImage, "won the auction!")
                        }

                    } else {
                        bind.winningLayout.isVisible = false
                        bind.soldLayout.isVisible = true
                    }
                }
            }
        }

    }
}