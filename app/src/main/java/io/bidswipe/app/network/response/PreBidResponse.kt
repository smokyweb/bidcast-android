package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName

// Basecamp #9933847997 (2026-05-29): pre-bid response models.
// Endpoint: POST /api/pre-bid
data class PreBidResponse(
    @SerializedName("status")  val status: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data")    val data: PreBidData? = null
) {
    data class PreBidData(
        @SerializedName("id")               val id: Int? = null,
        @SerializedName("product_id")       val productId: Int? = null,
        @SerializedName("schedule_show_id") val scheduleShowId: Int? = null,
        @SerializedName("amount")           val amount: Double? = null,
        @SerializedName("user_id")          val userId: Int? = null,
        @SerializedName("created_at")       val createdAt: String? = null,
    )
}

// Endpoint: GET /api/pre-bid/highest/{productId}
data class PreBidHighestResponse(
    @SerializedName("status")  val status: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data")    val data: PreBidHighestData? = null
) {
    data class PreBidHighestData(
        @SerializedName("amount")  val amount: Double? = null,
        @SerializedName("user_id") val userId: Int? = null,
    )
}

// Endpoint: GET /api/pre-bid
data class PreBidListResponse(
    @SerializedName("status")  val status: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data")    val data: List<PreBidResponse.PreBidData?>? = null
)
