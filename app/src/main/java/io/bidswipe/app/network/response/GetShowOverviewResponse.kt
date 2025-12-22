package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetShowOverviewResponse(
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
        @SerializedName("contributions_count")
        val contributionsCount: Int?,
        @SerializedName("new_followers")
        val newFollowers: Int?,
        @SerializedName("file_url")
        val fileUrl: String?,
        @SerializedName("order_count")
        val orderCount: Int?,
        @SerializedName("share_count")
        val shareCount: Int?,
        @SerializedName("total_bids")
        val totalBids: Int?,
        @SerializedName("total_sales")
        val totalSales: String?,
        @SerializedName("video_duration")
        val videoDuration: String?,
        @SerializedName("viewer_count")
        val viewerCount: Int?
    )
}