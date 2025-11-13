package io.bidswipe.app.utils

import android.content.Context
import android.util.Log
import io.bidswipe.app.model.LiveShowModel
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager private constructor(
	context: Context
) {

	private var socket: Socket? = null

	@Volatile
	private var isInitialized = false

	// Session state to avoid duplicate emits/listeners
	@Volatile
	private var currentRoomId: String? = null

	@Volatile
	private var hasJoinedRoom: Boolean = false

	@Volatile
	private var viewerJoinEmitted: Boolean = false

	// De-duplicate incoming chat messages within a sliding window
	private val recentMessageKeys: ArrayDeque<String> = ArrayDeque()
	private val recentMessageSet: HashSet<String> = HashSet()
	private val recentMessageCapacity: Int = 200

	companion object {
		private const val TAG = "SocketManager"

		@Volatile
		private var instance: SocketManager? = null

		fun getInstance(context: Context): SocketManager {
			return instance ?: synchronized(this) {
				instance ?: SocketManager(context).also { instance = it }
			}
		}
	}

	fun initialize(serverUrl: String, queryParams: Map<String, String> = emptyMap()) {
		if (isInitialized) {
			Log.d(TAG, "Socket already initialized")
			return
		}
		try {
			Log.d(TAG, "Initializing socket with URL: $serverUrl")
			val opts = IO.Options().apply {
				reconnection = true
				reconnectionAttempts = Int.MAX_VALUE
				reconnectionDelay = 1000
				forceNew = true
				query = if (queryParams.isNotEmpty()) {
					queryParams.entries.joinToString("&") { (k, v) -> "${k}=${v}" }
				} else null
			}
			socket = IO.socket(serverUrl, opts)
			isInitialized = true
			Log.d(TAG, "Socket initialized successfully")
		} catch (e: URISyntaxException) {
			Log.e(TAG, "Failed to initialize socket: ${e.message}")
			e.printStackTrace()
		}
	}

	fun connect(onConnected: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
		Log.d(TAG, "Attempting to connect to socket")
		// Remove previous listeners to prevent duplicates
		socket?.off(Socket.EVENT_CONNECT)
		socket?.off(Socket.EVENT_CONNECT_ERROR)

		socket?.on(Socket.EVENT_CONNECT) {
			Log.d(TAG, "Socket connected successfully")
			onConnected?.invoke()
		}

		socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
			val error = args.firstOrNull()?.toString() ?: "connect_error"
			Log.e(TAG, "Socket connection error: $error")
			onError?.invoke(error)
		}
		/* socket?.on(Socket.EVENT_CONNECT_TIMEOUT) {
			 Log.e(TAG, "Socket connection timeout")
			 onError?.invoke("connect_timeout")
		 }*/
		socket?.connect()
	}

	fun disconnect() {
		Log.d(TAG, "Disconnecting from socket")
		try {
			socket?.off() // remove all listeners to avoid future duplicates
		} catch (_: Throwable) {
		}
		socket?.disconnect()
		// Reset session flags
		currentRoomId = null
		hasJoinedRoom = false
		viewerJoinEmitted = false
	}

	fun createRoom(liveShowData: LiveShowModel) {
		val payload = JSONObject().apply {}
		Log.d(TAG, "EMIT: room_created - RoomId: $payload")
		liveShowData.products.first()?.isCurrent = true
		socket?.emit("room_create", liveShowData.toJson())
	}

	fun onRoomCreated(listener: (bidJson: JSONObject) -> Unit) {

		socket?.off("room_create_get")
		socket?.on("room_create_get") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: create_room_get - $obj")
				listener(obj)
			}
		}

	}

	fun joinRoom(roomId: String, userId: String, listener: (liveShowJson: JSONObject) -> Unit) {
		// Avoid duplicate join for the same room in the same session
		if (hasJoinedRoom && currentRoomId == roomId) {
			Log.d(TAG, "joinRoom skipped; already joined RoomId: $roomId")
			return
		}

		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("user_id", userId)
		}
		Log.d(TAG, "EMIT: join_room - RoomId: $roomId")

		socket?.emit("join_room", payload)
		listener(payload)
	}

	fun leaveRoom(roomId: String, userId: String) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("user_id", userId)
		}
		Log.d(TAG, "EMIT: leave_room - RoomId: $roomId")
		socket?.emit("leave_room", payload)
	}

	fun onDurationUpdate(listener: (timerJson: JSONObject) -> Unit) {
		socket?.off("show_timer_update")
		socket?.on("show_timer_update") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: show_timer_update - $obj")
				listener(obj)
			}
		}
	}

	fun emitEndRoom(roomId: String) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
		}
		Log.d(TAG, "EMIT: endRoom - RoomId: $payload")
		socket?.emit("endRoom", payload)
	}

	fun onRoomEnded(listener: (JSONObject) -> Unit) {
		socket?.off("roomEnded")
		socket?.on("roomEnded") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: roomEnded - $obj")
				listener(obj)
			}
		}
	}

	fun emitBid(
		roomId: String,
		userId: String,
		userName: String,
		userImage: String,
		productId: String?,
		bidAmount: String
	) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("bid_amount", bidAmount)
			put("user_name", userName)
			put("user_image", userImage)
			put("user_id", userId)
			put("product_id", productId)
		}

		Log.d(
			TAG,
			"EMIT: place_bid - RoomId: $roomId, UserId: $userId, BidAmount: $bidAmount, ProductId: $productId"
		)
		socket?.emit("place_bid", payload)
	}

	fun getBidTimerUpdate(listener: (json: JSONObject) -> Unit) {
		socket?.on("bid_timer_update") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: bid_timer_update - $obj")
				listener(obj)
			}
		}
	}

	fun getHighestBid(listener: (json: JSONObject) -> Unit) {
		socket?.on("get_highest_bid") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: get_highest_bid - $obj")
				listener(obj)
			}
		}
	}

	fun getBidFinalize(listener: (bidJson: JSONObject) -> Unit) {
		socket?.on("bid_finalized") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: bid_finalized - $obj")
				listener(obj)
			}
		}
	}

	fun setNextProduct(
		roomId: String,
		productId: String?
	) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("product_id", productId)
		}

		Log.d(
			TAG,
			"EMIT: set_next_product - RoomId: $roomId, ProductId: $productId"
		)
		socket?.emit("set_next_product", payload)
	}

	fun getUpdatedProduct(listener: (json: JSONObject) -> Unit) {
		socket?.on("next_product_set") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: next_product_set - $obj")
				listener(obj)
			}
		}
	}

	fun updateLiveShowStatus(roomId: String) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
		}

		Log.d(
			TAG,
			"EMIT: liveScheduler - RoomId: $roomId"
		)

		socket?.emit("liveScheduler", payload)
	}

	fun updateAllowBidForAll(roomId: String, allowBidForAll: Boolean) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("allow_bid_for_all", allowBidForAll)
		}

		Log.d(
			TAG,
			"EMIT: allow_bid_for_all - RoomId: $roomId"
		)

		socket?.emit("allow_bid_for_all", payload)
	}

	fun onAllowBidForAllUpdate(listener: (json: JSONObject) -> Unit) {
		socket?.on("allow_bid_for_all_get") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: allow_bid_for_all_get - $obj")
				listener(obj)
			}
		}
	}

	fun createRaid(sourceRoomId: String, targetRoomId: String, sourceHostId: String, targetHostId: String) {
		val payload = JSONObject().apply {
			put("source_room_id", sourceRoomId)
			put("target_room_id", targetRoomId)
			put("source_host_id", sourceHostId)
			put("target_host_id", targetHostId)
		}
		socket?.emit("createRaid", payload)
	}

	fun receiveRaid(listener: (json: JSONObject) -> Unit) {
		socket?.on("receiveRaid") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: receiveRaid - $obj")
				listener(obj)
			}
		}
	}

	/**
	 * Place a bid using LiveSocketModel structure
	 */

	fun onViewerCount(listener: (count: JSONObject) -> Unit) {
		socket?.on("viewerCount") { args ->
			val obj = args.firstOrNull()
			if (obj is JSONObject) {
				Log.d(TAG, "RECEIVED: next_product_set - $obj")
				listener(obj)
			}
		}
	}

	fun onMessage(listener: (message: JSONObject) -> Unit) {
		// Avoid duplicate message handlers on reconnect or re-entry
		socket?.off("chat_get")
		socket?.on("chat_get") { args ->
			val obj = args.firstOrNull() as? JSONObject ?: return@on

			// Extract required fields once
			val roomId = obj.optString("roomId")
			val userId = obj.optString("userId")
			val content = obj.optString("content")
			val timestamp = obj.optString("timestamp")

			// Build a unique key to detect duplicates
			val key = "$roomId|$userId|$content|$timestamp"

			synchronized(this) {
				if (recentMessageSet.contains(key)) {
					true
				} else {
					recentMessageSet.add(key)
					recentMessageKeys.addLast(key)

					if (recentMessageKeys.size > recentMessageCapacity) {
						val oldest = recentMessageKeys.removeFirst()
						recentMessageSet.remove(oldest)
					}
					false
				}
			}

//            if (isDuplicate) {
//                Log.d(TAG, "RECEIVED: duplicate message - $obj")
//                return@on
//            }

			Log.d(TAG, "RECEIVED: new message - $obj")
			listener(obj)
		}
	}

	fun sendMessage(
		roomId: String,
		content: String,
		userId: String,
		userName: String,
		userImage: String
	) {
		val payload = JSONObject().apply {
			put("room_id", roomId)
			put("message", content)
			put("user_id", userId)
			put("user_name", userName)
			put("user_image", userImage)
		}
		Log.d(TAG, "EMIT: message - $payload")
		socket?.emit("chat", payload)
	}

	fun followSeller(
		followerId: String,
		followingId: String
	) {
		val payload = JSONObject().apply {
			put("follower_id", followerId)
			put("following_id", followingId)
		}
		Log.d(TAG, "EMIT: Follow Seller - $payload")
		socket?.emit("follow_unfollow", payload)
	}

	fun onFollowSellerStatus(listener: (message: JSONObject) -> Unit) {
		// Avoid duplicate message handlers on reconnect or re-entry
		socket?.off("user_follow_status")
		socket?.on("user_follow_status") { args ->
			val obj = args.firstOrNull() as? JSONObject ?: return@on

			Log.d(TAG, "RECEIVED: Seller Follow Status - $obj")
			listener(obj)
		}
	}

}


