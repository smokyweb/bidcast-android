package io.bidswipe.app.utils

import android.os.Handler
import android.os.Looper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseReference
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.network.response.UpdateLiveStatusResponse

class FirebaseLiveSessionManager(
    private val liveSessionsRef : DatabaseReference ,
) {
	private var valueEventListener : ValueEventListener? = null
	private val updateStatusHandler = Handler(Looper.getMainLooper())
	private var periodicRunnable : Runnable? = null
	private var currentRoomId : String? = null

	fun updateLiveSessionNode(roomID : String , data : UpdateLiveStatusResponse.Data?) {
		val user = data?.user
		val seller = LiveShowModelOld.Seller(
			id = user?.id.toString() ,
			image = user?.profileImage ,
			isFollowed = false ,
			name = user?.name ,
			rating = user?.rating ?: ""
		)

		val highestBid = LiveShowModelOld.HighestBid()

		val liveShow = LiveShowModelOld(
			products = data?.products?.map { it?.toLiveShowProduct() } ,
			roomId = roomID ,
			seller = seller ,
			showDetail = "" ,
			thumbnail = data?.thumbnail?.get(0) ?: "" ,
			viewerCount = 1 ,
			highestBid = highestBid ,
			isLive = true ,
			time = System.currentTimeMillis().toString() ,
			showId = data?.id.toString()
		).toMap()
		liveSessionsRef.child(roomID).updateChildren(liveShow)
	}

	fun listenToLiveSession(roomID : String , onDataChange : (LiveShowModelOld?) -> Unit) {
		removeListener()
		currentRoomId = roomID
		valueEventListener = object : ValueEventListener {
			override fun onDataChange(snapshot : DataSnapshot) {
				val data = snapshot.getValue(LiveShowModelOld::class.java)
				onDataChange(data)
			}

			override fun onCancelled(error : DatabaseError) {}
		}
		liveSessionsRef.child(roomID).addValueEventListener(valueEventListener !!)
	}

	fun removeListener() {
		if (currentRoomId != null && valueEventListener != null) {
			liveSessionsRef.child(currentRoomId !!).removeEventListener(valueEventListener !!)
			valueEventListener = null
		}
	}

	fun startPeriodicTimeUpdate(roomID : String , intervalMillis : Long = 4 * 60 * 1000) {
		stopPeriodicTimeUpdate()
		periodicRunnable = object : Runnable {
			override fun run() {
				val updateValue = System.currentTimeMillis()
				liveSessionsRef.child(roomID).updateChildren(
					mapOf(
						"time" to Utils.getTimeFromTimestamp(updateValue / 1000 , "yyyy-MM-dd_hh:mm:ss_a")
					)
				)
				updateStatusHandler.postDelayed(this , intervalMillis)
			}
		}
		periodicRunnable?.let { updateStatusHandler.post(it) }
	}

	fun stopPeriodicTimeUpdate() {
		periodicRunnable?.let { updateStatusHandler.removeCallbacks(it) }
		periodicRunnable = null
	}

	fun removeLiveSession(roomID : String) {
		liveSessionsRef.child(roomID).removeValue()
	}
} 