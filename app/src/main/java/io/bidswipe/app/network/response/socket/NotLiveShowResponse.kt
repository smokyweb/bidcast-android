package io.bidswipe.app.network.response.socket


import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class NotLiveShowResponse(
    @SerializedName("auction_type_id")
    val auctionTypeId: Int?=0,
    @SerializedName("category_id")
    val categoryId: Int?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("img_thumbnail")
    val imgThumbnail: String?,
    @SerializedName("is_explicit")
    val isExplicit: Int?,
    @SerializedName("is_live")
    val isLive: Int?,
    @SerializedName("is_promote")
    val isPromote: String?,
    @SerializedName("is_promoted")
    val isPromoted: Int?,
    @SerializedName("is_repeat")
    val isRepeat: Int?,
    @SerializedName("is_room_created")
    val isRoomCreated: Boolean?,
    @SerializedName("language")
    val language: String?,
    @SerializedName("latest_viewer_count")
    val latestViewerCount: Int?,
    @SerializedName("product_ids")
    @JsonAdapter(ProductIdsDeserializer::class)
    val productIds: List<String?>?,
    @SerializedName("promote_show_id")
    val promoteShowId: Any?,
    @SerializedName("promoted_at")
    val promotedAt: Any?,
    @SerializedName("promotion_end_at")
    val promotionEndAt: Any?,
    @SerializedName("promotion_start_at")
    val promotionStartAt: Any?,
    @SerializedName("recording_resource_id")
    val recordingResourceId: Any?,
    @SerializedName("recording_sid")
    val recordingSid: Any?,
    @SerializedName("repeat_value")
    val repeatValue: String?,
    @SerializedName("room_id")
    val roomId: String?,
    @SerializedName("rtc_token")
    val rtcToken: Any?,
    @SerializedName("seller")
    val seller: Seller?,
    @SerializedName("share_count")
    val shareCount: Int?,
    @SerializedName("show_discoverability")
    val showDiscoverability: String?,
    @SerializedName("show_id")
    val showId: String?,
    @SerializedName("show_notes")
    val showNotes: Any?,
    @SerializedName("started_at")
    val startedAt: Any?,
    @SerializedName("thumbnail")
    val thumbnail: String?,
    @SerializedName("time")
    val time: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("viewer_count")
    val viewerCount: Int?
) {
    data class Seller(
        @SerializedName("id")
        val id: Int?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("username")
        val username: String?,
        @SerializedName("rating")
        val rating: String?
    )
}

class ProductIdsDeserializer : JsonDeserializer<List<String>> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String> {

        if (json == null || json.isJsonNull) return emptyList()

        return when {
            json.isJsonArray -> {
                json.asJsonArray.map { it.asString }
            }

            json.isJsonObject -> {
                json.asJsonObject.entrySet().map { it.value.asString }
            }

            else -> emptyList()
        }
    }
}
