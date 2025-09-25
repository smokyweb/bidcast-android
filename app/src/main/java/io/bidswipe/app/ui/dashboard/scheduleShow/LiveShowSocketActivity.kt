package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraAccessException
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.millicast.Core
import com.millicast.Media
import com.millicast.Media.audioSources
import com.millicast.Media.videoSources
import com.millicast.Publisher
import com.millicast.devices.source.audio.MicrophoneAudioSource
import com.millicast.devices.source.video.CameraVideoSource
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import com.millicast.devices.type.SwitchCameraHandler
import com.millicast.publishers.Credential
import com.millicast.publishers.Option
import com.millicast.publishers.state.PublisherConnectionState
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.ProductSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.databinding.ShowConfirmationAlertBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.model.PromoteShowModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.getValue

class LiveShowSocketActivity : BaseActivity() {

    private val bind by bind(ActivityLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private var socketManager: SocketManager? = null

    private var roomID: String = ""
    private var socketUrl: String = ""
    private lateinit var pipParams: PictureInPictureParams
    private lateinit var commentAdapter: CommentAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var durationRunnable: Runnable
    private var commentList = mutableListOf<LiveChatModel?>()
    var isFrontCamera = false
    var showId = ""
    var showTime = ""
    private var startTimeMillis: Long = 0L
    private var zoomLevel = 1.0f
    private var liveData: LiveShowModelOld? = null
    private var liveShowData: LiveShowModel? = null

    private lateinit var publisher: Publisher
    private lateinit var eglBase: EglBase

    private var audioSource: MicrophoneAudioSource? = null
    private var videoSource: CameraVideoSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var publisherStateJob: Job? = null
    private lateinit var cameraManager: CameraManager
    private var currentCameraId: String? = null
    private var maxZoom: Float = 1.0f
    private var minZoom: Float = 1.0f

    @SuppressLint("ClickableViewAccessibility")
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

        initPip()
        initCameraManager()

        liveShowData = intent.getSerializableExtra("showData") as LiveShowModel

        showId = liveShowData?.showId ?: ""
        showTime = intent.getStringExtra("time") ?: ""

        roomID = "live_room_${userId}_${showId}"

        log("LIVE SHOW DATA : $liveShowData ")

        publisher = Core.createPublisher()

        initRenderer()

        connectPublisher()

        bind.startBtn.setHapticClickListener {
            showConfirmationAlert()
        }

        commentAdapter = CommentAdapter(commentList)
        bind.recycler.adapter = commentAdapter

        socketUrl = Const.SOCKET_URL //intent.getStringExtra("socketUrl") ?: ""
        initializeSocket()

        bind.hostName.text = userName
        bind.hostImage.loadUrl(this, userImage)

        bind.controls.setHapticClickListener {
            hideKeyboard()
        }

        bind.recycler.setOnTouchListener { view, event ->
            hideKeyboard()
            return@setOnTouchListener false
        }

        bind.clip.isVisible = App.profileResponse.value?.preferences?.enableClips == true

        bind.hostImage.loadUrl(this, userImage)

        bind.message.setEndIconOnClickListener {
            if (bind.text.value().isNotEmpty()) {
                socketManager?.sendMessage(roomID, bind.text.value(), userId, userName, userImage)
                bind.text.text.clear()
            }
        }

        bind.more.setHapticClickListener {
            showMoreSheet()
        }

        bind.promote.setHapticClickListener {
            showPromoteSheet()
        }

        bind.clip.setHapticClickListener {
            createClipSheet()
        }

        bind.share.setHapticClickListener {
            shareSheet()
        }

        bind.cutButton.setHapticClickListener {

            log("STATE: ${publisher.isPublishing}")

            if (publisher.isPublishing) {
                endShowSheet()
            } else {
                stopStreaming()
                // Delay finish to allow microphone release
                handler.postDelayed({
                    finishAfterTransition()
                }, 200)
            }
        }

        bind.cameraSwitch.setHapticClickListener {
            videoSource?.switchCamera(object : SwitchCameraHandler {
                override fun onCameraSwitchDone(p0: Boolean) {
                    isFrontCamera = !isFrontCamera
                    bind.hostView.setMirror(isFrontCamera)
                }

                override fun onCameraSwitchError(p0: String?) {

                }
            })
        }

        bind.shop.setHapticClickListener {
            if (publisher.isPublishing) {
                showProductSheet()
            } else {
                Alerts.error(this, "Please start live show to access this feature")
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    enterPictureInPictureMode(pipParams)
                } else {
                    finishAfterTransition()
                }
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        log("Publisher cleanup starting")
        
        // Socket cleanup
        runSafe {
            socketManager?.emitEndRoom(roomID)
            socketManager?.leaveRoom(roomID, userId)
            socketManager?.disconnect()
        }

        // Cancel publisher state monitoring job first
        runSafe {
            publisherStateJob?.cancel()
            publisherStateJob = null
        }

        // Stop and release audio source safely
        runSafe {
            audioSource?.let { source ->
                try {
                    source.stopCapture()
                } catch (e: Exception) {
                    log("Error stopping audio capture: ${e.message}")
                }
                try {
                    source.release()
                } catch (e: Exception) {
                    log("Error releasing audio source: ${e.message}")
                }
            }
            audioSource = null
        }

        // Stop and release video source safely
        runSafe {
            videoSource?.let { source ->
                try {
                    source.stopCapture()
                } catch (e: Exception) {
                    log("Error stopping video capture: ${e.message}")
                }
                try {
                    source.release()
                } catch (e: Exception) {
                    log("Error releasing video source: ${e.message}")
                }
            }
            videoSource = null
        }

        // Disable audio track safely
        runSafe {
            audioTrack?.let { track ->
                try {
                    track.setEnabled(false)
                    track.setVolume(0.0)
                } catch (e: Exception) {
                    log("Error disabling audio track: ${e.message}")
                }
            }
            audioTrack = null
        }

        // Remove video sink safely
        runSafe {
            videoTrack?.let { track ->
                try {
                    track.removeVideoSink(bind.hostView)
                } catch (e: Exception) {
                    log("Error removing video sink: ${e.message}")
                }
            }
            videoTrack = null
        }

        // Unpublish and disconnect publisher safely
        runSafe {
            if (::publisher.isInitialized) {
                viewModel.viewModelScope.launch {
                    try {
                        publisher.unpublish()
                    } catch (e: Exception) {
                        log("Error unpublishing: ${e.message}")
                    }
                    try {
                        publisher.disconnect()
                    } catch (e: Exception) {
                        log("Error disconnecting publisher: ${e.message}")
                    }
                }
            }
        }

        // Clean up video view safely
        runSafe {
            try {
                bind.hostView.clearImage()
            } catch (e: Exception) {
                log("Error clearing video view: ${e.message}")
            }
            try {
                bind.hostView.release()
            } catch (e: Exception) {
                log("Error releasing video view: ${e.message}")
            }
        }

        // Release EGL base safely
        runSafe {
            if (::eglBase.isInitialized) {
                try {
                    eglBase.release()
                } catch (e: Exception) {
                    log("Error releasing EGL base: ${e.message}")
                }
            }
        }

        // Force cleanup
        forcedCleanup()

        // Give system time to release microphone
        handler.postDelayed({
            System.gc()
        }, 100)
        
        log("Publisher cleanup finished")
    }

    fun initPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibleRect = Rect()
            bind.root.getGlobalVisibleRect(visibleRect)

            pipParams = PictureInPictureParams.Builder().apply {
                setAspectRatio(Rational(100, 200))
                setSourceRectHint(visibleRect)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }.build()

            setPictureInPictureParams(pipParams)
        }
    }

