package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
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
import com.millicast.publishers.Credential
import com.millicast.publishers.Option
import com.millicast.publishers.state.PublisherConnectionState
import im.zego.zegoexpress.constants.ZegoScenario
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
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.PromoteShowModel
import io.bidswipe.app.model.ZIMExtendedData
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.ChatManager
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.StreamingManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import kotlin.getValue

class LiveShowSocketActivity : BaseActivity() {

    private val bind by bind(ActivityLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private var streamingManager : StreamingManager? = null
    private var chatManager : ChatManager? = null
    private var socketManager : SocketManager? = null

    private var roomID : String = ""
    private var socketUrl : String = ""
    private var bidCounter = 30
    private var countdownRunning = false
    private var selectedProductId : String? = null
    private lateinit var pipParams : PictureInPictureParams
    private lateinit var commentAdapter : CommentAdapter
    private val updateStatusHandler = Handler(Looper.getMainLooper())
    private val handler = Handler(Looper.getMainLooper())
    private val bidTimerHandler = Handler(Looper.getMainLooper())
    private lateinit var durationRunnable : Runnable
    private var runnable : Runnable? = null
    private lateinit var bidRunnable : Runnable
    private var commentList = mutableListOf<LiveChatModel?>()
    private var liveStatus = true
    var isFrontCamera = true
    var showId = ""
    var showTime = ""
    private var startTimeMillis : Long = 0L
    private var zoomLevel = 1L
    private var liveData : LiveShowModel? = null

    private lateinit var publisher: Publisher
    private lateinit var eglBase: EglBase

    private var audioSource: MicrophoneAudioSource? = null
    private var videoSource: CameraVideoSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var publisherStateJob: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            supportActionBar(false)
            keyboardEnable(true)
        }

        initPip()


        /*publisher = Core.createPublisher()

        initRenderer()

        bind.startBtn.setOnClickListener {
            connectPublisher()
        }*/

        bind.message.setMargins(resources.dpToPx(16) , resources.dpToPx(16) , resources.dpToPx(16) , resources.dpToPx(16))
        bind.startBtn.setMargins(resources.dpToPx(16) , resources.dpToPx(0) , resources.dpToPx(16) , navigationBarHeight)

        commentAdapter = CommentAdapter(commentList)
        bind.recycler.adapter = commentAdapter

        showId = intent.getStringExtra("showId") ?: ""
        showTime = intent.getStringExtra("time") ?: ""

        roomID = intent.getStringExtra("roomId") ?: ""
        socketUrl = intent.getStringExtra("socketUrl") ?: ""

        bind.hostName.text = userName
        bind.hostImage.loadUrl(this , userImage)

        bind.controls.setHapticClickListener {
            hideKeyboard()
        }

        bind.recycler.setOnTouchListener { view , event ->
            hideKeyboard()
            return@setOnTouchListener false
        }

        bind.clip.isVisible = App.profileResponse.value?.preferences?.enableClips == true

        bind.hostImage.loadUrl(this , userImage)

        initializeStreaming()
        initializeSocket()

