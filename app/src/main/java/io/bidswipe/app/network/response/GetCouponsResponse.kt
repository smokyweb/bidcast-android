package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetCouponsResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("assigned_count")
        val assignedCount: Int?,
        @SerializedName("coupon")
        val coupon: Coupon?,
        @SerializedName("coupon_id")
        val couponId: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("status")
        val status: Int?,
        @SerializedName("user_id")
        val userId: Int?
    ) {
        data class Coupon(
            @SerializedName("description")
            val description: String?,
            @SerializedName("exp_date")
            val expDate: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("max_users")
            val maxUsers: Int?,
            @SerializedName("min_amount")
            val minAmount: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("per_user_limit")
            val perUserLimit: Int?,
            @SerializedName("start_date")
            val startDate: String?,
            @SerializedName("status")
            val status: Boolean?,
            @SerializedName("type")
            val type: String?,
            @SerializedName("used_count")
            val usedCount: Int?,
            @SerializedName("value")
            val value: Int?
        )
    }
}