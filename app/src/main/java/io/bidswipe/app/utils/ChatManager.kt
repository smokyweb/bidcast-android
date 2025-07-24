package io.bidswipe.app.utils

import android.app.Application
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.callback.ZIMMessageSentFullCallback
import im.zego.zim.entity.*
import im.zego.zim.enums.ZIMConversationType
import im.zego.zim.enums.ZIMMessagePriority
import io.bidswipe.app.model.LiveChatModel
import org.json.JSONObject

class ChatManager(
    private val application: Application,
    private val appId: Long,
    private val appSign: String
) {
    private var zim: ZIM? = null
    private var isLoggedIn = false
    private var currentRoomId: String? = null

    var onMessageReceived: ((LiveChatModel) -> Unit)? = null
    var onMessageSent: ((LiveChatModel) -> Unit)? = null

    fun initializeZIM() {
        if (zim == null) {
            val appConfig = ZIMAppConfig().also {
                it.appID = appId
                it.appSign = appSign
            }
            zim = ZIM.create(appConfig, application)
            setUpEventHandler()
        }
    }

    private fun setUpEventHandler() {
        zim?.setEventHandler(object : ZIMEventHandler() {
            override fun onRoomMessageReceived(
                zim: ZIM?,
                messageList: java.util.ArrayList<ZIMMessage?>?,
                info: ZIMMessageReceivedInfo?,
                fromRoomID: String?
            ) {
                messageList?.forEach { zimMessage ->
                    if (zimMessage is ZIMTextMessage) {
                        val model = LiveChatModel.fromZIMMessage(zimMessage)
                        onMessageReceived?.invoke(model)
                    }
                }
            }
        })
    }

    fun login(userId: String, userName: String, callback: (error: ZIMError?) -> Unit) {
        val userInfo = ZIMUserInfo().also {
            it.userID = userId
            it.userName = userName
        }
        zim?.login(userInfo) { error ->
            isLoggedIn = error == null
            callback(error)
        }
    }

    fun logout() {
        zim?.logout()
        isLoggedIn = false
    }

    fun destroy() {
        zim?.destroy()
        zim = null
        isLoggedIn = false
    }

    fun createRoom(roomId: String, roomName: String, callback: (roomInfo: ZIMRoomFullInfo?, error: ZIMError?) -> Unit) {
        val roomInfo = ZIMRoomInfo().also {
            it.roomID = roomId
            it.roomName = roomName
        }
        zim?.createRoom(roomInfo) { info, error ->
            if (error == null) {
                currentRoomId = roomId
            }
            callback(info, error)
        }
    }

    fun sendTextMessage(
        content: String,
        extendedData: String? = null,
        roomId: String
    ) {
        val zimMessage = ZIMTextMessage(content)
        if (extendedData != null) {
            zimMessage.extendedData = extendedData
        }
        val config = ZIMMessageSendConfig().also { it.priority = ZIMMessagePriority.HIGH }
        zim?.sendMessage(
            zimMessage,
            roomId,
            ZIMConversationType.ROOM,
            config,
            object : ZIMMessageSentFullCallback {
                override fun onMessageAttached(message: ZIMMessage?) {}
                override fun onMessageSent(message: ZIMMessage?, errorInfo: ZIMError?) {
                    if (errorInfo == null && message is ZIMTextMessage) {
                        onMessageSent?.invoke(LiveChatModel.fromZIMMessage(message))
                    }
                }
                override fun onMediaUploadingProgress(
                    message: ZIMMediaMessage?,
                    currentFileSize: Long,
                    totalFileSize: Long
                ) {}
                override fun onMultipleMediaUploadingProgress(
                    message: ZIMMultipleMessage?,
                    currentFileSize: Long,
                    totalFileSize: Long,
                    messageInfoIndex: Int,
                    currentIndexFileSize: Long,
                    totalIndexFileSize: Long
                ) {}
            })
    }

    fun getZIMInstance(): ZIM? = zim
    fun isUserLoggedIn(): Boolean = isLoggedIn
    fun getCurrentRoomId(): String? = currentRoomId
} 