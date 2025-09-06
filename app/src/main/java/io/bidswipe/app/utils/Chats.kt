@file:Suppress("unused" , "SetTextI18n" , "InflateParams")

package io.bidswipe.app.utils

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.bidswipe.app.databinding.ProgressAlertViewBinding
import io.bidswipe.app.model.ChatModel
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class Chats(
	mCtx : Context ,
	private val chatKey : String ,
	private val users : ChatModel.Users ,
) {
	private var progressBind = ProgressAlertViewBinding.bind(
		LayoutInflater.from(mCtx).inflate(layout.progress_alert_view , null , false)
	)
	private var alert = Alerts.appAlert(mCtx , false , progressBind)

	companion object {
		const val TAG = "CHATS"
	}

	object ChatType {
		const val IMAGE = "image"
		const val VIDEO = "video"
		const val AUDIO = "audio"
		const val TEXT = "text"
		const val DATE = "date"
		const val PDF = "pdf"
	}

	/*

		fun sendImage(imgUrl : Uri , onComplete : () -> Unit) {
			progressBind.progress.text = "0 %"
			alert.show()

			val imgRef = FireRef.IMAGE_STR.child(chatKey).child("image_" + Utils.currentTimeInFormat + ".jpeg")

			try {
				imgRef.putFile(imgUrl).addOnSuccessListener {
					imgRef.downloadUrl.addOnSuccessListener { url : Uri ->
						sendChat(ChatModel(type = ChatType.IMAGE , attachment = ChatModel.Attachment(image = url.toString())) , onComplete)
						alert.dismiss()
					}
				}.addOnProgressListener {
					val progress = 100.0 * it.bytesTransferred / it.totalByteCount
					progressBind.progress.text = "${progress.toInt()} %"
				}.addOnFailureListener {
					Alerts.error(mCtx , "${it.message}")
					alert.dismiss()
				}
			}
			catch (e : Exception) {
				e.printStackTrace()
				Alerts.error(mCtx , "${e.message}")
				alert.dismiss()
			}
		}

		fun sendAudio(audioUri : Uri , onComplete : () -> Unit) {
			progressBind.progress.text = "0 %"
			alert.show()

			val audioRef = FireRef.AUDIO_STR.child(chatKey).child("audio_" + Utils.currentTimeInFormat + ".mp3")

			try {
				audioRef.putFile(audioUri).addOnSuccessListener {
					audioRef.downloadUrl.addOnSuccessListener { url : Uri ->
						sendChat(ChatModel(type = ChatType.AUDIO , attachment = ChatModel.Attachment(audio = url.toString())) , onComplete)
						alert.dismiss()
					}
				}.addOnProgressListener {
					val progress = 100.0 * it.bytesTransferred / it.totalByteCount
					progressBind.progress.text = "${progress.toInt()} %"
				}.addOnFailureListener {
					Alerts.error(mCtx , "${it.message}")
					alert.dismiss()
				}
			}
			catch (e : Exception) {
				e.printStackTrace()
				Alerts.error(mCtx , "${e.message}")
				alert.dismiss()
			}
		}

		fun sendVideo(videoUri : Uri , thumbnail : ByteArray , onComplete : () -> Unit) {
			progressBind.progress.text = "0 %"
			alert.show()

			val videoRef = FireRef.VIDEO_STR.child(chatKey).child("video_" + Utils.currentTimeInFormat + ".mp4")

			try {
				videoRef.putFile(videoUri).addOnSuccessListener {
					videoRef.downloadUrl.addOnSuccessListener { videoUrl : Uri ->
						uploadThumbnail(thumbnail , videoUrl.toString() , onComplete)
					}
				}.addOnProgressListener {
					val progress = 100.0 * it.bytesTransferred / it.totalByteCount
					progressBind.progress.text = "${progress.toInt()} %"
				}.addOnFailureListener {
					Alerts.error(mCtx , "${it.message}")
					alert.dismiss()
				}
			}
			catch (e : Exception) {
				e.printStackTrace()
				Alerts.error(mCtx , "${e.message}")
				alert.dismiss()
			}
		}
	*/

	fun sendChat(model : ChatModel , onComplete : () -> Unit) {

		val ref = FireRef.CHAT.child(chatKey)
		val key = ref.push().key.toString()

		model.id = key
		model.users = users

		checkIfSameDay { pair ->
			if (pair.first >= 1) {
				val dateModel = model.copy(type = ChatType.DATE , message = pair.second).toMap()

				ref.child(key).setValue(dateModel).addOnSuccessListener {
					val dateChatKey = ref.push().key.toString()
					model.id = dateChatKey

					ref.child(dateChatKey).setValue(model.toMap()).addOnCompleteListener {
						if (it.isSuccessful) {
							Alerts.log(TAG , "CHAT SENT")
							addToChatList(model)
						} else {
							Alerts.log(TAG , "CHAT NOT SENT : ${it.exception?.message}")
						}
					}
				}
			} else {
				ref.child(key).setValue(model.toMap()).addOnCompleteListener {
					if (it.isSuccessful) {
						Alerts.log(TAG , "CHAT SENT")
						addToChatList(model)
					} else {
						Alerts.log(TAG , "CHAT NOT SENT : ${it.exception?.message}")
					}
				}
			}
		}

		onComplete()
	}

	private fun checkIfSameDay(check : (day : Pair<Long , String>) -> Unit) {
		FireRef.CHAT.child(chatKey).orderByKey().limitToLast(1)
			.addListenerForSingleValueEvent(object : ValueEventListener {

				@RequiresApi(Build.VERSION_CODES.O)
				override fun onDataChange(snapshot : DataSnapshot) {
					Alerts.log(TAG , "SNAPSHOT => $snapshot")

					var previousTime : Long = 0
					var timeZone = ""

					for (data in snapshot.children) {
						previousTime = data.child("timestamp").value.toString().toLong()
						timeZone = data.child("timezone").value.toString()
					}

					val format = Utils.getSimpleDate("MM-dd-yyyy").apply {
						setTimeZone(TimeZone.getTimeZone(timeZone))
					}
					val currentDate = format.format(Date().toInstant().toEpochMilli())
					val messageDate =
						format.format(Instant.ofEpochSecond(previousTime).toEpochMilli())
					val diff = format.parse(currentDate) !!.time - format.parse(messageDate) !!.time

					val days = TimeUnit.DAYS.convert(diff , TimeUnit.MILLISECONDS)

					check(Pair(days , currentDate))
				}

				override fun onCancelled(error : DatabaseError) {}
			})
	}
	/*

		private fun uploadThumbnail(thumbnail : ByteArray , videoUrl : String , onComplete : () -> Unit) {
			val thumbRef = FireRef.IMAGE_STR.child(chatKey).child("thumbnail_" + Utils.currentTimeInFormat + ".jpeg")

			thumbRef.putBytes(thumbnail).addOnSuccessListener {
				thumbRef.downloadUrl.addOnSuccessListener { thumbUrl : Uri ->
					sendChat(
						ChatModel(type = ChatType.VIDEO , attachment = ChatModel.Attachment(video = videoUrl , thumbnail = thumbUrl.toString())) ,
						onComplete
					)
					alert.dismiss()
				}
			}.addOnFailureListener {
				Alerts.error(mCtx , "${it.message}")
				alert.dismiss()
			}
		}
	*/

	private fun addToChatList(model : ChatModel) {
		model.id = chatKey
		FireRef.CHAT_LIST.child(users.receiverId.toString()).child(users.senderId.toString())
			.setValue(model.toMap())
		FireRef.CHAT_LIST.child(users.senderId.toString()).child(users.receiverId.toString())
			.setValue(model.toMap())
	}

	fun updateChatList(map : Map<String , Boolean>) {
		FireRef.CHAT_LIST.child(users.receiverId.toString()).child(users.senderId.toString())
			.updateChildren(map)
	}
}