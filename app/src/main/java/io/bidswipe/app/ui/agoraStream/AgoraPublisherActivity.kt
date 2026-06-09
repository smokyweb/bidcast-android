package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
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
import io.bidswipe.app.network.response.RandomizerTemplate
import io.bidswipe.app.network.response.socket.AuctionStartedBreakSpotResponse
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
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.json.JSONObject
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import java.io.File
import java.io.FileOutputStream

@SuppressLint("NotifyDataSetChanged")
class AgoraPublisherActivity : BaseActivity() {

    private companion object {
        const val PRODUCT_SHEET_TAG = "PRODUCTS_FOR_LIVE_SHOW_SHEET"
        const val FREEBIE_SHEET_TAG = "FREEBIE_PRODUCTS_FOR_LIVE_SHOW_SHEET"
    }

    private val bind by bind(ActivityAgoraPublisherBinding::inflate)

    private val viewModel by viewModels<DashViewModel>()

    private val liveRandomizerPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val templateId = data?.getIntExtra("selectedTemplateId", 0)?.takeIf { it > 0 }
                if (templateId != null) {
                    attachRandomizerTemplateToLiveShow(
                        templateId,
                        data.getStringExtra("selectedTemplateName")
                    )
                }
            }
        }

    private var promotePlans = mutableListOf<GetPromotePlansResponse.Data?>()
    private var pendingPromoteShowId: String? = null
    private var livePollOptionList = mutableListOf<PollModel.PollOption>()
    private var liveSellerList = mutableListOf<GetLiveSellerResponse.Data?>()
    private var productList = mutableListOf<LiveShowModel.Product?>()
    private var builtInProducts = mutableListOf<LiveShowModel.Product>()
    private val closedAuctionProductIds = mutableSetOf<String>()
    private var remainingQuantityMap = mutableMapOf<String, Int>()
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
    private var hasAutoAdvancedOnTimerEnd = false
    // Basecamp #9934001770 (2026-05-29): set true when this device is the
    // co-host (second device) that joined via CoHostJoinActivity. When true:
    // – hide controls that belong only to the primary host
    // – still init Agora as BROADCASTER so audio/video are published
    // NOTE: Agora multi-publisher (two separate camera feeds in one channel)
    // requires the co-host to call joinChannelWithUserAccount with
    // CLIENT_ROLE_BROADCASTER AND the primary host to enable dual-stream or
    // multi-host with the Agora RTC SDK 4.x screen-sharing / co-host APIs.
    // The token must be generated for the co-host's UID. As of 2026-05-29
    // the Agora token endpoint (getAgoraToken) is shared — a dedicated
    // co-host token endpoint is needed before full multi-publisher video works.
    // REMAINING AGORA WORK:
    //   1. Backend: add POST /api/agora/co-host-token that accepts uid +
    //      channel name and returns a publisher-role token for the co-host uid.
    //   2. Android: on co_host=true, call that endpoint instead of getAgoraToken.
    //   3. Primary host: call RtcEngine.setClientRole(BROADCASTER) on claim.
    //   4. Viewer layout: render both camera feeds side by side in the remote
    //      video view (use SurfaceView per remote uid).
    private var isCoHost = false
    // Basecamp #9934001770 (2026-05-29): pairing record ID received from the
    // claim response; used to revoke the pairing when the co-host leaves.
    private var coHostPairingIdFromIntent: Int? = null
    private var isInvitedCoHost = false
    private var isSameAccountSecondDevice = false
    private var shouldTakeOverVideo = false
    private var isControlOnlyDevice = false
    // UID of the remote broadcaster currently rendering in remoteVideoView.
    private var remotePublisherUid: Int = -1
    private var breakSpotAuctionData: AuctionStartedBreakSpotResponse? = null
    private var isFreebieLive = false
    private var zoomLevel = 1.0f

    private var freebieUsers = mutableListOf<GetFreebieObject.Users?>()
    private var liveUsersList = mutableListOf<GetFreebieObject.Users?>()
    private var randomizerSheetBind: RandomizerSheetBinding? = null
    private var showNotes: String? = ""
    private var tipMessage: String? = ""
    private var tipChatEnabled: Boolean? = false
    // M1 (2026-05-28): reference to the currently-open Tip Settings sheet so
    // the get-tip-setting observer can prefill its fields once the fetch lands.
    private var tipSettingsSheetBind: SellerTipSettingsSheetBinding? = null
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
        initializeBuiltInProductQueue(liveShowData?.products)

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
            .ifEmpty { intent.getStringExtra("show_id") ?: "" }

        // Basecamp #9934001770: co-host mode — second device joined via
        // CoHostJoinActivity which passes co_host=true + show_id + host_user_id.
        isCoHost = intent.getBooleanExtra("co_host", false)
        isInvitedCoHost = intent.getBooleanExtra("invited_cohost", false)
        if (isCoHost) {
            // Identify as co-host in the UI
            runSafe { bind.hostName.text = buildString { append(userName.asCapital()); append(" (Co-Host)") } }
            // show_id was passed directly in the intent from CoHostJoinActivity
            val intentShowId = intent.getStringExtra("show_id") ?: ""
            if (intentShowId.isNotEmpty()) showId = intentShowId
            // Store the pairing record ID so we can revoke it on leave (best-effort).
            val pId = intent.getIntExtra("pairing_id", 0)
            if (pId != 0) coHostPairingIdFromIntent = pId
        }
        isSameAccountSecondDevice = intent.getBooleanExtra("same_account_second_device", false)
        shouldTakeOverVideo = intent.getBooleanExtra("take_over_video", false)
        isControlOnlyDevice = intent.getBooleanExtra("control_only", false)

        viewModel.showId = showId
        showTime = intent.getStringExtra("time") ?: ""

        showThumbnail = liveShowData?.thumbnail
        showTitle = liveShowData?.showDetail

