package io.bidswipe.app.utils

import android.content.Context
import android.util.Log
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.LiveSocketModel
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager private constructor(
    context: Context,
) {

    private var socket: Socket? = null
    private val appContext = context.applicationContext

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

        // Ensure we only have one handler
        // socket?.off("join_room")

    }

    fun leaveRoom(roomId: String, userId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }
        Log.d(TAG, "EMIT: leave_room - RoomId: $roomId")
        socket?.emit("leave_room", payload)
    }

    fun onBidUpdate(listener: (bidJson: JSONObject) -> Unit) {
        socket?.on("bid_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: bid_update - $obj")
                listener(obj)
            }
        }
    }

    /**
     * Listen for live show updates
     */

    fun onLiveShowUpdate(listener: (liveShowJson: JSONObject) -> Unit) {
        socket?.on("live_show_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: live_show_update - $obj")
                listener(obj)
            }
        }
    }

    /**
     * Listen for product status changes
     */
    fun onProductStatusUpdate(listener: (productJson: JSONObject) -> Unit) {
        socket?.on("product_status_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: product_status_update - $obj")
                listener(obj)
            }
        }
    }

    /**
     * Listen for current product changes
     */

    fun onCurrentProductChange(listener: (productJson: JSONObject) -> Unit) {
        socket?.on("current_product_change") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: current_product_change - $obj")
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
            put("roomId", roomId)
            put("userId", userId)
            put("userName", userName)
            put("userImage", userImage)
            put("productId", productId)
            put("bidAmount", bidAmount)
            put("timestamp", Utils.timestamp())
        }
        Log.d(
            TAG,
            "EMIT: place_bid - RoomId: $roomId, UserId: $userId, BidAmount: $bidAmount, ProductId: $productId"
        )
        socket?.emit("place_bid", payload)
    }

    /**
     * Place a bid using LiveSocketModel structure
     */

    fun emitBidWithLiveSocketModel(
        liveSocket: LiveSocketModel,
        userId: String,
        userName: String,
        userImage: String,
        bidAmount: String
    ) {
        val currentProduct = liveSocket.products.find { it?.isCurrent == true }
        if (currentProduct == null) {
            Log.w(TAG, "No current product found in live socket for bidding")
            return
        }

        val payload = JSONObject().apply {
            put("room_id", liveSocket.roomId)
            put("show_id", liveSocket.showId)
            put("user_id", userId)
            put("user_name", userName)
            put("user_image", userImage)
            put("product_id", currentProduct.id)
            put("product_name", currentProduct.name)
            put("bid_amount", bidAmount)
            put("timestamp", Utils.timestamp())
            put("allow_bid_for_all", liveSocket.allowBidForAll)
        }

        Log.d(
            TAG,
            "EMIT: place_bid_with_liveSocket - RoomId: ${liveSocket.roomId}, ShowId: ${liveSocket.showId}, ProductId: ${currentProduct.id}, BidAmount: $bidAmount"
        )
        socket?.emit("place_bid", payload)
    }

    /**
     * Update highest bid in LiveSocketModel structure
     */
    fun emitHighestBidUpdate(
        liveSocket: LiveSocketModel,
        highestBid: LiveSocketModel.HighestBid
    ) {
        val payload = JSONObject().apply {
            put("room_id", liveSocket.roomId)
            put("show_id", liveSocket.showId)
            put("highest_bid", highestBid.toJson())
            put("timestamp", Utils.timestamp())
        }

        Log.d(
            TAG,
            "EMIT: highest_bid_update - RoomId: ${liveSocket.roomId}, BidAmount: ${highestBid.bidAmount}, UserId: ${highestBid.userId}"
        )
        socket?.emit("highest_bid_update", payload)
    }

    /**
     * Emit product status change (e.g., sold, live, etc.)
     */
    fun emitProductStatusChange(
        liveSocket: LiveSocketModel,
        product: LiveSocketModel.Product,
        newStatus: String
    ) {
        val payload = JSONObject().apply {
            put("room_id", liveSocket.roomId)
            put("show_id", liveSocket.showId)
            put("product_id", product.id)
            put("product_name", product.name)
            put("old_status", product.status)
            put("new_status", newStatus)
            put("timestamp", Utils.timestamp())
        }

        Log.d(
            TAG,
            "EMIT: product_status_change - RoomId: ${liveSocket.roomId}, ProductId: ${product.id}, Status: $newStatus"
        )

        socket?.emit("product_status_change", payload)
    }

    /**
     * Emit current product change
     */
    fun emitCurrentProductChange(
        liveSocket: LiveSocketModel,
        newCurrentProduct: LiveSocketModel.Product
    ) {
        val payload = JSONObject().apply {
            put("room_id", liveSocket.roomId)
            put("show_id", liveSocket.showId)
            put("new_current_product", newCurrentProduct.toJson())
            put("timestamp", Utils.timestamp())
        }

        Log.d(
            TAG,
            "EMIT: current_product_change - RoomId: ${liveSocket.roomId}, NewProductId: ${newCurrentProduct.id}"
        )
        socket?.emit("current_product_change", payload)
    }

    /**
     * Emit live show state update
     */
    fun emitLiveShowStateUpdate(
        liveSocket: LiveSocketModel,
        isLive: Boolean
    ) {
        val payload = JSONObject().apply {
            put("room_id", liveSocket.roomId)
            put("show_id", liveSocket.showId)
            put("is_live", isLive)
            put("timestamp", Utils.timestamp())
        }

        Log.d(TAG, "EMIT: live_show_state_update - RoomId: ${liveSocket.roomId}, IsLive: $isLive")
        socket?.emit("live_show_state_update", payload)
    }

    /**
     * Emit complete LiveSocketModel update
     */


    fun emitLiveSocketUpdate(liveSocket: LiveSocketModel) {
        val payload = liveSocket.toJson()
        Log.d(
            TAG,
            "EMIT: live_socket_update - RoomId: ${liveSocket.roomId}, ShowId: ${liveSocket.showId}"
        )
        socket?.emit("live_socket_update", payload)
    }

    /**
     * Listen for complete LiveSocketModel updates
     */
    fun onLiveSocketUpdate(listener: (liveSocket: LiveSocketModel) -> Unit) {
        socket?.on("live_socket_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                try {
                    val liveSocket = LiveSocketModel.fromJson(obj)
                    Log.d(TAG, "RECEIVED: live_socket_update - RoomId: ${liveSocket.roomId}")
                    listener(liveSocket)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing live_socket_update: ${e.message}")
                }
            }
        }
    }

    fun onViewerCount(listener: (count: Int) -> Unit) {
        socket?.on("viewerCount") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                val count = obj.optInt("count", 0)
                Log.d(TAG, "RECEIVED: viewer_count - Count: $count")
                listener(count)
            }
        }
    }

    /*fun emitViewerJoin(roomId: String) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        Log.d(TAG, "EMIT: viewer_join - RoomId: $roomId")
        socket?.emit("viewer_join", payload)
    }*/

    fun emitViewerLeave(roomId: String) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        Log.d(TAG, "EMIT: viewer_leave - RoomId: $roomId")
        socket?.emit("viewer_leave", payload)
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

            val isDuplicate = synchronized(this) {
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

    fun emitTypedMessage(roomId: String, type: String, data: JSONObject) {
        val payload = JSONObject().apply {
            put("type", type)
            put("roomId", roomId)
            put("data", data)
            put("timestamp", Utils.timestamp())
        }
        Log.d(TAG, "EMIT: typed_message - RoomId: $roomId, Type: $type, Data: $data")
        socket?.emit("message", payload)
    }

    fun createRoom(roomId: String, liveShowData: LiveShowModel) {
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

}