        bind.message.setEndIconOnClickListener {
            if (bind.text.value().isNotEmpty()) {
                val extended = ZIMExtendedData(userImage, userId, userName).toJson()
                chatManager?.sendTextMessage(roomID, bind.text.value(), extended)
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
            if (chatManager != null) {
                endShowSheet()
            } else {
                finishAfterTransition()
            }

        }

        bind.cameraSwitch.setHapticClickListener {
            streamingManager?.toggleCamera()
            isFrontCamera = streamingManager?.isUsingFrontCamera() ?: true
        }

        bind.shop.setHapticClickListener {

            if (chatManager != null) {
                showProductSheet()
            } else {
                Alerts.error(this , "Please start live show to access this feature")
            }

        }

        bind.loader.isVisible = true

        viewModel.generateToken(showId.request())

        bind.startBtn.setHapticClickListener {

            showConfirmationAlert()

        }

        viewModel.generateTokenRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    roomID = mData?.roomId.toString()

                    log("ROOM ID FOR HOST: $roomID ")

                    startPreview()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this , TAG , object : AlertClicks {
                            override fun primaryClick(dialog : AppBottomSheet) {
                                dialog.dismiss()
                                finish()
                            }

                            override fun secondaryClick(dialog : AppBottomSheet) {
                                dialog.dismiss()

                                finish()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.updateLiveStatusRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    bind.message.setMargins(resources.dpToPx(16) , resources.dpToPx(16) , resources.dpToPx(16) , navigationBarHeight)

                    if (liveStatus) {
                        runSafe {
                            loginAndPublish()
                            bind.startBtn.isVisible = false
                            addShowData(mData)
                        }
                    } else {
                        finish()
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this , TAG , object : AlertClicks {
                            override fun primaryClick(dialog : AppBottomSheet) {
                                dialog.dismiss()
                                finish()
                            }

                            override fun secondaryClick(dialog : AppBottomSheet) {
                                dialog.dismiss()

                                finish()
                            }
                        })
                    }
                }

                else -> {}
            }
        }

        viewModel.createBidRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    viewModel.createBidRepo.value = null

                    bind.loader.isVisible = false

                    val index = liveData?.products?.indexOfFirst { product -> product?.isCurrent == true }

                    FireRef.LIVE_SESSIONS.child(roomID).child("products").child(index.toString()).updateChildren(
                        mapOf(
                            "status" to "sold" ,
                            "isCurrent" to false
                        )
                    )

                    if ((liveData?.products?.size ?: 0) > 1) {

                        showProductSheet()

                    } else {
                        Alerts.error(this , "Your Current Product has been sold, Please select next one to your shop")
                    }

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this , TAG , object : AlertClicks {
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

        onBackPressedDispatcher.addCallback(this , object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (chatManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        enterPictureInPictureMode(pipParams)
                    }
                } else {
                    finishAfterTransition()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager?.emitViewerLeave(roomID)
        socketManager?.disconnect()
        stopPublish()
        chatManager?.shutdown()
        streamingManager?.destroyEngine()
    }

    fun initPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibleRect = Rect()
            bind.root.getGlobalVisibleRect(visibleRect)

            pipParams = PictureInPictureParams.Builder().apply {
                setAspectRatio(Rational(100 , 200))
                setSourceRectHint(visibleRect)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }.build()

            setPictureInPictureParams(pipParams)
        }
    }


    private fun initializeStreaming() {
        streamingManager = StreamingManager.getInstance(this)
        streamingManager?.createEngine(Const.APP_ID.toLong() , Const.APP_SIGN , ZegoScenario.GENERAL)
    }

    private fun initializeSocket() {
        if (socketUrl.isEmpty()) return
        socketManager = SocketManager.getInstance(this)
        socketManager?.initialize(socketUrl , mapOf("uid" to userId))
        socketManager?.connect(onConnected = {
            socketManager?.joinRoom(roomID)
            socketManager?.emitViewerJoin(roomID)
        }) { err ->
            log("Socket connect error: $err")
        }
        socketManager?.onViewerCount { count ->
            runSafe { bind.liveCount.text = count.toString() }
        }

    }

    private fun initializeChat() {

        if (chatManager == null) {
            chatManager = ChatManager(
                application = application ,
                appId = Const.APP_ID.toLong() ,
                appSign = Const.APP_SIGN ,
                userId = userId ,
                userName = userName ,
                userImage = userImage
            )
        }

        chatManager?.initializeAndLogin(roomID) {
            val extended = ZIMExtendedData(userImage , userId , userName).toJson()
            chatManager?.sendTextMessage(roomID , "Active 👋" , extended)
        }
    }

    private fun startPreview() {
        streamingManager?.startPreview(bind.hostView)
    }

    private fun loginAndPublish() {
        bind.loader.isVisible = true
        streamingManager?.loginRoom(roomID , userId , userName , userImage) { error : Int , _ : JSONObject? ->
            bind.loader.isVisible = false
            if (error == 0) {
                streamingManager?.startPublishingStream(roomID , bind.hostView)
                initializeChat()
            } else {
                errorToast("Login failed: $error")
            }
        }
    }

    private fun stopPublish() {
        streamingManager?.stopPublishingStream()
        streamingManager?.logoutRoom()
    }

    // Product selection (simplified socket mirroring)
    private fun showProductSheet() {
        val productSheetBind = ProductSheetBinding.bind(layoutInflater.inflate(io.bidswipe.app.R.layout.product_sheet , null , false))
        val sheet = io.bidswipe.app.utils.Alerts.appBottomSheet(this , true , productSheetBind)
        // Expect server to push product list via a message; here we show only UI shell
        productSheetBind.close.setHapticClickListener { sheet.dismiss() }
        productSheetBind.addBtn.setHapticClickListener {
            // Notify server that host set a product live
            socketManager?.sendMessage(roomID , "set_current_product" , userId , userName , userImage)
            sheet.dismiss()
        }
        sheet.show()
    }

    private fun endShowSheet() {
        val endShowSheetBind = EndShowSheetBinding.bind(layoutInflater.inflate(io.bidswipe.app.R.layout.end_show_sheet , null , false))
        val sheet = io.bidswipe.app.utils.Alerts.appBottomSheet(this , true , endShowSheetBind)
        endShowSheetBind.close.setHapticClickListener { sheet.dismiss() }
        endShowSheetBind.endBtn.setHapticClickListener {
            sheet.dismiss()
            socketManager?.sendMessage(roomID , "end_show" , userId , userName , userImage)
            finishAfterTransition()
        }
        sheet.show()
    }

    fun addShowData(data : UpdateLiveStatusResponse.Data?) {
        val user = data?.user

        val seller = LiveShowModel.Seller(
            id = user?.id.toString() ,
            image = user?.profileImage ,
            isFollowed = false ,
            name = user?.name ,
            rating = user?.rating ?: ""
        )

        val products = data?.products?.map { it?.toLiveShowProduct() }
        products?.first()?.isCurrent = true

        val payload = JSONObject().apply {
            put("seller" , JSONObject().apply {
                put("id" , seller.id)
                put("image" , seller.image)
                put("isFollowed" , seller.isFollowed)
                put("name" , seller.name)
                put("rating" , seller.rating)
            })

            put("products" , org.json.JSONArray().apply {
                products?.forEach { p ->
                    if (p != null) {
                        put(JSONObject().apply {
                            put("id" , p.id)
                            put("name" , p.name)
                            put("image" , p.image)
                            put("price" , p.price)
                            put("isCurrent" , p.isCurrent)
                            put("status" , p.status)
                        })
                    }
                }
            })
            put("thumbnail" , data?.thumbnail?.getOrNull(0) ?: "")
            put("viewerCount" , 1)
            put("isLive" , true)
            put("showId" , showId)
            put("time" , Utils.timestamp().toString())
        }

        log("PAYLOAD  : $payload")

//        socketManager?.emitTypedMessage(roomID , "session_update" , payload)
    }

    fun showMoreSheet() {
        val moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu , null , false))
        val moreSheet = Alerts.appBottomSheet(this , true , moreSheetBind)

        moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu , object : RecyclerClicks {

            override fun itemClick(pos : Int , status : String?) {

            }
        })

        moreSheetBind.allowVerifiedUser.setOnCheckedChangeListener { view , isChecked ->

            FireRef.LIVE_SESSIONS.child(roomID).updateChildren(mapOf("allowBidForAll" to ! isChecked))

        }

        log(liveData?.allowBidForAll.toString())

        moreSheetBind.allowVerifiedUser.isChecked = liveData?.allowBidForAll == false

        if (streamingManager?.isMicrophoneMuted() == false) {
            moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
        } else {
            moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
        }

        moreSheetBind.zoomInLayout.setHapticClickListener {
            streamingManager?.zoomIn()
            zoomLevel = streamingManager?.getZoomLevel()?.toLong() ?: 1L
            moreSheet.dismiss()
        }

        moreSheetBind.micLayout.setHapticClickListener {
            val isMuted = streamingManager?.isMicrophoneMuted() ?: false
            streamingManager?.muteMicrophone(! isMuted)

            if (! isMuted) {
                moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
            } else {
                moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
            }
        }

        moreSheetBind.zoomOut.setHapticClickListener {
            streamingManager?.zoomOut()
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
                R.layout.promote_show_sheet ,
                null ,
                false
            )
        )

        val promoteSheet = Alerts.appBottomSheet(this , true , promoteSheetBind)
        val mList = mutableListOf<PromoteShowModel>(
            PromoteShowModel(
                "15 Minute Boost" ,
                "Quick visibility boost" ,
                "Get featured in the top shows for 15 minutes" ,
                "$3.99" ,
                listOf(R.color.boost_15_start , R.color.boost_15_end) ,
                R.drawable.ic_flash
            ) ,
            PromoteShowModel(
                "Full Show Promote" ,
                "Extended visibility" ,
                "Stay featured for your entire show duration" ,
                "$7.99" ,
                listOf(R.color.boost_full_start , R.color.boost_full_end) ,
                R.drawable.ic_star
            ) ,
            PromoteShowModel(
                "Community Boost" ,
                "Power of the crowd" ,
                "Rally your community for massive exposure" ,
                "$12.99" ,
                listOf(R.color.boost_community_start , R.color.boost_community_end) ,
                R.drawable.ic_people
            )
        )

        promoteSheetBind.optionList.adapter = PromoteSheetAdapter(mList , object : RecyclerClicks {
            override fun itemClick(pos : Int , status : String?) {

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
                R.layout.create_clip_sheet ,
                null ,
                false
            )
        )
        val clipSheet = Alerts.appBottomSheet(this , true , clipSheetBind)
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
        val shareSheetBind = ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet , null , false))
        val shareSheet = Alerts.appBottomSheet(this , true , shareSheetBind)
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

        val showConfirmationSheetBind = ShowConfirmationAlertBinding.bind(layoutInflater.inflate(R.layout.show_confirmation_alert , null , false))
        val showConfirmationSheet = Alerts.appAlert(this , true , showConfirmationSheetBind)

        showConfirmationSheetBind.timing.text = "Show Starts at ${Utils.getFormattedDateTime("HH:mm:ss" , "hh:mm a" , showTime)}"

        showConfirmationSheetBind.startBtn.setHapticClickListener {
            showConfirmationSheet.dismiss()
            bind.loader.isVisible = true

            viewModel.updateLiveStatus(showId.request() , "true".request())

        }

        showConfirmationSheet.show()

    }

    /*private fun initRenderer() {
        eglBase = EglBase.create()
        bind.remoteView.init(eglBase.eglBaseContext, null)
        bind.remoteView.setMirror(false)
        bind.remoteView.setEnableHardwareScaler(true)
        bind.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
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
                        it.setVideoSink(bind.remoteView)
                    }

                    val credentials = Credential(
                        streamName = Const.ACCOUNT_ID,
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
                                    publisher.publish(options)
                                }
                            }
                    }
                } catch (e: Exception) {
                    log("CONNECTING PUBLISHER ERROR : ${e.localizedMessage}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun readyPublishingSources(callback: (AudioTrack?, VideoTrack?) -> Unit) {
        audioTrack = try {
            audioSource = audioSources<MicrophoneAudioSource>().firstOrNull()
            audioSource?.startCapture()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        videoTrack = try {
            videoSource = videoSources<CameraVideoSource>().first()

            val capabilities = videoSource?.capabilities ?: emptyList()
            if (capabilities.isNotEmpty()) {
                // Prefer a reasonable preview size to avoid giant frames
                val preferred = capabilities.firstOrNull { it.width <= 1280 && it.height <= 720 } ?: capabilities.last()
                videoSource?.setCapability(preferred)
            }

            videoSource?.startCapture()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        log("AUDIO TRACK : ${audioTrack?.name} || VIDEO TRACK : ${videoTrack?.name}")

        callback(audioTrack, videoTrack)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { publisherStateJob?.cancel() } catch (_: Throwable) {}
        try { videoTrack?.removeVideoSink(bind.remoteView) } catch (_: Throwable) {}
        try { bind.remoteView.clearImage() } catch (_: Throwable) {}
        try { audioSource?.stopCapture(); audioSource?.release() } catch (_: Throwable) {}
        try { videoSource?.stopCapture(); videoSource?.release() } catch (_: Throwable) {}
        try { viewModel.viewModelScope.launch { publisher.unpublish(); publisher.disconnect() } } catch (_: Throwable) {}
        try { bind.remoteView.release() } catch (_: Throwable) {}
        try { eglBase.release() } catch (_: Throwable) {}
    }
*/
}