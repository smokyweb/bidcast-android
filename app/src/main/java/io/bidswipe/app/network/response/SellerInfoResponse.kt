package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SellerInfoResponse(
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
        @SerializedName("avg_ship")
        val avgShip: Any?,
        @SerializedName("is_following")
        val isFollowing: Boolean?,
        @SerializedName("rating_avg")
        val ratingAvg: Int?,
        @SerializedName("review")
        val review: Any?,
        @SerializedName("seller_details")
        val sellerDetails: SellerDetails?,
        @SerializedName("sold_avg")
        val soldAvg: Int?
    ) {
        @Keep
        data class SellerDetails(
            @SerializedName("authorize_net_cid")
            val authorizeNetCid: Any?,
            @SerializedName("bio")
            val bio: Any?,
            @SerializedName("default_card_id")
            val defaultCardId: Any?,
            @SerializedName("email")
            val email: String?,
            @SerializedName("first_name")
            val firstName: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("is_active")
            val isActive: Boolean?,
            @SerializedName("is_FirsttimeLogin")
            val isFirsttimeLogin: Boolean?,
            @SerializedName("jwt_token")
            val jwtToken: Any?,
            @SerializedName("last_name")
            val lastName: String?,
            @SerializedName("live_sell_vendor_status")
            val liveSellVendorStatus: String?,
            @SerializedName("marketplace_vendor_status")
            val marketplaceVendorStatus: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?,
            @SerializedName("profile_visits")
            val profileVisits: Int?,
            @SerializedName("referral_code")
            val referralCode: String?,
            @SerializedName("role_id")
            val roleId: Int?,
            @SerializedName("thumbnail")
            val thumbnail: Any?,
            @SerializedName("username")
            val username: Any?
        )
    }
}