package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SalesAnalyticsResponse(
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
        @SerializedName("chart")
        val chart: List<Chart?>?,
        @SerializedName("filter")
        val filter: String?,
        @SerializedName("range")
        val range: List<String?>?
    ) {
        @Keep
        data class Chart(
            @SerializedName("label")
            val label: String?,
            @SerializedName("total_revenue")
            val totalRevenue: String?,
            @SerializedName("total_sales")
            val totalSales: Int?
        )
    }
}