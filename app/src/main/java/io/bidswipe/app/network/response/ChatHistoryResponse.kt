package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName

// Basecamp #9943368953 (2026-05-29): live-show chat history.
// Backed by the EXISTING REST endpoint GET /api/live_chat/{room_id} ->
// ApiController::chatHistory(), which returns { success, room_id, chats: [...] }
// from the live_chats table (LiveChat model). Verified against live backend
// 2026-05-29 by Robin — there is NO get_chat_history socket event; this REST
// route is the supported path.
data class ChatHistoryResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("room_id") val roomId: String? = null,
    @SerializedName("chats") val chats: List<ChatHistoryItem>? = null,
)

data class ChatHistoryItem(
    @SerializedName("room_id") val roomId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("user_image") val userImage: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)
