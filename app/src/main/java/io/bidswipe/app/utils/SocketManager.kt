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
        val payload = JSONObject().apply {
            put("room_id", roomId)
            put("user_id", userId)
        }
        Log.d(TAG, "EMIT:join_show  - userId: $userId, showId: $roomId ")
        socket?.emit("join_show", payload)
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

}


