package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SellerAnalyticsResponse(
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
        @SerializedName("seller")
        val seller: Seller?,
        @SerializedName("stats")
        val stats: Stats?
    ) {
        @Keep
        data class Seller(
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile")
            val profile: String?,
            @SerializedName("since")
            val since: String?
        )

        @Keep
        data class Stats(
            @SerializedName("followers")
            val followers: Int?,
            @SerializedName("live_sessions")
            val liveSessions: Int?,
            @SerializedName("rating")
            val rating: Int?,
            @SerializedName("revenue")
            val revenue: String?,
            @SerializedName("total_items")
            val totalItems: Int?,
            @SerializedName("total_sales")
            val totalSales: Int?
        )
    }
}