package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetTipAmountResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("summary")
        val summary: Summary?,
        @SerializedName("tips")
        val tips: List<Tip?>?
    ) {
        @Keep
        data class Summary(
            @SerializedName("today_tips")
            val todayTips: Int?,
            @SerializedName("total_tips")
            val totalTips: String?
        )

        @Keep
        data class Tip(
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("total")
            val total: String?,
            @SerializedName("user")
            val user: User?,
            @SerializedName("user_id")
            val userId: Int?,
            @SerializedName("show")
            val show: Show?,
            @SerializedName("show_id")
            val showId: Int?,
        ) {
            @Keep
            data class User(
                @SerializedName("email")
                val email: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("name")
                val name: String?,
                @SerializedName("profile_image")
                val profileImage: String?
            )
            @Keep
            data class Show(
                @SerializedName("auction_type_id")
                val auctionTypeId: Int?,
                @SerializedName("category_id")
                val categoryId: Int?,
                @SerializedName("date")
                val date: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("img_thumbnail")
                val imgThumbnail: List<String?>?,
                @SerializedName("is_explicit")
                val isExplicit: Boolean?,
                @SerializedName("is_live")
                val isLive: Boolean?,
                @SerializedName("is_repeat")
                val isRepeat: Boolean?,
                @SerializedName("language")
                val language: String?,
                @SerializedName("latest_viewer_count")
                val latestViewerCount: Int?,
                @SerializedName("product_ids")
                val productIds: List<String?>?,
                @SerializedName("promote_show_id")
                val promoteShowId: Any?,
                @SerializedName("promoted_at")
                val promotedAt: Any?,
                @SerializedName("recording_resource_id")
                val recordingResourceId: String?,
                @SerializedName("recording_sid")
                val recordingSid: String?,
                @SerializedName("repeat_value")
                val repeatValue: String?,
                @SerializedName("rtc_token")
                val rtcToken: String?,
                @SerializedName("share_count")
                val shareCount: Int?,
                @SerializedName("show_discoverability")
                val showDiscoverability: String?,
                @SerializedName("started_at")
                val startedAt: String?,
                @SerializedName("thumbnail")
                val thumbnail: List<String?>?,
                @SerializedName("time")
                val time: String?,
                @SerializedName("title")
                val title: String?,
                @SerializedName("user_id")
                val userId: Int?,
                @SerializedName("viewer_count")
                val viewerCount: Int?
            )
        }
    }
}