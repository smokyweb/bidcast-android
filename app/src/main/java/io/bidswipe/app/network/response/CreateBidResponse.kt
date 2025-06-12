package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CreateBidResponse(
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
        @SerializedName("bid_price")
        val bidPrice: Int?,
        @SerializedName("created_by")
        val createdBy: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("product_id")
        val productId: Int?,
        @SerializedName("schedule_show_id")
        val scheduleShowId: Int?,
        @SerializedName("user_id")
        val userId: Int?
    )
}