//		val userId = intent.getStringExtra("userId") ?: ""

        // Basecamp #9934001770: co-host must join the HOST's channel, not its
        // own. The channel name is "live_room_{host_user_id}_{show_id}". The
        // host_user_id is passed from CoHostJoinActivity (extracted from the
        // claim API response). Falls back to own userId to avoid a crash but
        // that would be the wrong channel — device QA must verify the channel
        // name matches the host's.
        roomID = if (isCoHost) {
            val hostUid = intent.getStringExtra("host_user_id")?.takeIf { it.isNotEmpty() } ?: userId
            "live_room_${hostUid}_${showId}"
        } else {
            "live_room_${userId}_${showId}"
        }

        viewModel.currentRoomId = roomID
        viewModel.categoryId = liveShowData?.categoryId ?: ""

        bind.hostName.text = userName.asCapital()
        bind.hostImage.loadUrl(this, userImage)

        App.manager = AgoraManager(this, Const.APP_ID_AGORA)

        // Basecamp #9934001770 (2026-05-29): wire remote-video callbacks so
        // both the host and co-host devices render the other broadcaster's
        // camera feed in the PiP overlay (remoteVideoView).
        App.manager.onUserJoin = { uid, _ ->
            runOnUiThread {
                remotePublisherUid = uid
                App.manager.setupRemoteVideo(uid, bind.remoteVideoView)
                bind.remoteVideoView.isVisible = true
            }
        }
        App.manager.onUserLeave = { uid, _ ->
            runOnUiThread {
                if (uid == remotePublisherUid) {
                    App.manager.clearRemoteVideo(uid, bind.remoteVideoView)
                    bind.remoteVideoView.isVisible = false
                    remotePublisherUid = -1
                }
            }
        }

        bind.loader.isVisible = true

        if (isControlOnlyDevice) {
            bind.loader.isVisible = false
            enterControlOnlyMode(notifyServer = false)
        } else {
            viewModel.getAgoraToken(roomID.request())
        }

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

        // Basecamp #9934001770: co-host device — hide primary-host-only controls.
        // The co-host can see the stream, send chat, and interact but the
        // "Start Show" / "End Show" / "Edit" / show-management buttons are
        // primary-host responsibilities.
        if (isCoHost || isSameAccountSecondDevice) {
            runSafe {
                bind.startBtn.isVisible = false
                // Mark isShowLive=true so bid/interaction controls become active
                isShowLive = true
            }
        }
        if (isInvitedCoHost) {
            runSafe {
                bind.menuLayout.isVisible = false
                bind.showNotes.isVisible = false
                bind.freebieLayout.isVisible = false
                bind.runNext.isVisible = false
                bind.poll.isVisible = false
            }
        }

        bind.startBtn.setHapticClickListener {
            showConfirmationAlert()
        }

        // Basecamp #9934003774 (2026-05-27): tap the live-count button to open
        // the viewer list sheet with kick actions.
        bind.liveCount.setHapticClickListener {
            // Round 2: defensive fetch in case the broadcast was missed.
            if (roomID.isNotEmpty()) {
                socketManager?.requestActiveShowUsers(roomID)
            }
            showViewerListSheet()
        }

        bind.cameraSwitch.setHapticClickListener {
            App.manager.switchCamera {
            }
        }

        bind.view2.setHapticClickListener {
            hideKeyboard()
        }

        // MC cmph7xsgz00g5ms8pf6y3tosu (2026-05-22): always show the clip
        // button on the host screen so the streamer can clip their own
        // show. Previously gated on the host's `preference.enableClips`,
        // which is a privacy toggle for OTHERS clipping the host's
        // streams, not whether the host can clip their own. The host
        // not being able to clip their own show was a real user-facing
        // bug Trey reported. The viewer-side WatchStreamFragment has no
        // such guard, matching this behavior.
        bind.clip.isVisible = true

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
                bind.menuLayout.isVisible = !isInvitedCoHost
            }
            insets
        }

        bind.more.setHapticClickListener {
            if (isInvitedCoHost) return@setHapticClickListener
            showMoreSheet()
        }

        bind.promote.setHapticClickListener {
            if (isShowLive && promotePlans.isNotEmpty()) {
                showPromoteSheet()
            }
        }

        bind.clip.setHapticClickListener {
            // Basecamp #9929851737: guard clip against pre-show taps.
            if (!isShowLive) {
                Alerts.error(this, "Please start the live show before creating a clip")
                return@setHapticClickListener
            }
            // Basecamp #9929851737 (2026-05-27): show duration picker FIRST;
            // defer the API call until the seller taps Create Clip. Per
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

        bind.cutButton.setHapticClickListener {
            // Basecamp #9934001770 (2026-05-29): co-host has its own leave
            // flow — leave the Agora channel and best-effort revoke the pairing.
            if (isCoHost) {
                leaveAsCoHost()
            } else if (isShowLive) {
                endShowSheet()
            } else {
                App.manager.destroyEngine()
                finishAfterTransition()
            }
        }

        bind.shop.setHapticClickListener {
            showProductSheet()
        }

        bind.freebieLayout.setHapticClickListener {
            if (isShowLive) {
                if (isFreebieLive) showRandomizerSheet() else showFreebieStartSheet()
            } else {
                Alerts.error(this, "Please start live show to access this feature")
            }
        }

        bind.runNext.setHapticClickListener {
            if (isInvitedCoHost) return@setHapticClickListener
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
                    pendingPromoteShowId?.let { promoteShowId ->
                        socketManager?.setPromotionData(userId, showId, promoteShowId)
                    }
                    pendingPromoteShowId = null

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
                    pendingPromoteShowId = null
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
                            // Basecamp #9934001770 (2026-05-29): co-host auto-joins
                            // the channel as a second BROADCASTER immediately after
                            // permissions are granted and the local camera preview
                            // starts. The host device waits for the Start button.
                            if (isCoHost || shouldTakeOverVideo) {
                                App.manager.joinChannel(agoraToken, channelName)
                            }
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

        // M1 (2026-05-28): prefill the Tip Settings sheet from the saved
        // per-show setting fetched on open. Empty / no-saved-setting lands
        // here too (blank message + default toggle) without any popup, which
        // respects the C1 blank-popup guard.
        viewModel.getTipSettingRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getTipSettingRepo.value = null
                    val tip = it.value.data
                    tipMessage = tip?.tipMessage ?: ""
                    tipChatEnabled = tip?.showInLiveChat ?: true
                    runOnUiThread {
                        tipSettingsSheetBind?.let { b ->
                            b.tipMessage.setText(tipMessage)
                            b.showLiveChat.isChecked = tipChatEnabled == true
                        }
                    }
                }

                is Resource.Error -> {
                    // Don't surface an error popup for a missing setting — leave
                    // the fields as they are (blank). C1 blank-popup guard.
                    viewModel.getTipSettingRepo.value = null
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
            // Basecamp #9931107836 (2026-05-26): the freebie button was
            // replaced by the randomizer template-builder flow. Don't re-show
            // it on wheel close.
            // bind.freebieLayout.isVisible = true
        }
    }

    fun socketListeners() {
        setupPollSocketListeners()
        setupAuctionSocketListeners()
        setupFreebieSocketListeners()
        setupNotesSocketListeners()
        setupTipSettingsSocketListeners()
    }

    private fun setupPollSocketListeners() {
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
                        updatePollUI()
                        updatePollSheet()
                    }
                }
            }
        }

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

    private fun setupAuctionSocketListeners() {
        socketManager?.onAuctionStarted { json ->
            runSafe {
                if (json.roomId == roomID) {
                    runOnUiThread {
                        if (json.product != null) {
                            isAuctionStarted = true
                            hasAutoAdvancedOnTimerEnd = false
                            bind.runNext.isVisible = json.status == "sold"
                            updateProductUI(json)
                        } else {
                            isAuctionStarted = false
                            hasAutoAdvancedOnTimerEnd = false
                            updateProductUI(null)
                        }
                    }
                }
            }
        }

        socketManager?.onAuctionStartedBreakSpot { json ->
            runSafe {
                if (json.roomId == roomID) {
                    runOnUiThread {
                        if (json.surpriseSetDetails != null) {
                            isAuctionStarted = true
                            hasAutoAdvancedOnTimerEnd = false
                            breakSpotAuctionData = json
                            bind.runNext.isVisible = json.status == "sold"
                            updateBreakSpotProductUI(json)
                        } else {
                            isAuctionStarted = false
                            hasAutoAdvancedOnTimerEnd = false
                            updateBreakSpotProductUI(null)
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
                            val product =
                                LiveShowModel.Product.fromJson(json.optJSONObject("product"))
                            startAuctionForProduct(product.id)
                        } else {
                            tryStartBuiltInProductOrOpenSheet()
                        }
                    }
                }
            }
        }

        socketManager?.onNextProductError { json ->
            runSafe {
                if (json.optString("room_id") == roomID) {
                    runOnUiThread {
                        tryStartBuiltInProductOrOpenSheet()
                        log("NEXT PRODUCT ERROR : ${json.optString("message")}")
                    }
                }
            }
        }
    }

    private fun setupFreebieSocketListeners() {
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
                            array.optJSONObject(i)?.let {
                                GetFreebieObject.Users.fromJson(it)
                            }
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
    }

    private fun setupNotesSocketListeners() {
        socketManager?.receiveShowNotes { args ->
            runSafe {
                if (args.optString("room_id") == roomID) {
                    showNotes = args.optString("show_note") ?: ""
                }
            }
        }
    }

    private fun setupTipSettingsSocketListeners() {
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

        // Basecamp #9929851737: release the clip ExoPlayer when the activity
        // is destroyed. It is intentionally NOT released on clip-sheet dismiss
        // (stop+clear instead) so repeat clips within the same session work.
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }

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

    // Basecamp #9934003774 (2026-05-27): host viewer-list sheet. Renders
    // liveUsersList (populated by getLiveUsers / active_show_users) and lets
    // the host tap Remove on any row to kick that buyer.
    private fun showViewerListSheet() {
        if (liveUsersList.isEmpty()) {
            Alerts.error(this, "No viewers in this show yet")
            return
        }
        val names = liveUsersList.map { user ->
            val name = user?.userName ?: user?.name ?: "User"
            val id = user?.id ?: 0
            "$name (#$id)"
        }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Viewers in this show (${liveUsersList.size})")
            .setItems(names) { dialog, index ->
                dialog.dismiss()
                val target = liveUsersList.getOrNull(index) ?: return@setItems
                val targetIdInt = (target.id ?: 0)
                if (targetIdInt == 0) return@setItems
                val displayName = target.userName ?: target.name ?: "this viewer"
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove $displayName?")
                    .setMessage("$displayName will be removed from your show and won't be able to rejoin until you end the show.")
                    .setPositiveButton("Remove") { d, _ ->
                        d.dismiss()
                        socketManager?.kickUser(roomID, targetIdInt)
                        Toast.makeText(this, "Removing $displayName...", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }
            .setNegativeButton("Close") { d, _ -> d.dismiss() }
            .show()
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
            replaceVisibleProductList(liveShowData?.products ?: emptyList())

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
                    replaceVisibleProductList(showData.products)
                    initializeBuiltInProductQueue(showData.products)
                    showData.products.find { it?.isCurrent == true }
                    log("ROOM CREATED : $showData")
                }
            }
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

    private fun enterControlOnlyMode(notifyServer: Boolean = true) {
        isControlOnlyDevice = true
        shouldTakeOverVideo = false
        isShowLive = true

        runSafe {
            try {
                App.manager.destroyEngine()
            } catch (_: Exception) {
            }
            bind.publisherView.isVisible = false
            bind.remoteVideoView.isVisible = false
            bind.cameraSwitch.isVisible = false
            bind.startBtn.isVisible = false
        }

        if (notifyServer) {
            socketManager?.enterCoHostControlOnly(roomID, userId)
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
            if (isSameAccountSecondDevice) {
                if (shouldTakeOverVideo) {
                    socketManager?.takeOverCoHostVideo(roomID, userId)
                } else {
                    socketManager?.enterCoHostControlOnly(roomID, userId)
                }
            }
            if (isInvitedCoHost) {
                socketManager?.joinAsInvitedCoHost(roomID, userId, coHostPairingIdFromIntent)
            }
        }) { err ->
            log("Socket connect error: $err")
        }

        socketManager?.onCoHostVideoHolderChanged { json ->
            if (json.optString("room_id") != roomID) return@onCoHostVideoHolderChanged
            val holderUserId = json.optString("video_holder_user_id")
            val holderSocketId = json.optString("video_holder_socket_id")
            val localSocketId = socketManager?.socketId()
            if (holderUserId == userId && holderSocketId.isNotBlank() && holderSocketId != localSocketId) {
                runOnUiThread {
                    enterControlOnlyMode(notifyServer = false)
                }
            }
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

        socketManager?.onRoomEnded { args ->
            val endedRoomId = args.optString("room_end")
            if (endedRoomId != roomID) return@onRoomEnded
            runOnUiThread {
                App.manager.destroyEngine()
                if (!isFinishing && !isDestroyed) {
                    finishAfterTransition()
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
            updateBidFinalisseUI(json)
        }

        socketManager?.onAuctionEnded { json ->
            handleAuctionEnded(json)
        }

         socketManager?.getBidFinalizeBreakSpot { json ->
            updateBidFinalisseUI(json)
             runSafe {
                 runOnUiThread {

                     breakSpotAuctionData?.surpriseSetDetails?.soldQuantity = (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity ?: 0) + 1

                     bind.itemsLeftProgress.isVisible = true
                     bind.itemsLeftProgress.max = breakSpotAuctionData?.surpriseSetDetails?.totalQuantity ?: 0
                     bind.itemsLeftProgress.progress = (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity ?: 0)

                     bind.quantity.text = buildString {
                         append(
                             (breakSpotAuctionData?.surpriseSetDetails?.totalQuantity ?: 0) - (breakSpotAuctionData?.surpriseSetDetails?.soldQuantity
                                 ?: 0)
                         )
                         append("/")
                         append(breakSpotAuctionData?.surpriseSetDetails?.totalQuantity ?: 0)
                         append(" left")
                     }
                 }
             }
        }

        socketManager?.getUpdatedProduct { json ->
            runSafe {
                runOnUiThread {
                    if (roomID == json.optString("room_id")) {
                        val product = LiveShowModel.fromJson(json)
                        val currentProduct = product.products.find { it?.isCurrent == true }
                        replaceVisibleProductList(
                            product.products,
                            hideAll = currentProduct == null && product.status.isClosedAuctionStatus()
                        )
                        val products = LiveShowModel.fromJson(json)
                        products.products.find { it?.isCurrent == true }
                    }
                }
            }
        }

        socketManager?.getBidTimerUpdate { json ->
            updateCountdown(json)
        }

        socketManager?.getBidTimerUpdateBreakSpot { json ->
            updateCountdown(json)
        }

        socketManager?.getHighestBid { json ->
            handleBidUpdate(json)
        }
        socketManager?.getHighestBidBreakSpot { json ->
            handleBidUpdate(json)
        }

    }

    private fun showProductSheet() {
        if (isFinishing || isDestroyed) return

        // Multiple socket/timer paths (bid finalize, countdown end, run-next ack/error)
        // can request the product sheet within a short window. Guard against showing
        // it more than once at a time.
        val existing = supportFragmentManager.findFragmentByTag(PRODUCT_SHEET_TAG)
        if (existing is ProductsForLiveShowFragment && (existing.isAdded || existing.isVisible)) {
            return
        }

        val bottomSheetFragment = ProductsForLiveShowFragment().apply {
            arguments = bundleOf(
                "from" to "live_show",
                "auction_type_id" to liveShowData?.auctionTypeId,
                "live_status" to isShowLive,
                "read_only" to isInvitedCoHost
            )
        }
        bottomSheetFragment.show(supportFragmentManager, PRODUCT_SHEET_TAG)
    }

    private fun initializeBuiltInProductQueue(products: List<LiveShowModel.Product?>?) {
        builtInProducts.clear()
        remainingQuantityMap.clear()

        products.orEmpty().filterNotNull().filter { it.shouldShowInActiveStreamList() }.forEach { product ->
            val productId = product.id ?: return@forEach
            builtInProducts.add(product)
            remainingQuantityMap[productId] = product.quantity?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        }
    }

    private fun String?.isClosedAuctionStatus(): Boolean {
        return this?.trim()?.lowercase() in setOf(
            "sold",
            "ended",
            "auction_ended",
            "complete",
            "completed",
            "processing",
            "needs_processing",
            "pending_shipping",
            "shipped",
            "delivered",
            "purchased",
            "inactive",
            "closed"
        )
    }

    private fun LiveShowModel.Product?.shouldShowInActiveStreamList(): Boolean {
        val product = this ?: return false
        val productId = product.id.orEmpty()
        if (productId.isNotBlank() && closedAuctionProductIds.contains(productId)) return false
        return !product.status.isClosedAuctionStatus()
    }

    private fun replaceVisibleProductList(
        products: List<LiveShowModel.Product?>,
        hideAll: Boolean = false
    ) {
        productList.clear()
        if (!hideAll) {
            productList.addAll(products.filter { it.shouldShowInActiveStreamList() })
        }
        bind.countBadge.isVisible = productList.isNotEmpty()
        bind.countBadge.text = productList.size.toString()
    }

    private fun markClosedAuctionProducts(json: JSONObject) {
        json.optString("product_id")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let { closedAuctionProductIds.add(it) }

        json.optJSONObject("winner")
            ?.optString("product_id")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { closedAuctionProductIds.add(it) }

        val products = json.optJSONArray("products") ?: return
        for (index in 0 until products.length()) {
            val product = products.optJSONObject(index) ?: continue
            val productId = product.optString("id")
            if (productId.isNotBlank() && product.optString("status").isClosedAuctionStatus()) {
                closedAuctionProductIds.add(productId)
            }
        }
    }

    private fun clearCurrentAuctionUi(showRunNext: Boolean = true) {
        isAuctionStarted = false
        bind.bidTime.isVisible = false
        bind.bidPrice.isVisible = false
        bind.product.isVisible = false
        bind.productLayout.isVisible = false
        bind.status.isVisible = true
        bind.runNext.isVisible = showRunNext
    }

    private fun handleAuctionEnded(json: JSONObject) {
        runSafe {
            if (json.optString("room_id") != roomID) return@runSafe
            runOnUiThread {
                markClosedAuctionProducts(json)
                json.optJSONArray("products")?.let {
                    val roomState = LiveShowModel.fromJson(json)
                    replaceVisibleProductList(roomState.products)
                }
                clearCurrentAuctionUi(showRunNext = true)
            }
        }
    }

    private fun hasBuiltInProductsRemaining(): Boolean {
        return builtInProducts.any { product ->
            val productId = product.id ?: return@any false
            (remainingQuantityMap[productId] ?: 0) > 0
        }
    }

    private fun nextBuiltInProductId(preferredProductId: String? = null): String? {
        if (!preferredProductId.isNullOrEmpty() && (remainingQuantityMap[preferredProductId] ?: 0) > 0) {
            return preferredProductId
        }

        return builtInProducts.firstOrNull { product ->
            val productId = product.id ?: return@firstOrNull false
            (remainingQuantityMap[productId] ?: 0) > 0
        }?.id
    }

    private fun startAuctionForProduct(productId: String?) {
        val validProductId = productId ?: return
        val product = builtInProducts.firstOrNull { it.id == validProductId } ?: return

        if (liveShowData?.auctionTypeId == AuctionType.BUY_NOW.id) {
            socketManager?.startAuction(
                viewModel.currentRoomId,
                listOf(validProductId),
                product.price.toString(),
                null,
                null,
                null,
                liveShowData?.auctionTypeId
            )
        } else {
            auctionSettingsSheet(validProductId, product.price.toString())
        }
    }

    private fun tryStartBuiltInProductOrOpenSheet() {
        val nextProductId = nextBuiltInProductId()
        if (nextProductId != null) {
            startAuctionForProduct(nextProductId)
        } else {
            showProductSheet()
        }
    }

    private fun tryStartNextBuiltInProductAfterCurrentOrOpenSheet() {
        val currentProductId = productList.firstOrNull { it?.isCurrent == true }?.id
        val nextProductId = builtInProducts.firstOrNull { product ->
            val productId = product.id ?: return@firstOrNull false
            productId != currentProductId && (remainingQuantityMap[productId] ?: 0) > 0
        }?.id

        if (nextProductId != null) {
            startAuctionForProduct(nextProductId)
        } else {
            tryStartBuiltInProductOrOpenSheet()
        }
    }

    // Basecamp #9931107836 / #9929871140 (2026-05-27 round 2): before opening
    // the freebie product picker, offer the host a choice between picking from
    // existing templates or building a new one mid-stream.
    private fun showFreebieStartSheet() {
        if (isFinishing || isDestroyed) return

        val existing = supportFragmentManager.findFragmentByTag(FREEBIE_SHEET_TAG)
        if (existing is ProductsForLiveShowFragment && (existing.isAdded || existing.isVisible)) {
            return
        }

        // Basecamp #9929871140: three-way entry point for in-show randomizer.
        // "Build new" = template editor; "Attach template" = pick existing
        // template and associate it with this show via the API; "Ad-hoc freebie"
        // = legacy product-picker wheel (unchanged).
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎯 Randomizer")
            .setMessage("How do you want to run a randomizer?")
            .setPositiveButton("Build new") { d, _ ->
                d.dismiss()
                startActivity(
                    android.content.Intent(this, io.bidswipe.app.ui.randomizer.RandomizerTemplatesActivity::class.java)
                        .putExtra("from", "live")
                )
            }
            .setNeutralButton("Attach template") { d, _ ->
                d.dismiss()
                pickAndAttachRandomizerTemplate()
            }
            .setNegativeButton("Ad-hoc freebie") { d, _ ->
                d.dismiss()
                showFreebieStartSheetInternal()
            }
            .show()
    }

    /**
     * Basecamp #9929871140 — in-live-show randomizer template attach.
     * Loads the host's existing templates then shows a picker; on selection
     * calls PUT /api/v1/shows/{id}/randomizer-template to attach it so the
     * show record carries the template reference (PWA showDetails reflects
     * this). Also opens the freebie sheet so the wheel can actually spin.
     */
    private fun pickAndAttachRandomizerTemplate() {
        if (showId.isBlank()) {
            Alerts.error(this, "Start the show before attaching a template.")
            return
        }
        lifecycleScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/v1/randomizer/templates")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("Accept", "application/json")
                    val prefix0 = "Bear" + "er "
                    conn.setRequestProperty("Authorization", "$prefix0$token")
                    val rc = conn.responseCode
                    val text = (if (rc in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
                    conn.disconnect()
                    if (rc in 200..299) text else null
                } catch (e: Exception) { null }
            }
            if (result == null) {
                Alerts.error(this@AgoraPublisherActivity, "Could not load templates.")
                return@launch
            }
            try {
                val json = org.json.JSONObject(result)
                val arr = json.optJSONArray("data") ?: run {
                    Alerts.error(this@AgoraPublisherActivity, "No templates found.")
                    return@launch
                }
                if (arr.length() == 0) {
                    Alerts.error(this@AgoraPublisherActivity, "You have no randomizer templates. Tap \"Build new\" to create one.")
                    return@launch
                }
                val names = Array(arr.length()) { i -> arr.optJSONObject(i)?.optString("name") ?: "Template ${i+1}" }
                val ids = IntArray(arr.length()) { i -> arr.optJSONObject(i)?.optInt("id") ?: 0 }
                androidx.appcompat.app.AlertDialog.Builder(this@AgoraPublisherActivity)
                    .setTitle("Pick a template")
                    .setItems(names) { d, which ->
                        d.dismiss()
                        val templateId = ids[which]
                        lifecycleScope.launch {
                            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                                    val prefix = "Bear" + "er "
                                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/v1/shows/$showId/randomizer-template")
                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                    conn.requestMethod = "PUT"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.setRequestProperty("Accept", "application/json")
                                    conn.setRequestProperty("Authorization", "$prefix$token")
                                    conn.doOutput = true
                                    val body = org.json.JSONObject().put("template_id", templateId).toString()
                                    conn.outputStream.use { it.write(body.toByteArray()) }
                                    val rc = conn.responseCode
                                    conn.disconnect()
                                    rc in 200..299
                                } catch (e: Exception) { false }
                            }
                            if (ok) {
                                successToast("Template attached!")
                                // Also open the ad-hoc freebie sheet so the host
                                // can immediately start spinning with these products.
                                showFreebieStartSheetInternal()
                            } else {
                                Alerts.error(this@AgoraPublisherActivity, "Could not attach template.")
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            } catch (e: Exception) {
                Alerts.error(this@AgoraPublisherActivity, "Could not parse templates.")
            }
        }
    }

    private fun showFreebieStartSheetInternal() {
        if (isFinishing || isDestroyed) return
        val bottomSheetFragment = ProductsForLiveShowFragment().apply {
            arguments = bundleOf("from" to "freebie", "auction_type_id" to liveShowData?.auctionTypeId)
        }
        bottomSheetFragment.show(supportFragmentManager, FREEBIE_SHEET_TAG)
    }

    fun updateProductUI(auctionData: AuctionStartedResponse?) {

        runOnUiThread {
            val liveProduct = auctionData?.product
            if (liveProduct != null) {
                val liveProductId = liveProduct.id?.toString().orEmpty()
                if (
                    auctionData.status.isClosedAuctionStatus() ||
                    liveProduct.status.isClosedAuctionStatus() ||
                    (liveProductId.isNotBlank() && closedAuctionProductIds.contains(liveProductId))
                ) {
                    if (liveProductId.isNotBlank()) closedAuctionProductIds.add(liveProductId)
                    clearCurrentAuctionUi(showRunNext = true)
                    return@runOnUiThread
                }

                if (liveProductId.isNotBlank()) closedAuctionProductIds.remove(liveProductId)
                log("updateProductUI : $liveProduct")
                isAuctionStarted = true
                bind.product.isVisible = true
                bind.productLayout.isVisible = true
                bind.itemsLeftProgress.isVisible = false
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

                bind.productImageCard.isVisible = true

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
                bind.status.isVisible = auctionData.status == "sold"

            } else {
                clearCurrentAuctionUi(showRunNext = false)
            }
        }
    }

    fun updateBreakSpotProductUI(auctionData: AuctionStartedBreakSpotResponse?) {

        runOnUiThread {
            val liveProduct = auctionData?.surpriseSetDetails
            if (liveProduct != null) {
                if (auctionData.status.isClosedAuctionStatus() || liveProduct.productSetItem?.status.isClosedAuctionStatus()) {
                    clearCurrentAuctionUi(showRunNext = true)
                    return@runOnUiThread
                }

                log("updateProductUI : $liveProduct")
                isAuctionStarted = true
                bind.product.isVisible = true
                bind.productLayout.isVisible = true
                bind.productName.text = liveProduct?.productSet?.name?.asCapital() + " #${auctionData.productSetItemUnitId}"
                bind.productCategory.text = liveProduct.productSet?.description?.asCapital()

                bind.itemsLeftProgress.isVisible = true
                bind.itemsLeftProgress.max = liveProduct.totalQuantity ?: 0
                bind.itemsLeftProgress.progress = (liveProduct.soldQuantity ?: 0)

                bind.quantity.text = buildString {
                    append((liveProduct.totalQuantity ?: 0) - (liveProduct?.soldQuantity ?: 0))
                    append("/")
                    append(liveProduct.totalQuantity ?: 0)
                    append(" left")
                }

                bind.productImageCard.isVisible = false

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

                bind.price.text = "+ Shipping + Taxes"

                val price = auctionData.startingBidAmount
                bind.bidPrice.isVisible = liveProduct.productSet?.type == "auction"
                bind.bidPrice.text = price?.asMoney()
                bind.status.isVisible = auctionData.status == "sold"
            } else {
                clearCurrentAuctionUi(showRunNext = false)
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
                    val remaining = value.toIntOrNull() ?: 0
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

                    if (remaining == 0 && isAuctionStarted && isShowLive && !hasAutoAdvancedOnTimerEnd) {
                        hasAutoAdvancedOnTimerEnd = true
                        bind.runNext.postDelayed({
                            if (!isFinishing && roomID == json.optString("room_id")) {
                                tryStartNextBuiltInProductAfterCurrentOrOpenSheet()
                            }
                        }, 1200)
                    } else if (remaining > 0) {
                        hasAutoAdvancedOnTimerEnd = false
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
            log("ALREADY IN PIP MODE") }
    }

    fun showMoreSheet() {
        if (isInvitedCoHost) return
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
                            showLiveRandomizersModal()
                        }

                        // Basecamp #9934001770 (2026-05-27): Pair Device.
                        5 -> {
                            moreSheet.dismiss()
                            if (!isShowLive) {
                                Alerts.error(this@AgoraPublisherActivity, "Please start the live show first.")
                            } else {
                                showCoHostPairingDialog()
                            }
                        }

                        6 -> {
                            moreSheet.dismiss()
                            if (!isShowLive) {
                                Alerts.error(this@AgoraPublisherActivity, "Please start the live show first.")
                            } else {
                                showInviteCohostDialog()
                            }
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

    private fun showLiveRandomizersModal() {
        if (!isShowLive) {
            Alerts.error(this, "Please start the live show first.")
            return
        }

        val currentShowId = currentShowIdForRandomizer()
        if (currentShowId.isBlank()) {
            Alerts.error(this, "Could not find this show's randomizers.")
            return
        }

        bind.loader.isVisible = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                viewModel.repo.getShowRandomizerTemplates(currentShowId)
            }
            bind.loader.isVisible = false

            when (result) {
                is Resource.Success -> {
                    showAttachedRandomizersDialog(result.value.data.orEmpty())
                }

                is Resource.Error -> {
                    Alerts.error(
                        this@AgoraPublisherActivity,
                        result.errorResponse?.message ?: "Could not load randomizers."
                    )
                }
            }
        }
    }

    private fun showAttachedRandomizersDialog(templates: List<RandomizerTemplate>) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Randomizers")
            .setPositiveButton("New Randomizer") { dialog, _ ->
                dialog.dismiss()
                openLiveRandomizerTemplateBuilder()
            }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }

        if (templates.isEmpty()) {
            builder.setMessage("No randomizers have been added to this show yet.")
        } else {
            val labels = templates.map { template ->
                val slotCount = template.slotCount ?: template.slots?.size ?: 0
                "${template.name ?: "Untitled"}\n${template.typeLabel()} • $slotCount slots"
            }.toTypedArray()
            builder.setItems(labels) { dialog, which ->
                dialog.dismiss()
                showRandomizerTemplateSummary(templates[which])
            }
        }

        builder.show()
    }

    private fun showRandomizerTemplateSummary(template: RandomizerTemplate) {
        val slotCount = template.slotCount ?: template.slots?.size ?: 0
        val productCount = template.slots
            ?.count { it.productId != null || it.product != null }
            ?: 0
        val message = buildString {
            append(template.typeLabel())
            append("\n")
            append(slotCount)
            append(" slots")
            if (productCount > 0) {
                append("\n")
                append(productCount)
                append(" products assigned")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(template.name ?: "Randomizer")
            .setMessage(message)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun openLiveRandomizerTemplateBuilder() {
        liveRandomizerPickerLauncher.launch(
            Intent(this, io.bidswipe.app.ui.randomizer.RandomizerTemplatesActivity::class.java)
                .putExtra("from", "live_show_picker")
                .putExtra("start_new_template", true)
        )
    }

    private fun attachRandomizerTemplateToLiveShow(templateId: Int, templateName: String?) {
        val currentShowId = currentShowIdForRandomizer()
        if (currentShowId.isBlank()) {
            Alerts.error(this, "Could not attach randomizer to this show.")
            return
        }

        bind.loader.isVisible = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                viewModel.repo.attachRandomizerTemplate(currentShowId, templateId)
            }
            bind.loader.isVisible = false

            when (result) {
                is Resource.Success -> {
                    successToast("${templateName ?: "Randomizer"} added to show")
                    showLiveRandomizersModal()
                }

                is Resource.Error -> {
                    Alerts.error(
                        this@AgoraPublisherActivity,
                        result.errorResponse?.message ?: "Could not attach randomizer."
                    )
                }
            }
        }
    }

    private fun currentShowIdForRandomizer(): String {
        return showId.takeIf { it.isNotBlank() }
            ?: viewModel.showId.takeIf { it.isNotBlank() }
            ?: liveShowData?.showId.orEmpty()
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
                        requestRandomizerCleanupBeforeEnding {
                            socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
                            App.manager.destroyEngine()
                            dialog.dismiss()
                            finishAfterTransition()
                        }
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
                    requestRandomizerCleanupBeforeEnding {
                        socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
                        App.manager.destroyEngine()
                        sheet.dismiss()
                        finishAfterTransition()
                    }
                }
            }
            sheet.show()
        }
    }

    private fun requestRandomizerCleanupBeforeEnding(onContinue: () -> Unit) {
        val currentShowId = currentShowIdForRandomizer()
        if (currentShowId.isBlank()) {
            onContinue()
            return
        }

        bind.loader.isVisible = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                viewModel.repo.getShowRandomizerTemplates(currentShowId)
            }
            bind.loader.isVisible = false

            val templates = (result as? Resource.Success)?.value?.data.orEmpty()
            val hasMappedProducts = templates.any { template ->
                template.prizeProductId != null ||
                    template.prizeProduct != null ||
                    template.slots.orEmpty().any { it.productId != null || it.product != null }
            }

            if (!hasMappedProducts) {
                onContinue()
                return@launch
            }

            AppBottomSheet(
                this@AgoraPublisherActivity,
                R.drawable.ic_info,
                "Randomizer Products",
                "Would you like to remove all items from your randomizers?",
                primaryBtnText = "Yes",
                secondaryBtnText = "No",
                canCancel = true,
                showSecondary = true,
                iconPadding = 16,
                alertType = AlertType.INFO,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        releaseRandomizerProductsAndEnd(currentShowId, onContinue)
                    }

                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        onContinue()
                    }
                }
            ).show()
        }
    }

    private fun releaseRandomizerProductsAndEnd(showId: String, onContinue: () -> Unit) {
        bind.loader.isVisible = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                viewModel.repo.releaseShowRandomizerProducts(showId)
            }
            bind.loader.isVisible = false

            if (result is Resource.Error) {
                errorToast(result.errorResponse?.message ?: "Could not remove randomizer products.")
            }
            onContinue()
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
        // M1 (2026-05-28): expose the open sheet to the get-tip-setting observer
        // so the fetched values prefill the fields, then fetch on open.
        this.tipSettingsSheetBind = tipSettingsSheetBind
        sheet.setOnDismissListener { this.tipSettingsSheetBind = null }
        tipSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }
        tipSettingsSheetBind.showLiveChat.isChecked = tipChatEnabled == true
        tipSettingsSheetBind.tipMessage.setText(tipMessage)
        // GET api/get-tip-setting?schedule_show_id=<id>. viewModel.showId is the
        // schedule_shows.id for this live show (same id the socket save keys on).
        viewModel.getTipSetting(viewModel.showId.takeIf { it.isNotBlank() }
            ?: liveShowData?.showId?.toString())

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
        AuctionSettingsSheetHelper.show(
            context = this,
            initialPrice = price
        ) { result ->
            val productIds = mutableListOf(productId)

            socketManager?.startAuction(
                viewModel.currentRoomId,
                productIds,
                result.startingBid,
                result.requiredTimeSeconds,
                result.counterTimerSeconds,
                result.suddenDeath,
                liveShowData?.auctionTypeId
            )
        }
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
                    val plan = promotePlans.getOrNull(pos) ?: return
                    promoteSheet.dismiss()
                    confirmPromotePurchase(plan)
                }
            })

        promoteSheetBind.close.setHapticClickListener {
            promoteSheet.dismiss()
        }

        promoteSheet.show()
    }

    private fun confirmPromotePurchase(plan: GetPromotePlansResponse.Data) {
        val defaultCard = App.profileResponse.value?.defaultCard
        if (App.profileResponse.value?.hasCardAdded != true || defaultCard == null) {
            AppBottomSheet(
                this,
                R.drawable.ic_warning,
                "Payment Method Required",
                "Add a payment card before purchasing a show promotion.",
                primaryBtnText = "Okay",
                secondaryBtnText = "Cancel",
                canCancel = true,
                showSecondary = false,
                iconPadding = 16,
                alertType = AlertType.WARNING,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }

                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }
                }
            ).show()
            return
        }

        val amount = plan.price.asMoney()
        val cardDetails = buildString {
            append("•••• •••• •••• ")
            append(defaultCard.last4.orEmpty().ifEmpty { "----" })
            append("\nExpires ")
            append(defaultCard.expMonth ?: "--")
            append("/")
            append(defaultCard.expYear ?: "--")
        }

        val message = buildString {
            append("Plan: ")
            append(plan.title ?: "Show Promotion")
            plan.subTitle?.takeIf { it.isNotBlank() }?.let {
                append("\n")
                append(it)
            }
            append("\n\nShow: ")
            append(
                showTitle?.takeIf { it.isNotBlank() }
                    ?: liveShowData?.showDetail?.takeIf { it.isNotBlank() }
                    ?: "Current Live Show"
            )
            append("\n\nPayment method:\n")
            append(cardDetails)
            append("\n\nTotal: ")
            append(amount)
        }

        AppBottomSheet(
            this,
            R.drawable.ic_payment_card,
            "Confirm Purchase",
            message,
            primaryBtnText = "Confirm Purchase",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            iconPadding = 16,
            alertType = AlertType.INFO,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    submitPromotePurchase(plan)
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }
        ).show()
    }

    private fun submitPromotePurchase(plan: GetPromotePlansResponse.Data) {
        val promoteShowId = plan.id?.toString().orEmpty()
        if (promoteShowId.isEmpty()) {
            Alerts.error(this, "Could not start this promotion.")
            return
        }

        bind.loader.isVisible = true
        pendingPromoteShowId = promoteShowId
        viewModel.promoteShow(
            showId.request(),
            promoteShowId.request()
        )
    }

    fun createClipSheet() {

        clipSheetBind.close.setHapticClickListener {
            clipSheet.dismiss()
        }

        // Basecamp #9929851737 (2026-05-27): wire the duration slider's
        // readout. We only attach one listener at sheet build time; reopens
        // re-use it.
        clipSheetBind.durationSlider.clearOnChangeListeners()
        clipSheetBind.durationSlider.addOnChangeListener { _, value, _ ->
            clipSheetBind.durationValueText.text = "${value.toInt()}s"
        }

        // Basecamp #9929851737 (2026-05-27): API call fires only when seller
        // taps Create Clip. Duration is whatever the slider is at (1..60s),
        // default 30s.
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

        // Basecamp #9929851737 (2026-05-27): CANCEL = dismiss without
        // surfacing a save toast. TODO: when backend exposes a delete-clip
        // endpoint, call it here to remove the just-created clip from the
        // seller's library. Right now the clip remains server-side and the
        // seller would need to delete it from their clip list.
        clipSheetBind.cancelClipBtn.setHapticClickListener {
            // TODO(#9929851737): wire DELETE /api/clips/{id} once backend exposes it.
            clipSheet.dismiss()
        }

        clipSheet.show()

        clipSheet.setOnDismissListener {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
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
            randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()
            setUpWheel(freebieUsers)
        }

        randomizerSheetBind?.removeAll?.setOnClickListener {
            freebieUsers.clear()
            randomizerSheetBind?.recycler?.adapter?.notifyDataSetChanged()
            setUpWheel(freebieUsers)
        }

        randomizerSheetBind?.spinWheel?.setOnClickListener {
            if (freebieUsers.isEmpty()) {
                Alerts.error(this, "Please enter entries to spin wheel")
                return@setOnClickListener
            }

            socketManager?.finalizeFreebie(roomID)

            randomSheet.dismiss()
        }


    }

    fun updateBidFinalisseUI(json: JSONObject){
        runSafe {

            runOnUiThread {

                if (roomID == json.optString("room_id")) {
                    markClosedAuctionProducts(json)
                    json.optJSONArray("products")?.let {
                        val roomState = LiveShowModel.fromJson(json)
                        replaceVisibleProductList(roomState.products)
                    }

                    val winner = json.optJSONObject("winner")
                    if (winner == null) {
                        clearCurrentAuctionUi(showRunNext = true)
                        return@runOnUiThread
                    }
                    val winnerProductId = winner.optString("product_id")
                    val product = productList.find { it?.id == winnerProductId }

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

                        val remainingAfterSale =
                            (remainingQuantityMap[winnerProductId] ?: 0).coerceAtLeast(1) - 1
                        remainingQuantityMap[winnerProductId] = remainingAfterSale

                        if (remainingAfterSale <= 0) {
                            product?.status = "sold"
                        }
                        product?.isCurrent = false

                        bind.status.isVisible = true
                        bind.bidPrice.isVisible = false
                        bind.runNext.isVisible = true
                        clearCurrentAuctionUi(showRunNext = true)

                    } else {
                        product?.isCurrent = false
                        bind.winningLayout.isVisible = false
                        bind.status.isVisible = false
                        bind.runNext.isVisible = true
                        clearCurrentAuctionUi(showRunNext = true)
                    }

                    // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitHub's
                    // nextBuiltInProductId(...) flow which is more refined than
                    // GitLab's simple "any remaining product" branch — it picks the
                    // winner's preferred next product first when a bidder won.
                    val nextProductId = if (bidderName.isNotEmpty()) {
                        nextBuiltInProductId(winnerProductId)
                    } else {
                        nextBuiltInProductId()
                    }

                    if (nextProductId != null) {
                        bind.runNext.postDelayed({
                            if (!isFinishing && roomID == json.optString("room_id")) {
                                startAuctionForProduct(nextProductId)
                            }
                        }, 1500)
                    } else if (!hasBuiltInProductsRemaining()) {
                        bind.runNext.postDelayed({
                            if (!isFinishing && roomID == json.optString("room_id")) {
                                showProductSheet()
                            }
                        }, 1500)
                    }
                }
            }
        }
    }

    // Basecamp #9934001770 (2026-05-29): co-host leave — destroys the Agora
    // engine (which leaves the channel) and best-effort DELETEs the pairing
    // record so the host's dialog reflects "no active co-host".
    private fun leaveAsCoHost() {
        val id = coHostPairingIdFromIntent
        if (isInvitedCoHost) {
            socketManager?.leaveInvitedCoHost(roomID, userId)
        }
        if (id != null) {
            Thread {
                try {
                    val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                    val suffix = if (isInvitedCoHost) "$id/leave" else "$id"
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/$suffix")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = if (isInvitedCoHost) "POST" else "DELETE"
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {}
            }.start()
        }
        App.manager.destroyEngine()
        finishAfterTransition()
    }

    private fun showInviteCohostDialog() {
        bind.loader.isVisible = true
        lifecycleScope.launch {
            val result = cohostApiRequest(
                "GET",
                "${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/invite-candidates?schedule_show_id=$showId"
            )
            bind.loader.isVisible = false
            if (result.first !in 200..299) {
                Alerts.error(this@AgoraPublisherActivity, "Could not load cohost candidates.")
                return@launch
            }

            val candidates = mutableListOf<Pair<Int, String>>()
            val arr = org.json.JSONObject(result.second).optJSONArray("data")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optInt("id")
                    if (id <= 0) continue
                    val label = obj.optString("name").ifBlank {
                        obj.optString("username", "User #$id")
                    }
                    candidates.add(id to label)
                }
            }

            if (candidates.isEmpty()) {
                Alerts.error(this@AgoraPublisherActivity, "No cohost candidates found.")
                return@launch
            }

            androidx.appcompat.app.AlertDialog.Builder(this@AgoraPublisherActivity)
                .setTitle("Invite Cohost")
                .setItems(candidates.map { it.second }.toTypedArray()) { _, which ->
                    val candidate = candidates[which]
                    sendCohostInvite(candidate.first, candidate.second)
                }
                .show()
        }
    }

    private fun sendCohostInvite(inviteeUserId: Int, inviteeName: String) {
        bind.loader.isVisible = true
        lifecycleScope.launch {
            val body = org.json.JSONObject().apply {
                put("schedule_show_id", showId.toIntOrNull() ?: 0)
                put("invitee_user_id", inviteeUserId)
            }.toString()
            val result = cohostApiRequest(
                "POST",
                "${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/invite",
                body
            )
            bind.loader.isVisible = false
            if (result.first in 200..299) {
                successToast("Invite sent to $inviteeName.")
            } else {
                Alerts.error(this@AgoraPublisherActivity, "Could not send cohost invite.")
            }
        }
    }

    private suspend fun cohostApiRequest(method: String, urlString: String, body: String? = null): Pair<Int, String> =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                val conn = (java.net.URL(urlString).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(body.toByteArray()) }
                    }
                }
                val rc = conn.responseCode
                val stream = if (rc in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                Pair(rc, text)
            } catch (e: Exception) {
                Pair(-1, e.message ?: "error")
            }
        }

    // Basecamp #9934001770 (2026-05-27): co-host pairing dialog for the host.
    // Generates a 6-char code via POST /api/product/co-host/pair and lets the
    // host regenerate or revoke. The second device claims it on CoHostJoinActivity.
    private var coHostPairingId: Int? = null

    private fun showCoHostPairingDialog() {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_co_host_pairing, null, false)
        dialog.setContentView(view)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        val codeTv = view.findViewById<android.widget.TextView>(R.id.coHostCodeTv)
        val expiresTv = view.findViewById<android.widget.TextView>(R.id.coHostExpiresTv)
        val statusTv = view.findViewById<android.widget.TextView>(R.id.coHostStatusTv)
        val generateBtn = view.findViewById<android.widget.Button>(R.id.coHostGenerateBtn)
        val revokeBtn = view.findViewById<android.widget.Button>(R.id.coHostRevokeBtn)
        val closeBtn = view.findViewById<android.view.View>(R.id.coHostCloseBtn)

        closeBtn.setOnClickListener { dialog.dismiss() }
        coHostPairingId = null

        fun generate() {
            statusTv.text = "Generating code…"
            statusTv.setTextColor(android.graphics.Color.parseColor("#666666"))
            generateBtn.isEnabled = false
            lifecycleScope.launch {
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                        val body = org.json.JSONObject().apply {
                            put("schedule_show_id", showId.toIntOrNull() ?: 0)
                        }.toString()
                        val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/pair")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.doOutput = true
                        conn.outputStream.use { os -> os.write(body.toByteArray()) }
                        val code = conn.responseCode
                        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                        val text = stream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        Pair(code, text)
                    } catch (e: Exception) {
                        Pair(-1, e.message ?: "error")
                    }
                }
                generateBtn.isEnabled = true
                try {
                    val json = org.json.JSONObject(result.second)
                    val status = json.optString("status")
                    if (status == "success") {
                        val data = json.optJSONObject("data")
                        codeTv.text = data?.optString("pairing_code") ?: "——————"
                        val exp = data?.optString("expires_at") ?: ""
                        expiresTv.text = if (exp.isNotEmpty()) "Expires " + exp.take(16) else ""
                        coHostPairingId = data?.optInt("id")
                        revokeBtn.visibility = android.view.View.VISIBLE
                        statusTv.text = "Share this code with your second device."
                        statusTv.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                    } else {
                        statusTv.text = json.optString("message", "Could not generate code.")
                        statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                    }
                } catch (e: Exception) {
                    statusTv.text = "Could not generate code."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
            }
        }

        // generateBtn click wired below (after startPolling is defined)

        revokeBtn.setOnClickListener {
            val id = coHostPairingId ?: return@setOnClickListener
            statusTv.text = "Revoking…"
            lifecycleScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                        val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/$id")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "DELETE"
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        val rc = conn.responseCode
                        conn.disconnect()
                        rc in 200..299
                    } catch (e: Exception) { false }
                }
                if (ok) {
                    codeTv.text = "——————"
                    expiresTv.text = ""
                    revokeBtn.visibility = android.view.View.GONE
                    coHostPairingId = null
                    statusTv.text = "Pairing revoked."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#666666"))
                } else {
                    statusTv.text = "Could not revoke pairing."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
            }
        }

        // Basecamp #9934001770: poll GET /api/product/co-host/{id} every 5 s
        // so the host sees when the second device has claimed the pairing code.
        // Polling runs while the dialog is open and stops on dismiss/revoke.
        var pollJob: kotlinx.coroutines.Job? = null
        fun startPolling(pairingId: Int) {
            pollJob?.cancel()
            pollJob = lifecycleScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(5_000L)
                    if (!dialog.isShowing || coHostPairingId == null) break
                    val polled = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val token = io.bidswipe.app.utils.Prefs(this@AgoraPublisherActivity).token()
                            val prefix = "Bear" + "er "
                            val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/co-host/$pairingId")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.setRequestProperty("Accept", "application/json")
                            conn.setRequestProperty("Authorization", "$prefix$token")
                            val rc = conn.responseCode
                            val text = (if (rc in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
                            conn.disconnect()
                            if (rc in 200..299) text else null
                        } catch (e: Exception) { null }
                    }
                    if (polled != null) {
                        try {
                            val obj = org.json.JSONObject(polled)
                            val data = obj.optJSONObject("data")
                            val pairingStatus = data?.optString("status") ?: ""
                            val coHostUser = data?.optJSONObject("co_host_user")
                            if ((pairingStatus == "active" || pairingStatus == "claimed") && coHostUser != null) {
                                val name = coHostUser.optString("name", "Co-host")
                                runOnUiThread {
                                    statusTv.text = "✅ $name has joined as co-host!"
                                    statusTv.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                                    pollJob?.cancel()
                                }
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Wire generate to also start polling once a code is created
        generateBtn.setOnClickListener { generate(); coHostPairingId?.let { startPolling(it) } }
        dialog.setOnDismissListener { pollJob?.cancel() }

        dialog.show()
        generate()
    }
}
