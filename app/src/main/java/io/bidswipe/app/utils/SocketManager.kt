package io.bidswipe.app.utils

import android.content.Context
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
        @Volatile private var instance : SocketManager? = null

        fun getInstance(context : Context) : SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager(context).also { instance = it }
            }
        }
    }

    fun initialize(serverUrl : String , queryParams : Map<String , String> = emptyMap()) {
        if (isInitialized) return
        try {
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
        } catch (e : URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect(onConnected : (() -> Unit)? = null , onError : ((String) -> Unit)? = null) {
        socket?.on(Socket.EVENT_CONNECT) {
            onConnected?.invoke()
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            onError?.invoke(args.firstOrNull()?.toString() ?: "connect_error")
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            onError?.invoke("connect_timeout")
        }
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun joinRoom(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        socket?.emit("join_room" , payload)
    }

    fun leaveRoom(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        socket?.emit("leave_room" , payload)
    }

    fun onBidUpdate(listener : (bidJson : JSONObject) -> Unit) {
        socket?.on("bid_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) listener(obj)
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
        socket?.emit("place_bid" , payload)
    }

    fun onViewerCount(listener : (count : Int) -> Unit) {
        socket?.on("viewer_count") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                val count = obj.optInt("count" , 0)
                listener(count)
            }
        }
    }

    fun emitViewerJoin(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        socket?.emit("viewer_join" , payload)
    }

    fun emitViewerLeave(roomId : String) {
        val payload = JSONObject().apply { put("roomId" , roomId) }
        socket?.emit("viewer_leave" , payload)
    }

    fun onMessage(listener : (message : JSONObject) -> Unit) {
        socket?.on("message") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) listener(obj)
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
        socket?.emit("message" , payload)
    }

    fun emitTypedMessage(roomId : String , type : String , data : JSONObject) {
        val payload = JSONObject().apply {
            put("type" , type)
            put("roomId" , roomId)
            put("data" , data)
            put("timestamp" , Utils.timestamp())
        }
        socket?.emit("message" , payload)
    }

}