//	private fun initializeStreaming() {
//		streamingManager = StreamingManager.getInstance(this)
//		streamingManager?.createEngine(Const.APP_ID.toLong(), Const.APP_SIGN, ZegoScenario.GENERAL)
//	}

    private fun startLiveDurationTimer() {
        startTimeMillis = System.currentTimeMillis()

        durationRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / (1000 * 60)) % 60
                val hours = (elapsed / (1000 * 60 * 60))

                val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                bind.duration.text = buildString {
                    append("Show Time: ")
                    append(formatted)
                }

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(durationRunnable)
    }

    private fun stopLiveDurationTimer() {
        if (this::durationRunnable.isInitialized) {
            handler.removeCallbacks(durationRunnable)
        }
    }

    private fun stopStreaming() {
        try {
            // Disable audio track first
            audioTrack?.setEnabled(false)
            audioTrack?.setVolume(0.0)

            // Stop audio capture
            audioSource?.stopCapture()

            // Stop video capture
            videoSource?.stopCapture()

            // Unpublish from publisher
            viewModel.viewModelScope.launch {
                try {
                    publisher.unpublish()
                } catch (e: Exception) {
                    log("Error unpublishing: ${e.message}")
                }
            }

            // Force cleanup since Millicast's release() does nothing
            forcedCleanup()

            log("Streaming stopped successfully")
        } catch (e: Exception) {
            log("Error stopping stream: ${e.message}")
        }
    }

    private fun forcedCleanup() {
        try {
            // Force garbage collection to clean up native resources
            System.gc()

            // Clear references
            audioTrack = null
            videoTrack = null
            audioSource = null
            videoSource = null

            log("Forced cleanup completed")
        } catch (e: Exception) {
            log("Error in forced cleanup: ${e.message}")
        }
    }

    private fun initializeSocket() {
        log("SOCKET URL $socketUrl")
        if (socketUrl.isEmpty()) return

        socketManager = SocketManager.getInstance(this)
        socketManager?.initialize(socketUrl, mapOf("uid" to userId))
        socketManager?.connect(onConnected = {
            socketManager?.joinRoom(roomID, userId) {

                socketManager?.sendMessage(
                    roomID,
                    "Joined \uD83D\uDC4B",
                    userId,
                    userName,
                    userImage
                )

            }
//			socketManager?.emitViewerJoin(roomID)
        }) { err ->
            log("Socket connect error: $err")
        }

        socketManager?.onViewerCount { count ->
            runSafe { bind.liveCount.text = count.toString() }
        }

        socketManager?.onMessage { msg ->

            log("MESSAGE : $msg")

            if (msg.optString("roomId") == roomID) {
                runOnUiThread {
                    commentList.add(
                        LiveChatModel(
                            msg.optString("userImage"),
                            msg.optString("userName"),
                            msg.optString("userId"),
                            msg.optString("content")
                        )
                    )
                    commentAdapter.notifyItemInserted(commentList.size - 1)
                    bind.recycler.scrollToPosition(commentList.size - 1)
                }
            }
        }
    }

    // Product selection (simplified socket mirroring)
    private fun showProductSheet() {
        val productSheetBind =
            ProductSheetBinding.bind(layoutInflater.inflate(R.layout.product_sheet, null, false))
        val sheet = Alerts.appBottomSheet(this, true, productSheetBind)
        // Expect server to push product list via a message; here we show only UI shell
        productSheetBind.close.setHapticClickListener { sheet.dismiss() }
        productSheetBind.addBtn.setHapticClickListener {
            // Notify server that host set a product live
            socketManager?.sendMessage(roomID, "set_current_product", userId, userName, userImage)
            sheet.dismiss()
        }
        sheet.show()
    }

    private fun endShowSheet() {
        val endShowSheetBind =
            EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet, null, false))
        val sheet = Alerts.appBottomSheet(this, true, endShowSheetBind)
        endShowSheetBind.close.setHapticClickListener { sheet.dismiss() }
        endShowSheetBind.endBtn.setHapticClickListener {
            sheet.dismiss()
            socketManager?.sendMessage(roomID, "end_show", userId, userName, userImage)
            stopStreaming()
            // Delay finish to allow microphone release
            handler.postDelayed({
                finishAfterTransition()
            }, 200)
        }
        sheet.show()
    }

    fun addShowData(data: LiveShowModel) {


        /*val user = data?.user

        val products = data?.products?.map { it?.toLiveShowProduct() }

        products?.first()?.isCurrent = true

        val showData = LiveShowModel(
            seller = LiveShowModel.Seller(
                id = user?.id.toString(),
                image = user?.profileImage,
                name = user?.name,
                rating = user?.rating ?: ""
            ),
            products = products?.map { p ->
                LiveShowModel.Product(
                    p?.category,
                    p?.id,
                    p?.image,
                    p?.status,
                    p?.name,
                    p?.price,
                    "1",
                )
            }?.toList() ?: mutableListOf(),
            roomId = roomID,
            showDetail = "Test Details",
            thumbnail = data?.thumbnail?.getOrNull(0) ?: "",
            viewerCount = 1,
            highestBid = LiveShowModel.HighestBid(
                bidAmount = "",
                userName = "",
                userImage = "",
                userId = "",
                productId = ""
            ),
            isLive = true,
            time = Utils.timestamp().toString(),
            showId = showId,
            allowBidForAll = true,
            bidCountDown = "",
            showTimer = "",
        )*/


        socketManager?.createRoom(roomID, data)

        socketManager?.onRoomCreated { obj ->
//			startLiveDurationTimer()
        }

        socketManager?.onDurationUpdate { obj ->

            if (roomID == obj.optString("room_id")) {
                bind.duration.text = buildString {
                    append("Show Time: ")
                    append(obj.optString("elapsed"))
                }
            }

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
                            if (publisher.isPublishing) {
                                endShowSheet()
                                moreSheet.dismiss()
                            } else {
                                finishAfterTransition()
                            }
                        }

                        else -> {

                        }
                    }

                }
            })

        moreSheetBind.allowVerifiedUser.setOnCheckedChangeListener { view, isChecked ->

//			FireRef.LIVE_SESSIONS.child(roomID).updateChildren(mapOf("allowBidForAll" to !isChecked))

        }

        log(liveData?.allowBidForAll.toString())

        moreSheetBind.allowVerifiedUser.isChecked = liveData?.allowBidForAll == false

        if (audioSource?.isCapturing == true) {
            moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
        } else {
            moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
        }

        moreSheetBind.zoomInLayout.setHapticClickListener {
            zoomIn()
            moreSheet.dismiss()
        }

        moreSheetBind.micLayout.setHapticClickListener {
            if (audioSource?.isCapturing == true) {
                audioSource?.stopCapture()
                moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
            } else {
                audioSource?.startCapture()
                moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
            }
        }

        moreSheetBind.zoomOut.setHapticClickListener {
            zoomOut()
            moreSheet.dismiss()
        }

        moreSheetBind.close.setHapticClickListener {
            moreSheet.dismiss()
        }

        moreSheet.show()
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
        val mList = mutableListOf(
            PromoteShowModel(
                "15 Minute Boost",
                "Quick visibility boost",
                "Get featured in the top shows for 15 minutes",
                "$3.99",
                listOf(R.color.boost_15_start, R.color.boost_15_end),
                R.drawable.ic_flash
            ),
            PromoteShowModel(
                "Full Show Promote",
                "Extended visibility",
                "Stay featured for your entire show duration",
                "$7.99",
                listOf(R.color.boost_full_start, R.color.boost_full_end),
                R.drawable.ic_star
            ),
            PromoteShowModel(
                "Community Boost",
                "Power of the crowd",
                "Rally your community for massive exposure",
                "$12.99",
                listOf(R.color.boost_community_start, R.color.boost_community_end),
                R.drawable.ic_people
            )
        )

        promoteSheetBind.optionList.adapter = PromoteSheetAdapter(mList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

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

    fun shareSheet() {
        val shareSheetBind =
            ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet, null, false))
        val shareSheet = Alerts.appBottomSheet(this, true, shareSheetBind)
        val mList = mutableListOf<String?>()

        repeat(2) {
            mList.add("")
        }

        /* shareSheetBind.optionList.adapter = ShareSheetAdapter(mList , object : RecyclerClicks {
 
             override fun itemClick(pos : Int , status : String?) {

             }
         })

         shareSheetBind.close.setHapticClickListener {
             shareSheet.dismiss()
         }*/

        shareSheet.show()
    }

    fun showConfirmationAlert() {

        val showConfirmationSheetBind = ShowConfirmationAlertBinding.bind(
            layoutInflater.inflate(
                R.layout.show_confirmation_alert,
                null,
                false
            )
        )
        val showConfirmationSheet = Alerts.appAlert(this, true, showConfirmationSheetBind)

        showConfirmationSheetBind.timing.text =
            "Show Starts at ${Utils.getFormattedDateTime("HH:mm:ss", "hh:mm a", showTime)}"

        showConfirmationSheetBind.startBtn.setHapticClickListener {
            showConfirmationSheet.dismiss()
//			bind.loader.isVisible = true
//			connectPublisher()
            bind.startBtn.isVisible = false
            bind.message.setMargins(
                resources.dpToPx(16),
                resources.dpToPx(16),
                resources.dpToPx(16),
                navigationBarHeight
            )

            addShowData(liveShowData!!)
        }

        showConfirmationSheet.show()

    }

    private fun initRenderer() {
        eglBase = EglBase.create()
        bind.hostView.init(eglBase.eglBaseContext, null)
        bind.hostView.setMirror(false)
        bind.hostView.setEnableHardwareScaler(true)
        bind.hostView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
    }

    private fun connectPublisher() {
        // Start preparing media sources (mic + camera)
        readyPublishingSources { aTrack, vTrack ->

            log("CONNECTING PUBLISHER")

            if (aTrack == null && vTrack == null) {
                log("Failed to initialize audio/video tracks.")
                return@readyPublishingSources
            }

            viewModel.viewModelScope.launch {
                try {
                    log("CONNECTING PUBLISHER")
                    aTrack?.let { publisher.addTrack(it) }
                    vTrack?.let {
                        publisher.addTrack(it)
                        // Show local preview
                        it.setVideoSink(bind.hostView)
                    }

                    val credentials = Credential(
                        streamName = roomID,//Const.ACCOUNT_ID,
                        token = Const.PUBLISHING_TOKEN,
                        apiUrl = "https://director.millicast.com/api/director/publish"
                    )

                    publisher.setCredentials(credentials)

                    publisher.connect()

                    // Publish once connected
                    publisherStateJob?.cancel()
                    publisherStateJob = viewModel.viewModelScope.launch {
                        publisher.state
                            .map { it.connectionState }
                            .distinctUntilChanged()
                            .collect { state ->
                                if (state == PublisherConnectionState.Connected) {
                                    val videoCodecs = Media.supportedVideoCodecs
                                    val audioCodecs = Media.supportedAudioCodecs

                                    val options = Option(
                                        videoCodec = videoCodecs.firstOrNull(),
                                        audioCodec = audioCodecs.firstOrNull(),
                                        dtx = true,
                                        stereo = true
                                    )

                                    bind.loader.isVisible = false
                                    publisher.publish(options)
                                }
                            }
                    }

                } catch (e: Exception) {
                    bind.loader.isVisible = false
                    log("CONNECTING PUBLISHER ERROR : ${e.localizedMessage}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun readyPublishingSources(callback: (AudioTrack?, VideoTrack?) -> Unit) {
        audioTrack = try {
            audioSource = audioSources<MicrophoneAudioSource>().firstOrNull()
            audioSource?.startCapture()  // This DOES return AudioTrack, but could be null if audioSource is null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        videoTrack = try {
            videoSource = videoSources<CameraVideoSource>().first()

            val capabilities = videoSource?.capabilities ?: emptyList()
            if (capabilities.isNotEmpty()) {
                // Prefer a reasonable preview size to avoid giant frames
                val preferred = capabilities.firstOrNull { it.width <= 1280 && it.height <= 720 }
                    ?: capabilities.last()
                videoSource?.setCapability(preferred)
            }

            videoSource?.startCapture()  // This DOES return VideoTrack, but could be null if videoSource is null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        log("AUDIO TRACK : ${audioTrack?.name} || VIDEO TRACK : ${videoTrack?.name}")

        callback(audioTrack, videoTrack)
    }

    private fun initCameraManager() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            // Get available cameras and find front/back camera IDs
            val cameraIds = cameraManager.cameraIdList

            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_FRONT || currentCameraId == null) {
                    currentCameraId = cameraId

                    // Get zoom range
                    val zoomRange =
                        characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
                    maxZoom = zoomRange ?: 1.0f
                    minZoom = 1.0f

                    log("Camera ID: $cameraId, Max Zoom: $maxZoom")
                    break
                }
            }
        } catch (e: CameraAccessException) {
            log("Camera access error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Zoom functionality
    private fun zoomIn() {
        try {
            if (zoomLevel < maxZoom) {
                zoomLevel = (zoomLevel + 0.5f).coerceAtMost(maxZoom)
                applyVisualZoom(zoomLevel)
                log("Zoom in: $zoomLevel")
            } else {
                log("Maximum zoom level reached: $maxZoom")
            }
        } catch (e: Exception) {
            log("Zoom in error: ${e.message}")
        }
    }

    private fun zoomOut() {
        try {
            if (zoomLevel > minZoom) {
                zoomLevel = (zoomLevel - 0.5f).coerceAtLeast(minZoom)
                applyVisualZoom(zoomLevel)
                log("Zoom out: $zoomLevel")
            } else {
                log("Minimum zoom level reached: $minZoom")
            }
        } catch (e: Exception) {
            log("Zoom out error: ${e.message}")
        }
    }

    private fun applyVisualZoom(zoom: Float) {
        try {
            // Apply visual zoom by scaling the video view
            // This provides visual feedback but doesn't affect the actual camera zoom
            bind.hostView.scaleX = zoom
            bind.hostView.scaleY = zoom

            // Center the scaled view
            bind.hostView.pivotX = bind.hostView.width / 2f
            bind.hostView.pivotY = bind.hostView.height / 2f

            log("Applied visual zoom: ${zoom}x to video view")
        } catch (e: Exception) {
            log("Visual zoom error: ${e.message}")
        }
    }

	
}