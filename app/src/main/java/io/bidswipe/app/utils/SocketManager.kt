package io.bidswipe.app.utils

import android.content.Context
import android.icu.util.TimeZone
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.response.socket.AuctionStartedBreakSpotResponse
import io.bidswipe.app.network.response.socket.AuctionStartedResponse
import io.bidswipe.app.network.response.socket.NotLiveShowResponse
import io.bidswipe.app.network.response.socket.ProductIdsDeserializer
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager private constructor(
    context: Context
) {

    // Basecamp #9933402746 (2026-05-27): store context for prefs-backed user lookup.
    private val mContext: Context? = context.applicationContext

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

    // Basecamp #9958514184 / #9958518263 / #9958527259 (2026-06-03):
    // SHARED ROOT CAUSE for the three Android-buyer live-show realtime bugs.
    // The buyer's socket is a long-lived app-wide singleton (App.socketManager).
    // When the buyer opens a live show we emit join_room / join_show ONCE. But
    // socket.io transparently reconnects (network change, transport upgrade,
    // app backgrounding) and the server treats every reconnect as a BRAND-NEW
    // socket id with EMPTY room membership. Nothing re-emitted join_room, so the
    // reconnected socket was never socket.join(room_id)'d on the server. Since
    // chat_get, auction_started, next_product_set are all broadcast via
    // io.to(room_id).emit(...), the buyer in a reconnected/late socket received
    // NONE of them (chat cut off, no product/bid card). join_show likewise never
    // re-ran, so the buyer's show_user_joins row was missing and the seller's
    // viewer list omitted the Android buyer.
    //
    // FIX: remember the desired room/user and re-emit join_room (+ join_show when
    // the viewer-join was requested) on EVERY (re)connect, so room membership is
    // restored after any reconnect. Also gate the initial emit on the connected
    // state — if we're not connected yet, the connect listener performs the join
    // the moment the socket comes up.
    @Volatile
    private var desiredRoomId: String? = null

    @Volatile
    private var desiredUserId: String? = null

    @Volatile
    private var desiredJoinShow: Boolean = false

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

    fun  initialize(serverUrl: String, queryParams: Map<String, String> = emptyMap()) {
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

            // Basecamp #9958518263 / #9958527259 (2026-06-03, round 2): the buyer's
            // socket connects ONCE at app startup, so by the time they open a live
            // show the Socket-level EVENT_CONNECT has already fired and been
            // consumed. socket.io then performs its polling->websocket transport
            // upgrade and periodic reconnects, each of which gives the SERVER a
            // brand-new socket id with EMPTY room membership. The Socket-level
            // EVENT_CONNECT rejoin in connect() covers most cases, but to be
            // bulletproof we ALSO listen on the Manager's reconnect events (which
            // fire reliably on every transport reconnect) and re-emit join_room/
            // join_show there too. This is what makes live chat + the bidding
            // product card keep flowing for the buyer after the first reconnect.
            // Verified server-side: a socket that is actually in room_id receives
            // chat_get / auction_started; the bug was purely lost membership.
            try {
                socket?.io()?.on(Manager.EVENT_RECONNECT) {
                    Log.d(TAG, "Manager EVENT_RECONNECT -> rejoin desired room")
                    rejoinDesiredRoom()
                }
                socket?.io()?.on(Manager.EVENT_OPEN) {
                    Log.d(TAG, "Manager EVENT_OPEN -> rejoin desired room")
                    rejoinDesiredRoom()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to attach manager reconnect listeners: ${e.message}")
            }

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
            // Basecamp #9958514184 / #9958518263 / #9958527259 (2026-06-03):
            // (re)join the desired room/show on EVERY connect. This covers the
            // very first connect AND every transparent reconnect, restoring the
            // server-side socket.join(room_id) membership that broadcasts of
            // chat_get / auction_started / next_product_set depend on.
            rejoinDesiredRoom()
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

        // Basecamp #9958562821 / #9958567122 (2026-06-03): SELLER (and any screen)
        // root cause. The socket is an app-wide singleton that App.setUpSocket()
        // already connected at startup. When a live screen later calls connect()
        // again, socket.io is ALREADY connected, so socket.connect() is a no-op
        // and EVENT_CONNECT NEVER FIRES AGAIN. That meant the onConnected callback
        // (which performs joinRoom) never ran, so the seller never joined the
        // room and received none of the io.to(room_id) broadcasts -> auction card
        // never appeared on the seller's own screen, and the chat stream stayed
        // empty. Fix: if we're already connected when connect() is called, run the
        // connect path (rejoin + onConnected) immediately instead of waiting for
        // an EVENT_CONNECT that will never come.
        if (socket?.connected() == true) {
            Log.d(TAG, "connect(): socket already connected; running join path immediately")
            rejoinDesiredRoom()
            onConnected?.invoke()
        } else {
            socket?.connect()
        }
    }

    /** True when the underlying socket exists and is currently connected. */
    fun isConnected(): Boolean = socket?.connected() == true

    fun socketId(): String? = socket?.id()

    // Basecamp #9958514184 / #9958518263 / #9958527259 (2026-06-03):
    // Re-emit the join for whatever room the buyer is currently watching.
    // Called on every socket (re)connect and from joinRoom when we were not
    // yet connected. Idempotent on the server (join_room re-adds to the same
    // room/Set; join_show is an upsert with ON DUPLICATE KEY).
    private fun rejoinDesiredRoom() {
        val roomId = desiredRoomId ?: return
        val userId = desiredUserId ?: return
        Log.d(TAG, "EMIT (rejoin on connect): join_room - RoomId: $roomId")
        socket?.emit("join_room", JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        })
        hasJoinedRoom = true
        currentRoomId = roomId
        if (desiredJoinShow) {
            Log.d(TAG, "EMIT (rejoin on connect): join_show - RoomId: $roomId")
            socket?.emit("join_show", JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
            })
        }
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
        desiredRoomId = null
        desiredUserId = null
        desiredJoinShow = false
    }

    fun createRoom(liveShowData: LiveShowModel) {
        Log.d(TAG, "EMIT: room_created - RoomId: $liveShowData")
        socket?.emit("room_create", liveShowData.toJson())
    }

    fun onRoomCreated(listener: (bidJson: LiveShowModel) -> Unit) {

//        socket?.off("room_create_get")
        socket?.on("room_create_get") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: create_room_get - $obj")
                if (!obj.optBoolean("is_room_created")) {

                    val gson = GsonBuilder()
                        .registerTypeAdapter(
                            object : TypeToken<List<String>>() {}.type,
                            ProductIdsDeserializer()
                        )
                        .create()

                    val shoDataNotLive = gson.fromJson(
                        obj.toString(),
                        NotLiveShowResponse::class.java
                    )

                    val user = shoDataNotLive.seller
                    
                    val timee= Utils.getTimeStampFromServerTime( shoDataNotLive.date?.replace("00:00:00",shoDataNotLive.time?:"00:00:00")?:"", timeZone = TimeZone.getDefault().id).toString()

                    val showData = LiveShowModel(
                        seller = LiveShowModel.Seller(
                            id = user?.id.toString(),
                            image = Const.BASE_URL+"/"+(user?.profileImage ?: ""),
                            name = user?.username,
                            rating = user?.rating ?: ""
                        ),
                        products = emptyList<LiveShowModel.Product>(),
                        roomId = shoDataNotLive.roomId,
                        showDetail = shoDataNotLive?.title ?: "",
                        thumbnail = shoDataNotLive?.thumbnail ?: "",
                        viewerCount = "1",
                        highestBid = LiveShowModel.HighestBid(
                            bidAmount = "",
                            userName = "",
                            userImage = "",
                            userId = "",
                            productId = ""
                        ),
                        isLive = false,
                        time =timee.take(10),
                        showId = shoDataNotLive?.id.toString(),
                        allowBidForAll = true,
                        bidCountDown = "",
                        showTimer = "",
                        categoryId = shoDataNotLive?.categoryId.toString(),
                        auctionTypeId = shoDataNotLive.auctionTypeId
                    )
                    listener(showData)       
                } else {
                    val res = LiveShowModel.fromJson(obj)
                    listener(res)
                }
            }
        }
    }

    fun joinRoom(roomId: String, userId: String, listener: (liveShowJson: JSONObject) -> Unit) {
        // Basecamp #9958514184 / #9958518263 / #9958527259 (2026-06-03):
        // Remember the room/user we want to be in so the connect listener can
        // (re)join it after any reconnect. We intentionally DO NOT early-return
        // on "already joined" anymore: a reconnect produces a new server socket
        // with no room membership, so a repeat join_room for the same room must
        // be allowed to re-run. The server is idempotent (re-adds to the same
        // room Set), so duplicate join_room emits are harmless.
        // Reset the join_show intent when switching to a DIFFERENT room (e.g.
        // a raid). joinShow() re-sets it to true right after for buyer views.
        if (desiredRoomId != roomId) desiredJoinShow = false
        desiredRoomId = roomId
        desiredUserId = userId

        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }

        if (isConnected()) {
            Log.d(TAG, "EMIT: join_room - RoomId: $roomId")
            socket?.emit("join_room", payload)
            hasJoinedRoom = true
            currentRoomId = roomId
        } else {
            // Not connected yet — the EVENT_CONNECT handler will perform the
            // join via rejoinDesiredRoom() the moment the socket comes up.
            Log.d(TAG, "join_room deferred until connect - RoomId: $roomId")
        }
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
//                Log.d(TAG, "RECEIVED: show_timer_update - $obj")
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
//        socket?.off("roomEnded")
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
        bidAmount: String?,
        auctionTypeId:Int?
    ) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("bid_amount", bidAmount)
            put("user_name", userName)
            put("user_image", userImage)
            put("user_id", userId)
            put("product_id", productId)
            put("auction_type_id", auctionTypeId)
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

    fun onBidRejected(listener: (json: JSONObject) -> Unit) {
        socket?.off("place_bid_rejected")
        socket?.on("place_bid_rejected") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: place_bid_rejected - $obj")
                listener(obj)
            }
        }
    }

    fun onAuctionOrderFailed(listener: (json: JSONObject) -> Unit) {
        socket?.off("auction_order_failed")
        socket?.on("auction_order_failed") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_order_failed - $obj")
                listener(obj)
            }
        }

        socket?.off("auction_order_failed_break_spot")
        socket?.on("auction_order_failed_break_spot") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_order_failed_break_spot - $obj")
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

    fun onAuctionEnded(listener: (json: JSONObject) -> Unit) {
        socket?.off("auction_ended")
        socket?.on("auction_ended") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_ended - $obj")
                listener(obj)
            }
        }

        socket?.off("auction_ended_no_bid")
        socket?.on("auction_ended_no_bid") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_ended_no_bid - $obj")
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

    fun addShowNotes(roomId: String, note: String) {
        // Basecamp #9933402746 (2026-05-27 round 2): include user_id so the
        // server-side ownership check passes. Previously missing — server
        // silently dropped the update, buyers never saw the new note.
        val context = mContext
        val userId = if (context != null) {
            try { io.bidswipe.app.utils.Prefs(context).getUserData()?.id ?: 0 } catch (e: Exception) { 0 }
        } else 0
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("show_note", note)
            if (userId > 0) put("user_id", userId)
        }
        Log.d(TAG, "EMIT: SHOW NOTE  - $payload")

        socket?.emit("add_show_note", payload)
    }

    // Basecamp #9933402746 (2026-05-27 round 2): explicit on-demand fetch.
    // Buyer emits after join_room as a defensive backup to the broadcast.
    fun requestShowNote(roomId: String) {
        val payload = JSONObject().apply { put("room_id", roomId) }
        Log.d(TAG, "EMIT: request_show_note - $payload")
        socket?.emit("request_show_note", payload)
    }

    // Basecamp #9934003774 (2026-05-27 round 2): seller-side fetch — covers
    // the race where buyers joined before this seller's active_show_users
    // listener registered.
    fun requestActiveShowUsers(roomId: String) {
        val payload = JSONObject().apply { put("room_id", roomId) }
        Log.d(TAG, "EMIT: request_active_show_users - $payload")
        socket?.emit("request_active_show_users", payload)
    }

    fun receiveShowNotes(listener: (count: JSONObject) -> Unit) {
        socket?.on("get_show_note") { args ->
            Log.d(TAG, "receiveShowNotes: $args")
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                listener(obj)
            }
        }
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
                Log.d(TAG, "RECEIVED: viewerCount - $obj")
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
        followingId: String,
        showId: String
    ) {
        val payload = JSONObject().apply {
            put("follower_id", followerId)
            put("following_id", followingId)
            put("show_id", showId)
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

    fun createPoll(
        roomId: String,
        question: String,
        options: List<String>,
        duration: Int
    ) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("question", question)
            put("options", org.json.JSONArray(options))
            put("duration", duration)
        }
        Log.d(TAG, "EMIT: create_poll - RoomId: $roomId, Question: $question , Options: $options, Duration: $duration")
        socket?.emit("create_poll", payload)
    }

    fun onPollCreated(listener: (pollJson: JSONObject) -> Unit) {
        socket?.off("poll_created")
        socket?.on("poll_created") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: poll_created - $obj")
                listener(obj)
            }
        }
    }

    fun onPollUpdate(listener: (pollJson: JSONObject) -> Unit) {
        socket?.off("poll_vote_update")
        socket?.on("poll_vote_update") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: poll_vote_update - $obj")
                listener(obj)
            }
        }
    }

    fun onPollEnded(listener: (pollJson: JSONObject) -> Unit) {
        socket?.off("poll_ended")
        socket?.on("poll_ended") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: poll_ended  - $obj")
                listener(obj)
            }
        }
    }

    fun votePoll(roomId: String, pollId: Int, optionIndex: Int, userId: String) {
        val payload = JSONObject().apply {
            put("poll_id", pollId)
            put("option_index", optionIndex)
            put("user_id", userId)
            put("room_id", roomId)
        }

        Log.d(TAG, "EMIT: vote_poll - RoomId: $roomId, PollId: $pollId, OptionIndex: $optionIndex")
        socket?.emit("vote_poll", payload)
    }

    fun onPollVoteResult(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("poll_vote_result")
        socket?.on("poll_vote_result") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: poll_vote_result - $obj")
                listener(obj)
            }
        }
    }

    fun onVoteErrorResult(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("vote_error")
        socket?.on("vote_error") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: vote_error - $obj")
                listener(obj)
            }
        }
    }

    fun endPoll(roomId: String, pollId: String) {
        val payload = JSONObject().apply {
            put("poll_id", pollId)
            put("room_id", roomId)
        }

        Log.d(TAG, "EMIT: end_poll - RoomId: $roomId, PollId: $pollId")
        socket?.emit("end_poll", payload)
    }

    fun saveTipSetting(showId: String, tipMessage: String, showInLiveChat: Boolean) {
        val payload = JSONObject().apply {
            put("show_id", showId)
            put("tip_message", tipMessage)
            put("show_in_live_chat", showInLiveChat)
        }

        Log.d(TAG, "EMIT:tip_setting_save  - showId: $payload")
        socket?.emit("tip_setting_save", payload)
    }

    fun onSaveTipSettingResult(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("tip_setting_updated")
        socket?.on("tip_setting_updated") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: tipSettingUpdated - $obj")
                listener(obj)
            }
        }
    }

    fun sendTip(roomId: String, showId: String, userId: String, sellerId: String, amount: String, cardNumber: String? = null) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("show_id", showId)
            put("seller_id", sellerId)
            put("user_id", userId)
            put("amount", amount)
        }

        Log.d(
            TAG,
            "EMIT: send_tip  - showId: $showId, showId : $showId, userId : $userId, sellerId : $sellerId, amount : $amount, cardNumber: $cardNumber"
        )
        socket?.emit("send_tip", payload)
    }

    fun startAuction(
        roomId: String,
        productIds: List<String>,
        startingBidAmount: String,
        requireTime: Int?,
        counterBidTime: Int?,
        suddenDeath: Boolean?,
        auctionTypeId:Int?
    ) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("products", JSONArray(productIds))
            put("starting_bid_amount", startingBidAmount)
            put("require_time", requireTime)
            put("counter_bid_time", counterBidTime)
            put("sudden_death", suddenDeath)
            put("auction_type_id", auctionTypeId)
        }

        Log.d(TAG, "startAuction: ${payload}")
        Log.d(
            TAG,
            "EMIT:start_auction  - roomId: $roomId, productIds: $productIds, startingBidAmount: $startingBidAmount, requireTime: $requireTime, counterBidTime: $counterBidTime: $suddenDeath"
        )
        socket?.emit("start_auction", payload)
    }

    fun onAuctionStarted(listener: (resultJson: AuctionStartedResponse) -> Unit) {
        socket?.off("auction_started")
        socket?.on("auction_started") { args ->
            val obj = args.firstOrNull()
            val auctionResponse = Gson().fromJson(
                obj.toString(),
                AuctionStartedResponse::class.java
            )
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_started - $obj")
                listener(auctionResponse)
            }
        }
    }

    fun pinProduct(roomId: String, productId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("product_id", productId)
        }
        Log.d(TAG, "EMIT:pin_product  - roomId: $roomId, productId: $productId")
        socket?.emit("pin_product", payload)
    }

    fun onProductPinned(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("product_pinned")
        socket?.on("product_pinned") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: product_pinned - $obj")
                listener(obj)
            }
        }
    }

    fun onProductUnPinned(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("product_unpinned")
        socket?.on("product_unpinned") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: product_unpinned - $obj")
                listener(obj)
            }
        }
    }

    fun runNextProduct(roomId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
        }
        Log.d(TAG, "EMIT:run_next_product  - roomId: $roomId")
        socket?.emit("run_next_product", payload)
    }

    fun onAuctionNExtProduct(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("auction_next_product")
        socket?.on("auction_next_product") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_next_product - $obj")
                listener(obj)
            }
        }
    }

    fun onNextProductError(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("run_next_product_error")
        socket?.on("run_next_product_error") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: run_next_product_error - $obj")
                listener(obj)
            }
        }
    }

    fun setPromotionData(userId: String, showId: String, promoteShowId: String) {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("show_id", showId)
            put("promote_show_id", promoteShowId)
        }
        Log.d(TAG, "EMIT:set_promotion_data  - userId: $userId, showId: $showId, promoteShowId: $promoteShowId")
        socket?.emit("set_promotion_data", payload)
    }

    fun joinShow(userId: String?, roomId: String?) {
        // Basecamp #9958514184 (2026-06-03): record that the active room also
        // wants a join_show, so rejoinDesiredRoom() re-emits it on reconnect.
        // Without this the buyer's show_user_joins row was lost on reconnect and
        // the seller's viewer list omitted the Android buyer.
        if (!roomId.isNullOrEmpty()) {
            desiredRoomId = roomId
            desiredJoinShow = true
            if (!userId.isNullOrEmpty()) desiredUserId = userId
        }
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }
        if (isConnected()) {
            Log.d(TAG, "EMIT:join_show  - userId: $userId, showId: $roomId ")
            socket?.emit("join_show", payload)
        } else {
            // Deferred: rejoinDesiredRoom() on EVENT_CONNECT will emit it.
            Log.d(TAG, "join_show deferred until connect - showId: $roomId")
        }
    }

    fun sustainWatches(userId: String?, showId: String?) {

        val payload = JSONObject().apply {
            put("show_id", showId)
            put("user_id", userId)
        }

        Log.d(TAG, "EMIT:sustained_watches  - userId: $userId, showId: $showId ")

        socket?.emit("sustained_watches", payload)
    }

    fun createFreebie(roomId: String, productId: String, time: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("product_id", productId)
            put("time", time)
        }
        Log.d(TAG, "EMIT:create-freebie  - showId: $roomId, productId: $productId, time: $time")
        socket?.emit("create-freebie", payload)
    }

    // MC cmph7xsgy00g4ms8pslgxzr1u (2026-05-22): multi-product freebie
    // emit. The first id stays in `product_id` for backwards compat with
    // older clients; the full pool ships as a comma-separated `product_ids`
    // string which the patched socketEvents.js expects. The server emits a
    // chat-style notification to all room members when this lands.
    fun createFreebieMulti(roomId: String, productIds: List<String>, time: String) {
        if (productIds.isEmpty()) return
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("product_id", productIds.first())
            put("product_ids", productIds.joinToString(","))
            put("time", time)
        }
        Log.d(TAG, "EMIT:create-freebie (multi) - showId: $roomId, productIds: ${productIds.joinToString(",")}, time: $time")
        socket?.emit("create-freebie", payload)
    }

    fun enterInFreebie(roomId: String, userId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }
        Log.d(TAG, "EMIT:enter-in-freebie  - showId: $roomId, userId: $userId")
        socket?.emit("enter-in-freebie", payload)
    }

    fun getFreebie(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("get-freebie")
        socket?.on("get-freebie") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: get-freebie - $obj")
                listener(obj)
            }
        }
    }

    fun getLiveUsers(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("active_show_users")
        socket?.on("active_show_users") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: active_show_users - $obj")
                listener(obj)
            }
        }
    }

    // Basecamp #9934003774 (2026-05-27): seller emits kick_user to remove a buyer.
    // Server-side verifies caller is the show's host before persisting + executing.
    fun kickUser(roomId: String, targetUserId: Int) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("target_user_id", targetUserId)
        }
        Log.d(TAG, "EMIT: kick_user - $payload")
        socket?.emit("kick_user", payload)
    }

    // Listener for the buyer-side notification that they were kicked.
    fun onKickedFromShow(listener: (msg: String) -> Unit) {
        socket?.off("kicked_from_show")
        socket?.on("kicked_from_show") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: kicked_from_show - $obj")
                val msg = obj.optString("message", "You have been removed from this show.")
                listener(msg)
            }
        }
    }

    // Basecamp #9956272376 (2026-06-02): the server rejects a buyer's join with
    // `join_room_error` (e.g. code "kicked" when the buyer was removed from this
    // show earlier). Android had NO listener for this, so a rejected buyer's
    // `room_create_get` never arrived and they sat forever on the black loading
    // screen (same bug fixed on iOS). Surface the message + let the caller exit.
    fun onJoinRoomError(listener: (msg: String, code: String?) -> Unit) {
        socket?.off("join_room_error")
        socket?.on("join_room_error") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: join_room_error - $obj")
                val code = obj.optString("code", "").takeIf { it.isNotEmpty() }
                val msg = if (code == "kicked")
                    "You have been removed from this show by the host."
                else
                    obj.optString("message", "This show can’t be opened right now.")
                listener(msg, code)
            }
        }
    }

    // Listener for host-side kick_user confirmation.
    fun onKickUserSuccess(listener: (targetUserId: Int) -> Unit) {
        socket?.off("kick_user_success")
        socket?.on("kick_user_success") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: kick_user_success - $obj")
                listener(obj.optInt("target_user_id", 0))
            }
        }
    }

    fun finalizeFreebie(roomId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
        }
        Log.d(TAG, "EMIT:finalize-freebie  - showId: $roomId ")
        socket?.emit("finalize-freebie", payload)
    }

    fun getFreebieWinner(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("get-freebie-winner")
        socket?.on("get-freebie-winner") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: get-freebie-winner - $obj")
                listener(obj)
            }
        }
    }

    fun onFreebieSpinning(listener: (resultJson: JSONObject) -> Unit) {
        socket?.off("freebie-spinning")
        socket?.on("freebie-spinning") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: freebie-spinning - $obj")
                listener(obj)
            }
        }
    }

    fun removeFreebieUser(roomId: String, userId: String) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }
        Log.d(TAG, "EMIT:remove-freebie-user  - showId: $roomId , userId: $userId")
        socket?.emit("remove-freebie-user", payload)
    }

    //SPOT BREAK
    fun startAuctionBreakSpot(
        roomId: String,
        productSetId: String,
        productSetItemId: String,
        productSetItemUnitId: String,
        startingBidAmount: String,
        requireTime: Int?,
        counterBidTime: Int?,
        suddenDeath: Boolean?,
    ) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("productSetId", productSetId)
            put("productSetItemId", productSetItemId)
            put("productSetItemUnitId", productSetItemUnitId)
            put("starting_bid_amount", startingBidAmount)
            put("require_time", requireTime)
            put("counter_bid_time", counterBidTime)
            put("sudden_death", suddenDeath)
        }

        Log.d(TAG, "startAuctionBreakSpot: ${payload}")

        socket?.emit("start_auction_break_spot", payload)
    }

    fun onAuctionStartedBreakSpot(listener: (resultJson: AuctionStartedBreakSpotResponse) -> Unit) {
        socket?.off("auction_started_break_spot")
        socket?.on("auction_started_break_spot") { args ->
            val obj = args.firstOrNull()
            val auctionResponse = Gson().fromJson(
                obj.toString(),
                AuctionStartedBreakSpotResponse::class.java
            )
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: auction_started_break_spot - $obj")
                listener(auctionResponse)
            }
        }
    }

    fun emitBidBreakSpot(
        roomId: String,
        userId: String,
        userName: String,
        userImage: String,
        bidAmount: String?,
        breakSpotData: AuctionStartedBreakSpotResponse?
    ) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("bid_amount", bidAmount)
            put("user_name", userName)
            put("user_image", userImage)
            put("user_id", userId)
            put("product_set_id", breakSpotData?.productSetId)
            put("product_set_item_id", breakSpotData?.productSetItemId)
            put("product_set_item_unit_id", breakSpotData?.productSetItemUnitId)
            put("product_set_type", breakSpotData?.surpriseSetDetails?.productSet?.type)
        }

        Log.d(TAG, "EMIT: place_bid_break_spot - $payload")
        socket?.emit("place_bid_break_spot", payload)
    }

    fun getBidTimerUpdateBreakSpot(listener: (json: JSONObject) -> Unit) {
        socket?.on("bid_timer_update_break_spot") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: bid_timer_update_break_spot - $obj")
                listener(obj)
            }
        }
    }

    fun getHighestBidBreakSpot(listener: (json: JSONObject) -> Unit) {
        socket?.on("get_highest_bid_break_spot") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: get_highest_bid_break_spot - $obj")
                listener(obj)
            }
        }
    }

    fun getBidFinalizeBreakSpot(listener: (bidJson: JSONObject) -> Unit) {
        socket?.on("bid_finalized_break_spot") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: bid_finalized_break_spot - $obj")
                listener(obj)
            }
        }
    }

    fun requestCoHostSecondary(roomId: String, userId: String, deviceLabel: String = "Android") {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
            put("device_label", deviceLabel)
        }
        Log.d(TAG, "EMIT: cohost_request_secondary - $payload")
        socket?.emit("cohost_request_secondary", payload)
    }

    fun enterCoHostControlOnly(roomId: String, userId: String, deviceLabel: String = "Android") {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
            put("device_label", deviceLabel)
        }
        Log.d(TAG, "EMIT: cohost_enter_control_only - $payload")
        socket?.emit("cohost_enter_control_only", payload)
    }

    fun takeOverCoHostVideo(roomId: String, userId: String, deviceLabel: String = "Android") {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
            put("device_label", deviceLabel)
        }
        Log.d(TAG, "EMIT: cohost_take_over_video - $payload")
        socket?.emit("cohost_take_over_video", payload)
    }

    fun joinAsInvitedCoHost(roomId: String, userId: String, coHostId: Int?) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
            if (coHostId != null && coHostId > 0) put("co_host_id", coHostId)
        }
        Log.d(TAG, "EMIT: cohost_join - $payload")
        socket?.emit("cohost_join", payload)
    }

    fun leaveInvitedCoHost(roomId: String, userId: String, coHostUserId: String? = null, removedProductIds: List<Int>? = null) {
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
            if (!coHostUserId.isNullOrBlank()) put("co_host_user_id", coHostUserId)
            if (!removedProductIds.isNullOrEmpty()) put("removed_product_ids", JSONArray(removedProductIds))
        }
        Log.d(TAG, "EMIT: cohost_leave - $payload")
        socket?.emit("cohost_leave", payload)
    }

    fun onCoHostVideoHolderChanged(listener: (JSONObject) -> Unit) {
        socket?.off("cohost_video_holder_changed")
        socket?.on("cohost_video_holder_changed") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: cohost_video_holder_changed - $obj")
                listener(obj)
            }
        }
    }

    fun onCoHostControlMode(listener: (JSONObject) -> Unit) {
        socket?.off("cohost_control_mode")
        socket?.on("cohost_control_mode") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: cohost_control_mode - $obj")
                listener(obj)
            }
        }
    }

    fun onCoHostError(listener: (JSONObject) -> Unit) {
        socket?.off("cohost_error")
        socket?.on("cohost_error") { args ->
            val obj = args.firstOrNull()
            if (obj is JSONObject) {
                Log.d(TAG, "RECEIVED: cohost_error - $obj")
                listener(obj)
            }
        }
    }

}
