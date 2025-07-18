package io.bidswipe.app.model

import com.google.firebase.database.DataSnapshot
import io.bidswipe.app.utils.Chats
import io.bidswipe.app.utils.Utils


data class ChatModel(
	var isReply : Boolean? = null,
	var id : String? = "",
	var message : String? = "",
	var seen : Boolean? = false,
	var users : Users? = Users(),
	var replyMessage : Reply? = null,
	var timezone : String? = Utils.timezone,
	var type : String? = Chats.ChatType.TEXT,
	var attachment : Attachment? = Attachment(),
	var timestamp : Long? = Utils.timestamp(),
) {
	data class Attachment(
		var audio : String? = "" ,
		var image : String? = "" ,
		var video : String? = "" ,
		var thumbnail : String? = "" ,
	) {
		fun toMap() = mapOf(
			"audio" to audio ,
			"image" to image ,
			"video" to video ,
			"thumbnail" to thumbnail
		)

		fun fromMap(it : DataSnapshot) = Attachment(
			"" + it.child("audio").getValue(String::class.java) ,
			"" + it.child("image").getValue(String::class.java) ,
			"" + it.child("video").getValue(String::class.java) ,
			"" + it.child("thumbnail").getValue(String::class.java) ,
		)
	}

	data class Reply(
		var type : String? = Chats.ChatType.TEXT ,
		var message : String? = "" ,
		var senderId : String? = "" ,
		var senderName : String? = "" ,
		var messageId : String? = "" ,
	) {
		fun toMap() = mapOf(
			"type" to type ,
			"message" to message ,
			"senderId" to senderId ,
			"senderName" to senderName ,
			"messageId" to messageId ,
		)

		fun fromMap(it : DataSnapshot) = Reply(
			"" + it.child("type").getValue(String::class.java) ,
			"" + it.child("message").getValue(String::class.java) ,
			"" + it.child("senderId").getValue(String::class.java) ,
			"" + it.child("senderName").getValue(String::class.java) ,
			"" + it.child("messageId").getValue(String::class.java) ,
		)
	}

	data class Users(
		var senderId : String? = "" ,
		var senderName : String? = "" ,
		var senderImage : String? = "" ,
		var receiverId : String? = "" ,
		var receiverName : String? = "" ,
		var receiverImage : String? = "" ,
	) {
		fun toMap() = mapOf(
			"senderId" to senderId ,
			"senderName" to senderName ,
			"senderImage" to senderImage ,
			"receiverId" to receiverId ,
			"receiverName" to receiverName ,
			"receiverImage" to receiverImage
		)

		fun fromMap(it : DataSnapshot) = Users(
			"" + it.child("senderId").getValue(String::class.java) ,
			"" + it.child("senderName").getValue(String::class.java) ,
			"" + it.child("senderImage").getValue(String::class.java) ,
			"" + it.child("receiverId").getValue(String::class.java) ,
			"" + it.child("receiverName").getValue(String::class.java) ,
			"" + it.child("receiverImage").getValue(String::class.java) ,
		)
	}

	fun toMap() = mapOf(
		"id" to id ,
		"type" to type ,
		"seen" to seen ,
		"message" to message ,
		"timezone" to timezone ,
		"timestamp" to timestamp ,
		"isReply" to isReply ,
		"users" to users?.toMap() ,
		"replyMessage" to replyMessage?.toMap() ,
		"attachment" to attachment?.toMap()
	)

	fun fromMap(it : DataSnapshot) = ChatModel(
		users = Users().fromMap(it.child("users")) ,
		id = it.child("id").getValue(String::class.java) ,
		type = it.child("type").getValue(String::class.java) ,
		seen = it.child("seen").getValue(Boolean::class.java) ,
		replyMessage = Reply().fromMap(it.child("replyMessage")) ,
		attachment = Attachment().fromMap(it.child("attachment")) ,
		message = it.child("message").getValue(String::class.java) ,
		isReply = it.child("isReply").getValue(Boolean::class.java) ,
		timezone = it.child("timezone").getValue(String::class.java) ,
		timestamp = it.child("timestamp").getValue(Long::class.java) ,
	)

}