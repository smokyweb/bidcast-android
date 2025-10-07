package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class VisitorsAnalyticsResponse(
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
        val range: List<Any?>?
    ) {
        @Keep
        data class Chart(
            @SerializedName("label")
            val label: String?,
            @SerializedName("total_visitors")
            val totalVisitors: String?
        )
    }
}