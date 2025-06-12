package io.bidswipe.app.ui.dashboard.watchStream

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBarrageMessageInfo
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {
    override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentWatchStreamBinding.inflate(inflater,view,false)

    private lateinit var roomID: String
    private lateinit var streamID: String

    companion object {
        fun newInstance(roomID: String, streamID: String) = WatchStreamFragment().apply {
            arguments = Bundle().apply {
                putString("roomID", roomID)
                putString("streamID", streamID)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomID = requireArguments().getString("roomID")!!
        streamID = requireArguments().getString("streamID")!!
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log("RoomId: $roomID")


            fetchMessage()

        bind.message.setEndIconOnClickListener {
           if (bind.text.value().isNotEmpty()) {
               sendMessage(bind.text.value())
               fetchMessage()
           }

        }



        // Or observe selectedStream if you want to react to changes
        viewModel.selectedStream.observe(viewLifecycleOwner) { stream ->
            if (stream.roomId == roomID) {

                bind.userImage.loadUrl(mCtx, stream.seller?.image.toString(), placeHolder = draw.user_image)

                bind.userName.text = stream.seller?.name.toString()
                bind.productName.text = stream.product?.name
                bind.productImage.loadUrl(mCtx, stream?.product?.image.toString() , placeHolder = draw.product_img )
                bind.quantity.text = buildString {
                    append("Price: ")
                    append(stream.product?.price.toString())
                }

                bind.max.text = stream.product?.price.toString()

                bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
                    override fun onSlideComplete(view: SlideToActView) {
                        log("SWIPED")

                        bind.loader.isVisible = true

                        viewModel.createBid(
                            stream.showId?.request(),
                            userId.request(),
                            stream.product?.id.toString().request(),
                            "100".request()
                        )

                    }
                }

            }
        }

//        createEngine()
//        loginRoom(roomID)
//        startListenEvent()

    }

    private fun createEngine() {
        val profile = ZegoEngineProfile().apply {
            appID =  1005763407
            appSign = "73678be720c3ea2d871376882d27d21d5c2bc891363547424458f9febc8bf423"
            scenario = ZegoScenario.BROADCAST
            application = mCtx.applicationContext as Application
        }

        ZegoExpressEngine.createEngine(profile, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
    }


    override fun onResume() {
        super.onResume()
        loginAndPlay()
    }

    override fun onPause() {
        super.onPause()
        stopStream()
    }

    private fun loginAndPlay() {
        val user = ZegoUser("user_${System.currentTimeMillis()}")
        ZegoExpressEngine.getEngine().loginRoom(roomID,  user,ZegoRoomConfig())
        val canvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
       /* val a= ZegoExpressEngine.getEngine().getRoomStreamList(roomID, ZegoRoomStreamListType.ALL).playStreamList.size

        bind.liveCount.text = a.toString()*/

        ZegoExpressEngine.getEngine().startPlayingStream(roomID, canvas)
    }

    private fun stopStream() {
        ZegoExpressEngine.getEngine().stopPlayingStream(roomID)
        ZegoExpressEngine.getEngine().logoutRoom(roomID)
    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

   /* private fun startListenEvent() {
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

                    startPlayStream(streamID)

                    log("STREAM ID : $streamID")
                  *//*  if (updateType == ZegoUpdateType.ADD) {

                    } else {
                        stopPlayStream(streamID)
                    }*//*
                }
            }

            override fun onRoomUserUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                userList: ArrayList<ZegoUser>
            ) {
                super.onRoomUserUpdate(roomID, updateType, userList)
                val context = mCtx
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
                val context = mCtx
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
                    Toast.makeText(mCtx, "ZegoPublisherState.NO_PUBLISH", Toast.LENGTH_LONG).show()
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
                        mCtx,
                        "onPlayerStateUpdate, state: $state errorCode: $errorCode",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (state == ZegoPlayerState.NO_PLAY) {
                    Toast.makeText(mCtx, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_LONG).show()
                }
            }
        })
    }


    private fun stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null)
    }*/


   /* fun loginRoom(roomId : String) {
        val user = ZegoUser(userId, )
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
                    Toast.makeText(mCtx, "Login successful.", Toast.LENGTH_LONG).show()


//                        startPreview()
//                        startPublish()

                } else {
                    // Login failed. For details, see [Error codes\|_blank](/404).
                    Toast.makeText(mCtx, "Login failed. error = " + error, Toast.LENGTH_LONG).show()
                }
            })
    }

    fun logoutRoom() {
        ZegoExpressEngine.getEngine().logoutRoom()
    }*/

   /* fun startPlayStream(streamID: String?) {
        bind.hostView.setVisibility(View.VISIBLE)
        val playCanvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
        val config = ZegoPlayerConfig()
        config.resourceMode = ZegoStreamResourceMode.DEFAULT // Live Streaming
        // config.resourceMode = ZegoStreamResourceMode.ONLY_L3; // Interactive Live Streaming
        ZegoExpressEngine.getEngine().startPlayingStream(streamID, playCanvas, config)
    }

    fun stopPlayStream(streamID: String?) {
        ZegoExpressEngine.getEngine().stopPlayingStream(streamID)
        bind.hostView.setVisibility(View.GONE)
    }*/

/*    fun fetchMessage(){

// Listen to incoming messages
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {
            override fun onIMRecvBroadcastMessage(
                roomID: String?,
                messageList: ArrayList<ZegoBroadcastMessageInfo?>?
            ) {
                super.onIMRecvBroadcastMessage(roomID, messageList)

                Log.d("Tag","MESSAGE RECEIVED : ${messageList}")
            }
        })
    }*/

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



}