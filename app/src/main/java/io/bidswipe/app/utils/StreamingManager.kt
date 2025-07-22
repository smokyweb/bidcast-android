package io.bidswipe.app.utils

import android.app.Application
import android.content.Context
import android.view.View
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import org.json.JSONObject

class StreamingManager(
    private val context: Context,
    private val application: Application
) {
    private var isEngineCreated = false
    private var isFrontCamera = true
    private var zoomLevel = 1f

    var onUserJoined: ((String) -> Unit)? = null
    var onUserLeft: ((String) -> Unit)? = null
    var onStreamError: ((String) -> Unit)? = null
    var onPublisherStateChanged: ((ZegoPublisherState, Int) -> Unit)? = null
    var onPlayerStateChanged: ((ZegoPlayerState, Int) -> Unit)? = null
    var onRoomStateChanged: ((ZegoRoomStateChangedReason, Int) -> Unit)? = null

    fun createEngine(appId: Long, appSign: String, scenario: ZegoScenario = ZegoScenario.GENERAL) {
        if (!isEngineCreated) {
            val profile = ZegoEngineProfile().apply {
                this.appID = appId
                this.appSign = appSign
                this.scenario = scenario
                this.application = application
            }
            ZegoExpressEngine.createEngine(profile, null)
            isEngineCreated = true
            setUpEventHandler()
        }
    }

    private fun setUpEventHandler() {
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {
            override fun onRoomUserUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                userList: ArrayList<ZegoUser>
            ) {
                for (user in userList) {
                    when (updateType) {
                        ZegoUpdateType.ADD -> onUserJoined?.invoke(user.userID)
                        ZegoUpdateType.DELETE -> onUserLeft?.invoke(user.userID)
                        else -> {}
                    }
                }
            }

            override fun onPublisherStateUpdate(
                streamID: String,
                state: ZegoPublisherState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                onPublisherStateChanged?.invoke(state, errorCode)
                if (errorCode != 0) {
                    onStreamError?.invoke("Publish error: $errorCode")
                }
            }

            override fun onPlayerStateUpdate(
                streamID: String,
                state: ZegoPlayerState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                onPlayerStateChanged?.invoke(state, errorCode)
                if (errorCode != 0) {
                    onStreamError?.invoke("Player error: $errorCode")
                }
            }

            override fun onRoomStateChanged(
                roomID: String,
                reason: ZegoRoomStateChangedReason,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                onRoomStateChanged?.invoke(reason, errorCode)
            }
        })
    }

    fun destroyEngine() {
        if (isEngineCreated) {
            ZegoExpressEngine.destroyEngine(null)
            isEngineCreated = false
        }
    }

    fun loginRoom(roomId: String, userId: String, userName: String, userImage: String, callback: (error: Int, extendedData: JSONObject?) -> Unit) {
        val user = ZegoUser(userId, userImage)
        val roomConfig = ZegoRoomConfig().apply { isUserStatusNotify = true }
        ZegoExpressEngine.getEngine().loginRoom(roomId, user, roomConfig, callback)
    }

    fun logoutRoom() {
        if (isEngineCreated) {
            ZegoExpressEngine.getEngine().logoutRoom()
        }
    }

    fun startPreview(view: View) {
        if (isEngineCreated) {
            val previewCanvas = ZegoCanvas(view).apply { viewMode = ZegoViewMode.ASPECT_FILL }
            ZegoExpressEngine.getEngine().startPreview(previewCanvas)
        }
    }

    fun stopPreview() {
        if (isEngineCreated) {
            ZegoExpressEngine.getEngine().stopPreview()
        }
    }

    fun startPublishingStream(streamId: String, view: View) {
        if (isEngineCreated) {
            val previewCanvas = ZegoCanvas(view).apply { viewMode = ZegoViewMode.ASPECT_FILL }
            ZegoExpressEngine.getEngine().startPreview(previewCanvas)
            ZegoExpressEngine.getEngine().startPublishingStream(streamId)
        }
    }

    fun stopPublishingStream() {
        if (isEngineCreated) {
            ZegoExpressEngine.getEngine().stopPublishingStream()
        }
    }

    fun useFrontCamera(useFront: Boolean) {
        if (isEngineCreated) {
            ZegoExpressEngine.getEngine().useFrontCamera(useFront)
            isFrontCamera = useFront
        }
    }

    fun toggleCamera() {
        useFrontCamera(!isFrontCamera)
    }

    fun setCameraZoomLevel(level: Float) {
        if (isEngineCreated) {
            zoomLevel = level
            ZegoExpressEngine.getEngine().setCameraZoomFactor(zoomLevel)
        }
    }

    fun zoomIn() {
        setCameraZoomLevel(zoomLevel + 1f)
    }

    fun muteMicrophone(mute: Boolean) {
        if (isEngineCreated) {
            ZegoExpressEngine.getEngine().muteMicrophone(mute)
        }
    }

    fun isMicrophoneMuted(): Boolean {
        return if (isEngineCreated) {
            ZegoExpressEngine.getEngine().isMicrophoneMuted
        } else false
    }

    fun isUsingFrontCamera(): Boolean = isFrontCamera
    fun getZoomLevel(): Float = zoomLevel
} 