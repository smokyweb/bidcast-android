package io.bidswipe.app.utils

import com.google.firebase.database.database
import com.google.firebase.Firebase

object FireRef {
	private val dbRef = Firebase.database.reference

	const val CHAT_LIST_REF = "chat_list"
	const val CHAT_REF = "chats"

	val CHAT_LIST = dbRef.child(CHAT_LIST_REF)
	val CHAT = dbRef.child(CHAT_REF)

	//STORAGE REFERENCES
//	private val strRef =  Firebase.storage.reference
//	val VIDEO_STR = strRef.child("chat_videos")
//	val IMAGE_STR = strRef.child("chat_images")
//	val AUDIO_STR = strRef.child("chat_audio")

}