package io.bidswipe.app.network.request

import com.google.gson.annotations.SerializedName

/**
 * MC cmph7xsgw00g2ms8pmtc6xgz5 (Trey 2026-05-22): request body for
 * POST /api/seller-coupons. Matches the validation rules in
 * Api/SellerCouponController::store on the backend.
 */
data class CreateSellerCouponRequest(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String, // "percentage" or "flat"
    @SerializedName("value") val value: Double,
    @SerializedName("start_date") val startDate: String, // YYYY-MM-DD
    @SerializedName("exp_date") val expDate: String,     // YYYY-MM-DD
    @SerializedName("product_ids") val productIds: List<Int>,
    @SerializedName("min_amount") val minAmount: Double? = null,
    @SerializedName("max_users") val maxUsers: Int? = null,
    @SerializedName("per_user_limit") val perUserLimit: Int? = null,
    @SerializedName("status") val status: Boolean? = true,
    @SerializedName("description") val description: String? = null
)
