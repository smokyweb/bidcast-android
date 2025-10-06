package io.bidswipe.app.model

import org.json.JSONObject

data class LiveChatModel(
	val userImage : String? ,
	val userName : String? ,
	val userId : String? ,
	val message : String? ,
) {
	companion object {
		/*fun fromZIMMessage(message : ZIMTextMessage) : LiveChatModel {
			val mData = ZIMExtendedData.fromJson(JSONObject(message.extendedData).toString())
			val senderImage = mData.userImage
			val senderId = mData.userId
			val senderName = mData.userName

			return LiveChatModel(
				senderImage ,
				senderName ,
				senderId ,
				message.message
			)
		}*/
	}
}
