package io.bidswipe.app.ui.dashboard.scheduleShow

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.isVisible
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback
import im.zego.zegoexpress.callback.IZegoRoomLoginCallback
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBarrageMessageInfo
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShareSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value
import org.json.JSONObject

class LiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    var isFrontCamera = true
    var roomID = ""
    var showId = ""
    private var liveStatus = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)

        showId= intent.getStringExtra("showId") ?:""

        createEngine()

        startListenEvent()

        fetchMessage()

        bind.more.setOnClickListener {
            showMoreSheet()
        }

        bind.promote.setOnClickListener {
            showPromoteSheet()
        }

        bind.clip.setOnClickListener {
            createClipSheet()
        }

        bind.share.setOnClickListener {
            shareSheet()
        }

        bind.cutButton.setOnClickListener {
            endShowSheet()
        }

        bind.message.setEndIconOnClickListener {

           if (bind.text.value().isNotEmpty()){
               sendMessage(bind.text.value())
           }
        }

        bind.cameraSwitch.setOnClickListener {

            if (isFrontCamera){
                ZegoExpressEngine.getEngine().useFrontCamera(false)
                isFrontCamera = false

            }else{
                ZegoExpressEngine.getEngine().useFrontCamera(true)
                isFrontCamera = true

            }

        }

        bind.loader.isVisible = true

        viewModel.generateToken(showId.request())

        bind.startBtn.setOnClickListener {

            bind.loader.isVisible = true

            viewModel.updateLiveStatus(showId.request(),"true".request())
        }

        viewModel.generateTokenRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    roomID =  mData?.roomId.toString()

                    log("ROOM ID FOR HOST: $roomID ")

                    loginRoom(mData?.userId.toString(), mData?.roomId.toString())



                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                                finish()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
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

                    if (liveStatus){
                        addDataOnFirebase(mData)


                        startPublish()
                        bind.startBtn.isVisible = false
                    }else{
                        finish()
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                                finish()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                                finish()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

    }

    fun showMoreSheet() {
        var moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu, null, false))
        var moreSheet = Alerts.appBottomSheet(this, true, moreSheetBind)


        moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        moreSheetBind.close.setOnClickListener {
            moreSheet.dismiss()
        }


        moreSheet.show()
    }

    fun showPromoteSheet() {
        var promoteSheetBind = PromoteShowSheetBinding.bind(layoutInflater.inflate(R.layout.promote_show_sheet, null, false))
        var promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)
        var mList = mutableListOf<String?>()

        repeat(3) {
            mList.add("")
        }

        promoteSheetBind.optionList.adapter = PromoteSheetAdapter( mList, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        promoteSheetBind.close.setOnClickListener {
            promoteSheet.dismiss()
        }


        promoteSheet.show()
    }

    fun createClipSheet() {
        var clipSheetBind = CreateClipSheetBinding.bind(layoutInflater.inflate(R.layout.create_clip_sheet, null, false))
        var clipSheet = Alerts.appBottomSheet(this, true, clipSheetBind)
        var mList = mutableListOf<String?>()

        repeat(3) {
            mList.add("")
        }


        clipSheetBind.close.setOnClickListener {
            clipSheet.dismiss()
        }


        clipSheet.show()
    }

    fun shareSheet() {
        var shareSheetBind = ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet, null, false))
        var shareSheet = Alerts.appBottomSheet(this, true, shareSheetBind)
        var mList = mutableListOf<String?>()

        repeat(2) {
            mList.add("")
        }

        shareSheetBind.optionList.adapter = ShareSheetAdapter( mList, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        shareSheetBind.close.setOnClickListener {
            shareSheet.dismiss()
        }

        shareSheet.show()
    }

    fun endShowSheet() {
        var endShowSheetBind = EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet, null, false))
        var endShowSheet = Alerts.appBottomSheet(this, true, endShowSheetBind)

        endShowSheetBind.close.setOnClickListener {
            endShowSheet.dismiss()
        }

        endShowSheetBind.endBtn.setOnClickListener {
            endShowSheet.dismiss()
            liveStatus = false
            bind.loader.isVisible = true
            viewModel.updateLiveStatus(showId.request(),"false".request())
        }
        endShowSheet.show()

    }

    override fun onDestroy() {
        super.onDestroy()



        Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).removeValue()



        stopPublish()
        logoutRoom()
        destroyEngine()
    }

    private fun createEngine() {
        val profile = ZegoEngineProfile().apply {
            appID = Const.APP_ID.toLong()
            appSign = Const.APP_SIGN
            scenario = ZegoScenario.GENERAL
            application = applicationContext as Application
        }

        ZegoExpressEngine.createEngine(profile, null)
    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

    private fun startListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {

            override fun onRoomStreamUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                streamList: ArrayList<ZegoStream>,
                extendedData: JSONObject
            ) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData)
                if (streamList.isNotEmpty()) {
                    val streamID = streamList[0].streamID
                    /*if (updateType == ZegoUpdateType.ADD) {
                        startPlayStream(streamID)
                    } else {
                        stopPlayStream(streamID)
                    }*/
                }
            }

            override fun onRoomUserUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                userList: ArrayList<ZegoUser>
            ) {
                super.onRoomUserUpdate(roomID, updateType, userList)
                val context = applicationContext
                for (user in userList) {
                    val message = when (updateType) {
                        ZegoUpdateType.ADD -> "${user.userID} logged in to the room."
                        ZegoUpdateType.DELETE -> "${user.userID} logged out of the room."
                        else -> ""
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onRoomStateChanged(
                roomID: String,
                reason: ZegoRoomStateChangedReason,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onRoomStateChanged(roomID, reason, errorCode, extendedData)
                val context = applicationContext
                when (reason) {
                    ZegoRoomStateChangedReason.LOGIN_FAILED ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.LOGIN_FAILED", Toast.LENGTH_LONG).show()

                    ZegoRoomStateChangedReason.RECONNECT_FAILED ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.RECONNECT_FAILED", Toast.LENGTH_LONG).show()

                    ZegoRoomStateChangedReason.KICK_OUT ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.KICK_OUT", Toast.LENGTH_LONG).show()

                    else -> {
                        // Other room states can be handled here if needed
                    }
                }
            }

            override fun onPublisherStateUpdate(
                streamID: String,
                state: ZegoPublisherState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData)
                if (errorCode != 0) {
                    // Handle publish error
                }

                if (state == ZegoPublisherState.NO_PUBLISH) {
                    Toast.makeText(applicationContext, "ZegoPublisherState.NO_PUBLISH", Toast.LENGTH_LONG).show()
                }
            }

            override fun onPlayerStateUpdate(
                streamID: String,
                state: ZegoPlayerState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData)

                if (errorCode != 0) {
                    Toast.makeText(
                        applicationContext,
                        "onPlayerStateUpdate, state: $state errorCode: $errorCode",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (state == ZegoPlayerState.NO_PLAY) {
                    Toast.makeText(applicationContext, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_LONG).show()
                }
            }

            /*override fun onIMRecvBroadcastMessage(
                roomID: String?,
                messageList: java.util.ArrayList<ZegoBroadcastMessageInfo?>?
            ) {
                super.onIMRecvBroadcastMessage(roomID, messageList)
                Log.d("ZEGO", "Barrage message received for room: $roomID")
                if (messageList != null) {
                    for (msg in messageList) {
                        Log.d("CHAT", "Received message from ${msg?.fromUser?.userName}: ${msg?.message}")
                        // Update UI accordingly
                    }
                }
            }*/
        })
    }

    private fun stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null)
    }

    fun loginRoom(liveUserId : String, roomId : String) {
        val user = ZegoUser(liveUserId,"LIVE TEST USER")
        val roomConfig = ZegoRoomConfig()
        // The `onRoomUserUpdate` callback can be received only when
        // `ZegoRoomConfig` in which the `isUserStatusNotify` parameter is set to
        // `true` is passed.
        roomConfig.isUserStatusNotify = true
        ZegoExpressEngine.getEngine().loginRoom(
            roomId,
            user,
            roomConfig,
            IZegoRoomLoginCallback { error: Int, extendedData: JSONObject? ->
                // Room login result. This callback is sufficient if you only need to
                // check the login result.
                if (error == 0) {
                    // Login successful.
                    // Start the preview and stream publishing.
                    Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()

                    startPreview()

                } else {
                    // Login failed. For details, see [Error codes\|_blank](/404).
                    Toast.makeText(this, "Login failed. error = " + error, Toast.LENGTH_LONG).show()
                }
            })
    }

    fun logoutRoom() {
        ZegoExpressEngine.getEngine().logoutRoom()
    }

    fun startPreview() {
        val previewCanvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
        ZegoExpressEngine.getEngine().startPreview(previewCanvas)
    }

    fun stopPreview() {
        ZegoExpressEngine.getEngine().stopPreview()
    }

    fun startPublish() {
        // After calling the `loginRoom` method, call this method to publish streams.
        // The StreamID must be unique in the room.
        val previewCanvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
        ZegoExpressEngine.getEngine().startPreview(previewCanvas)
        val streamID: String = roomID
        ZegoExpressEngine.getEngine().startPublishingStream(streamID)
    }

    fun stopPublish() {
        ZegoExpressEngine.getEngine().stopPublishingStream()
    }

    fun sendMessage(message: String){

        ZegoExpressEngine.getEngine().sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
            override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
                if (errorCode == 0) {
                    Log.d("CHAT", "Message sent successfully")
                } else {
                    Log.e("CHAT", "Failed to send message")
                }
            }
        })

    }

    fun fetchMessage() {

        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler(){

            override fun onIMRecvBroadcastMessage(
                roomID: String?,
                messageList: kotlin.collections.ArrayList<ZegoBroadcastMessageInfo?>?
            ) {
                Log.d("ZEGO", "Broadcast message received for room: $roomID")
                if (messageList != null) {
                    for (msgInfo in messageList) {
                        Log.d("BROADCAST", "Received broadcast message from ${msgInfo?.fromUser?.userName}: ${msgInfo?.message}")
                        // Update UI for broadcast messages
                    }
                }
            }

            override fun onIMRecvBarrageMessage(
                roomID: String?,
                messageList: kotlin.collections.ArrayList<ZegoBarrageMessageInfo?>?
            ) {
                Log.d("ZEGO", "Barrage message received for room: $roomID")
                if (messageList != null) {
                    for (msg in messageList) {
                        Log.d("CHAT", "Received message from ${msg?.fromUser?.userName}: ${msg?.message}")
                        // Update UI accordingly
                    }
                }
            }
        })
    }

    fun addDataOnFirebase(data: UpdateLiveStatusResponse.Data?){

        val prod = data?.products?.get(0)

        val user = data?.user

        val product = LiveShowModel.Product(category = prod?.categoryId.toString(), id = prod?.id?.toInt(), image = (prod?.images?.get(0) ?:"").toString(),name=prod?.title , price =prod?.pricing?.toDouble())

        val seller = LiveShowModel.Seller(
            id =user?.id.toString(),
            image = user?.profileImage,
            isFollowed = false,
            name = user?.name,
            rating = user?.rating ?:""
        )

        val a= LiveShowModel(
            product = product,
            roomId = roomID,
            seller = seller,
            showDetail = "",
            thumbnail = data?.thumbnail?.get(0) ?:"",
            viewerCount = "",
            highestBid = "",
            isLive = true,
            time = Utils.getTimeFromTimestamp(System.currentTimeMillis()/1000,"yyyy-MM-dd_HH:mm:ss_a"),
            showId = showId
            )

        Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).setValue(a).addOnCompleteListener {

        }


    }

}