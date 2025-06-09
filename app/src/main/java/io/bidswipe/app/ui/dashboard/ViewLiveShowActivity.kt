package io.bidswipe.app.ui.dashboard

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoRoomLoginCallback
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoStreamResourceMode
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoPlayerConfig
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.utils.bind
import org.json.JSONObject

class ViewLiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityViewLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private var roomId  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)

        roomId = intent.getStringExtra("roomId") ?: ""

        createEngine()
        loginRoom(roomId)
        startListenEvent()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
    }

    private fun createEngine() {
        val profile = ZegoEngineProfile().apply {
            appID =  1005763407
            appSign = "73678be720c3ea2d871376882d27d21d5c2bc891363547424458f9febc8bf423"
            scenario = ZegoScenario.BROADCAST
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

                    log("STREAM ID : $streamID")
                    if (updateType == ZegoUpdateType.ADD) {
                        startPlayStream(streamID)
                    } else {
                        stopPlayStream(streamID)
                    }
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
        })
    }

    private fun stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null)
    }


    fun loginRoom(roomId : String) {
        val user = ZegoUser(userId, userName)
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


//                        startPreview()
//                        startPublish()

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

    fun startPlayStream(streamID: String?) {
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
    }

}