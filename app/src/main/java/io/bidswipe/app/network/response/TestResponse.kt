package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class TestResponse(
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
        val stats: Stats?,
        @SerializedName("top_buyers_by_orders")
        val topBuyersByOrders: List<TopBuyersByOrder?>?,
        @SerializedName("top_buyers_by_sales")
        val topBuyersBySales: List<TopBuyersBySale?>?,
        @SerializedName("total_all_orders")
        val totalAllOrders: Int?,
        @SerializedName("total_all_sales")
        val totalAllSales: String?
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

        @Keep
        data class TopBuyersByOrder(
            @SerializedName("total_orders")
            val totalOrders: Int?,
            @SerializedName("user")
            val user: User?,
            @SerializedName("user_id")
            val userId: Int?
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
        }

        @Keep
        data class TopBuyersBySale(
            @SerializedName("total")
            val total: String?,
            @SerializedName("user")
            val user: User?,
            @SerializedName("user_id")
            val userId: Int?
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
        }
    }
}