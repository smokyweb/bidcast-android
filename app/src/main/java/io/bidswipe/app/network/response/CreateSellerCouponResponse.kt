package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName

/**
 * MC cmph7xsgw00g2ms8pmtc6xgz5 (Trey 2026-05-22): response for the
 * seller-coupon create/update endpoint. The server returns the coupon
 * row with attached products (id/title/images).
 */
data class CreateSellerCouponResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error_type") val errorType: String? = null,
    @SerializedName("data") val data: Coupon? = null
) {
    data class Coupon(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("seller_id") val sellerId: Int? = null,
        @SerializedName("name") val name: String? = null,
        @SerializedName("type") val type: String? = null,
        @SerializedName("value") val value: Double? = null,
        @SerializedName("min_amount") val minAmount: Double? = null,
        @SerializedName("max_users") val maxUsers: Int? = null,
        @SerializedName("used_count") val usedCount: Int? = null,
        @SerializedName("per_user_limit") val perUserLimit: Int? = null,
        @SerializedName("start_date") val startDate: String? = null,
        @SerializedName("exp_date") val expDate: String? = null,
        @SerializedName("status") val active: Boolean? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("products") val products: List<Product>? = null
    )

    data class Product(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("images") val images: List<String>? = null
    )
}
