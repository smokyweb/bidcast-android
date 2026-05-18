package io.bidswipe.app.network.response


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class SellerHubResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("account_health")
        val accountHealth: AccountHealth?,
        @SerializedName("items")
        val items: Int?,
        @SerializedName("payouts")
        val payouts: Double?,
        @SerializedName("rating")
        val rating: Double?,
        @SerializedName("revenue")
        val revenue: Double?,
        @SerializedName("new_orders")
        val newOrders: Int?,
        @SerializedName("total_orders")
        val totalOrders: Int?,
        @SerializedName("upcoming_show")
        val upcomingShow: UpcomingShow?,
        @SerializedName("vacation_mode")
        val vacationMode: Boolean?
    ) {
        data class AccountHealth(
            @SerializedName("defect_free_order_rate")
            val defectFreeOrderRate: String?,
            @SerializedName("on_time_scan_rate")
            val onTimeScanRate: String?,
            @SerializedName("policy_standing")
            val policyStanding: String?
        )

        data class UpcomingShow(
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
            val recordingResourceId: Any?,
            @SerializedName("recording_sid")
            val recordingSid: Any?,
            @SerializedName("repeat_value")
            val repeatValue: String?,
            @SerializedName("rtc_token")
            val rtcToken: String?,
            @SerializedName("share_count")
            val shareCount: Int?,
            @SerializedName("show_discoverability")
            val showDiscoverability: String?,
            @SerializedName("started_at")
            val startedAt: Any?,
            @SerializedName("thumbnail")
            val thumbnail: List<String?>?,
            @SerializedName("time")
            val time: String?,
            @SerializedName("title")
            val title: String?,
            @SerializedName("user_id")
            val userId: Int?,
            @SerializedName("viewer_count")
            val viewerCount: Int?,
            @SerializedName("category")
            val category: Category?,
        ) {
            @Keep
            data class Category(
                @SerializedName("color")
                val color: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("image")
                val image: String?,
                @SerializedName("name")
                val name: String?,
                @SerializedName("thumbnail")
                val thumbnail: String?,
            )
        }
    }
}