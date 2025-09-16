package io.bidswipe.app.utils

import android.content.Context
import android.util.Log
import io.bidswipe.app.model.LiveSocketModel
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager private constructor(
    context : Context ,
) {

    private var socket : Socket? = null
    private val appContext = context.applicationContext

    @Volatile
    private var isInitialized = false

    companion object {
        private const val TAG = "SocketManager"
        @Volatile private var instance : SocketManager? = null

        fun getInstance(context : Context) : SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager(context).also { instance = it }
            }
        }
    }

    fun initialize(serverUrl : String , queryParams : Map<String , String> = emptyMap()) {
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
                    queryParams.entries.joinToString("&") { (k , v) -> "${k}=${v}" }
                } else null
            }
            socket = IO.socket(serverUrl , opts)
            isInitialized = true
            Log.d(TAG, "Socket initialized successfully")
        } catch (e : URISyntaxException) {
            Log.e(TAG, "Failed to initialize socket: ${e.message}")
            e.printStackTrace()
        }
    }

    fun connect(onConnected : (() -> Unit)? = null , onError : ((String) -> Unit)? = null) {
        Log.d(TAG, "Attempting to connect to socket")
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected successfully")
            onConnected?.invoke()
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()?.toString() ?: "connect_error"
            Log.e(TAG, "Socket connection error: $error")
            onError?.invoke(error)
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e(TAG, "Socket connection timeout")
            onError?.invoke("connect_timeout")
        }
        socket?.connect()
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from socket")
        socket?.disconnect()
    }

    fun joinRoom(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        Log.d(TAG, "EMIT: join_room - RoomId: $roomId")
        socket?.emit("join_room" , payload)
    }

    fun leaveRoom(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        Log.d(TAG, "EMIT: leave_room - RoomId: $roomId")
        socket?.emit("leave_room" , payload)
    }

    fun onBidUpdate(listener : (bidJson : JSONObject) -> Unit) {
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

    fun emitBid(roomId : String , userId : String , userName : String , userImage : String , productId : String? , bidAmount : String) {
        val payload = JSONObject().apply {
            put("roomId" , roomId)
            put("userId" , userId)
            put("userName" , userName)
            put("userImage" , userImage)
            put("productId" , productId)
            put("bidAmount" , bidAmount)
            put("timestamp" , Utils.timestamp())
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
            "EMIT: place_bid_with_livesocket - RoomId: ${liveSocket.roomId}, ShowId: ${liveSocket.showId}, ProductId: ${currentProduct.id}, BidAmount: $bidAmount"
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

    fun onViewerCount(listener : (count : Int) -> Unit) {
        socket?.on("viewer_count") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                val count = obj.optInt("count" , 0)
                Log.d(TAG, "RECEIVED: viewer_count - Count: $count")
                listener(count)
            }
        }
    }

    fun emitViewerJoin(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        Log.d(TAG, "EMIT: viewer_join - RoomId: $roomId")
        socket?.emit("viewer_join" , payload)
    }

    fun emitViewerLeave(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        Log.d(TAG, "EMIT: viewer_leave - RoomId: $roomId")
        socket?.emit("viewer_leave" , payload)
    }

    fun onMessage(listener : (message : JSONObject) -> Unit) {
        socket?.on("message") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: message - $obj")
                listener(obj)
            }
        }
    }

    fun sendMessage(roomId : String , content : String , userId : String , userName : String , userImage : String) {
        val payload = JSONObject().apply {
            put("roomId" , roomId)
            put("content" , content)
            put("userId" , userId)
            put("userName" , userName)
            put("userImage" , userImage)
            put("timestamp" , Utils.timestamp())
        }
        Log.d(TAG, "EMIT: message - RoomId: $roomId, UserId: $userId, Content: $content")
        socket?.emit("message" , payload)
    }

    fun emitTypedMessage(roomId : String , type : String , data : JSONObject) {
        val payload = JSONObject().apply {
            put("type" , type)
            put("roomId" , roomId)
            put("data" , data)
            put("timestamp" , Utils.timestamp())
        }
        Log.d(TAG, "EMIT: typed_message - RoomId: $roomId, Type: $type, Data: $data")
        socket?.emit("message" , payload)
    }

}


