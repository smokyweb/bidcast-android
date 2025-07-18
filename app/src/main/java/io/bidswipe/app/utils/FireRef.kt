package io.bidswipe.app.utils

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FireRef {

	//REALTIME-DB NODES
	private val dbRef =  Firebase.database.reference

	val CHAT_LIST = dbRef.child("chat_list")
	val CHAT = dbRef.child("chats")

	//STORAGE REFERENCES
//	private val strRef =  Firebase.storage.reference

//	val VIDEO_STR = strRef.child("chat_videos")
//	val IMAGE_STR = strRef.child("chat_images")
//	val AUDIO_STR = strRef.child("chat_audio")

}