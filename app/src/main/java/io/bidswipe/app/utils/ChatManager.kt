package io.bidswipe.app.utils

import android.app.Application
import org.json.JSONObject
import im.zego.zim.ZIM
import im.zego.zim.callback.ZIMEventHandler
import im.zego.zim.callback.ZIMMessageSentFullCallback
import im.zego.zim.entity.ZIMAppConfig
import im.zego.zim.entity.ZIMError
import im.zego.zim.entity.ZIMMediaMessage
import im.zego.zim.entity.ZIMMessage
import im.zego.zim.entity.ZIMMessageReceivedInfo
import im.zego.zim.entity.ZIMMultipleMessage
import im.zego.zim.entity.ZIMRoomInfo
import im.zego.zim.entity.ZIMTextMessage
import im.zego.zim.entity.ZIMUserInfo
import im.zego.zim.enums.ZIMConversationType
import im.zego.zim.enums.ZIMErrorCode
import im.zego.zim.enums.ZIMMessagePriority
import im.zego.zim.enums.ZIMRoomEvent
import im.zego.zim.enums.ZIMRoomState

class ChatManager(
	private val application : Application ,
	private val appId : Long ,
	private val appSign : String ,
	private val userId : String ,
	private val userName : String ,
	private val userImage : String ,
) {

	interface Listener {
		fun onMessageReceived(message : ZIMTextMessage)
		fun onRoomStateChanged(state : String)
	}

	private var zim : ZIM? = null
	private var listener : Listener? = null

	fun setListener(listener : Listener?) {
		this.listener = listener
	}

	fun initializeAndLogin(roomId : String , callback : () -> Unit) {
		if (zim == null) {
			val appConfig = ZIMAppConfig().also {
				it.appID = appId
				it.appSign = appSign
			}
			zim = ZIM.create(appConfig , application)
		}

		val userInfo = ZIMUserInfo().also {
			it.userID = userName.replace(" " , ".") + "_" + userId
			it.userName = userImage
		}

		zim?.login(userInfo) { error ->
			if (error != null) {
				// Login success
				createOrJoinRoom(roomId) {
					callback.invoke()
				}
			} else {
				// Login error
				listener?.onRoomStateChanged("login_error")
			}
		}
	}

	private fun createOrJoinRoom(roomId : String , callback : () -> Unit) {
		val roomInfo = ZIMRoomInfo().also {
			it.roomID = roomId
			it.roomName = roomId + "_room"
		}

		zim?.createRoom(roomInfo) { _ , errorInfo ->
			when (errorInfo.code) {
				ZIMErrorCode.SUCCESS -> {
					attachEventHandler()
					listener?.onRoomStateChanged("room_created")
					callback.invoke()
				}

				ZIMErrorCode.THE_ROOM_ALREADY_EXISTS -> {
					joinExistingRoom(roomId) {
						callback.invoke()
					}
				}

				else -> {
					listener?.onRoomStateChanged("room_create_failed:${errorInfo.code}")
				}
			}
		}
	}

	private fun joinExistingRoom(roomId : String , callback : () -> Unit) {
		ZIM.getInstance().joinRoom(roomId) { _ , joinError ->
			if (joinError.code == ZIMErrorCode.SUCCESS) {
				attachEventHandler()
				listener?.onRoomStateChanged("room_joined")
				callback.invoke()
			} else {
				listener?.onRoomStateChanged("room_join_failed:${joinError.code}")
			}
		}
	}

	private fun attachEventHandler() {
		ZIM.getInstance().setEventHandler(object : ZIMEventHandler() {
			override fun onRoomMessageReceived(
				zim : ZIM? ,
				messageList : ArrayList<ZIMMessage?>? ,
				info : ZIMMessageReceivedInfo? ,
				fromRoomID : String? ,
			) {
				super.onRoomMessageReceived(zim , messageList , info , fromRoomID)
				messageList?.forEach { message ->
					if (message is ZIMTextMessage) {
						listener?.onMessageReceived(message)
					}
				}
			}

			override fun onRoomStateChanged(
				zim : ZIM? ,
				state : ZIMRoomState? ,
				event : ZIMRoomEvent? ,
				extendedData : JSONObject? ,
				roomID : String? ,
			) {
				super.onRoomStateChanged(zim , state , event , extendedData , roomID)
				listener?.onRoomStateChanged(state?.name ?: "unknown")
			}
		})
	}

	fun sendTextMessage(roomId : String , content : String , extendedDataJson : String) {
		val message = ZIMTextMessage(content)
		message.extendedData = extendedDataJson

		val config = im.zego.zim.entity.ZIMMessageSendConfig().also { it.priority = ZIMMessagePriority.HIGH }

		zim?.sendMessage(
			message ,
			roomId ,
			ZIMConversationType.ROOM ,
			config ,
			object : ZIMMessageSentFullCallback {
				override fun onMessageAttached(message : ZIMMessage?) {}

				override fun onMessageSent(message : ZIMMessage? , errorInfo : ZIMError?) {
					if (errorInfo != null) {
						// success
						if (message is ZIMTextMessage) {
							listener?.onMessageReceived(message)
						}
					} else {
						listener?.onRoomStateChanged("send_error:${errorInfo.toString()}")
					}
				}

				override fun onMediaUploadingProgress(message : ZIMMediaMessage? , currentFileSize : Long , totalFileSize : Long) {}

				override fun onMultipleMediaUploadingProgress(
					message : ZIMMultipleMessage? ,
					currentFileSize : Long ,
					totalFileSize : Long ,
					messageInfoIndex : Int ,
					currentIndexFileSize : Long ,
					totalIndexFileSize : Long ,
				) {
				}
			}
		)
	}

	fun leaveAllRoomsAndLogout() {
		zim?.leaveAllRoom { _ , _ -> }
		zim?.logout()
	}

	fun shutdown() {
		leaveAllRoomsAndLogout()
		zim?.destroy()
		zim = null
	}
}
