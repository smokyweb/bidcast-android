package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetOrdersResponse(
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("perPage")
    val perPage: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?,
    @SerializedName("new_order_count")
    val newOrderCount: Int?,
    @SerializedName("processing_order_count")
    val processingOrderCount: Int?,
    @SerializedName("completed_order_count")
    val completeOrderCount: Int?,
) {
    @Keep
    data class Data(
        @SerializedName("card_id")
        val cardId: String?,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("gift_msg")
        val giftMsg: String?,
        @SerializedName("gift_user_id")
        val giftUserId: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("order_id")
        val orderId: String?,
        @SerializedName("product")
        val product: Product?,
        @SerializedName("product_id")
        val productId: Int?,
        @SerializedName("promo_code")
        val promoCode: String?,
        @SerializedName("send_as_gift")
        val sendAsGift: Boolean?,
        @SerializedName("shipping_address")
        val shippingAddress: String?,
        @SerializedName("shipping_tracking")
        val shippingTracking: List<ShippingTracking?>?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("user_id")
        val userId: Int?,
    ) {
        @Keep
        data class Product(
            @SerializedName("accept_offers")
            val acceptOffers: Boolean?,
            @SerializedName("category_id")
            val categoryId: Int?,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("flash_sale")
            val flashSale: Boolean?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("images")
            val images: List<String?>?,
            @SerializedName("pricing")
            val pricing: Double?,
            @SerializedName("product_show")
            val productShow: String?,
            @SerializedName("purchased_quantity")
            val purchasedQuantity: Int?,
            @SerializedName("quantity")
            val quantity: Int?,
            @SerializedName("reserve_for_live")
            val reserveForLive: Boolean?,
            @SerializedName("shipping_profile_id")
            val shippingProfileId: Int?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("thumbnail")
            val thumbnail: List<String?>?,
            @SerializedName("title")
            val title: String?,
            @SerializedName("user_id")
            val userId: Int?,
        )

        @Keep
        data class ShippingTracking(
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("order_id")
            val orderId: Int?,
            @SerializedName("title")
            val title: String?,
        )

        @Keep
        data class User(
            @SerializedName("bio")
            val bio: String?,
            @SerializedName("email")
            val email: String?,
            @SerializedName("first_name")
            val firstName: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("is_active")
            val isActive: Boolean?,
            @SerializedName("last_name")
            val lastName: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?,
            @SerializedName("referral_code")
            val referralCode: String?,
            @SerializedName("role_id")
            val roleId: Int?,
            @SerializedName("thumbnail")
            val thumbnail: String?,
            @SerializedName("username")
            val username: String?,
        )
    }
}