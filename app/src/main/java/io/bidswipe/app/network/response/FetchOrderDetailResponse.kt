package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class FetchOrderDetailResponse(
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
        val avgShip: String?,
        @SerializedName("is_following")
        val isFollowing: Boolean?,
        @SerializedName("order")
        val order: Order?,
        @SerializedName("rating_avg")
        val ratingAvg: String?,
        @SerializedName("review")
        val review: String?,
        @SerializedName("seller_details")
        val sellerDetails: SellerDetails?,
        @SerializedName("shipping_address")
        val shippingAddress: ShippingAddress?,
        @SerializedName("sold_count")
        val soldCount: Int?,
        @SerializedName("bid_video_url")
        val bidVideoUrl: String?
    ) {
        @Keep
        data class Order(
            @SerializedName("card_id")
            val cardId: Any?,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("customer_payment_profile_id")
            val customerPaymentProfileId: Any?,
            @SerializedName("gift_msg")
            val giftMsg: Any?,
            @SerializedName("gift_user")
            val giftUser: Any?,
            @SerializedName("gift_user_id")
            val giftUserId: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("order_id")
            val orderId: String?,
            @SerializedName("product")
            val product: Product?,
            @SerializedName("product_id")
            val productId: Int?,
            @SerializedName("promo_code")
            val promoCode: Any?,
            @SerializedName("send_as_gift")
            val sendAsGift: Boolean?,
            @SerializedName("shipping_address")
            val shippingAddress: String?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("user_id")
            val userId: Int?,
            @SerializedName("order_status_percentage")
            val orderStatusPercentage: Any?,
            @SerializedName("sub_total")
            val subTotal: Any?,
            @SerializedName("tax_amount")
            val taxAmount: Any?,
            @SerializedName("shipping_charges")
            val shippingCharges: Any?,
            @SerializedName("total")
            val total: Any?,
            @SerializedName("discount")
            val discount: Any?,
            // Cancel-request flow (2026-05-29): new fields from backend.
            @SerializedName("cancellation_status")
            val cancellationStatus: String?,
            @SerializedName("cancellation_reason")
            val cancellationReason: String?,
            @SerializedName("cancellation_reject_reason")
            val cancellationRejectReason: String?
        ) {
            @Keep
            data class Product(
                @SerializedName("accept_offers")
                val acceptOffers: Boolean?,
                @SerializedName("auction")
                val auction: Boolean?,
                @SerializedName("category")
                val category: Category?,
                @SerializedName("category_id")
                val categoryId: Int?,
                @SerializedName("created_at")
                val createdAt: String?,
                @SerializedName("description")
                val description: String?,
                @SerializedName("flash_sale")
                val flashSale: Boolean?,
                @SerializedName("height")
                val height: Any?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("images")
                val images: List<String?>?,
                @SerializedName("length")
                val length: Any?,
                @SerializedName("mail_class")
                val mailClass: Any?,
                @SerializedName("pricing")
                val pricing: String?,
                @SerializedName("processing_category")
                val processingCategory: Any?,
                @SerializedName("product_show")
                val productShow: String?,
                @SerializedName("purchased_quantity")
                val purchasedQuantity: String?,
                @SerializedName("quantity")
                val quantity: String?,
                @SerializedName("reserve_for_live")
                val reserveForLive: Boolean?,
                @SerializedName("shipping_profile_id")
                val shippingProfileId: Int?,
                @SerializedName("status")
                val status: String?,
                @SerializedName("sub_category_id")
                val subCategoryId: Any?,
                @SerializedName("thumbnail")
                val thumbnail: List<Any?>?,
                @SerializedName("title")
                val title: String?,
                @SerializedName("type")
                val type: Any?,
                @SerializedName("user_id")
                val userId: Int?,
                @SerializedName("variant")
                val variant: Any?,
                @SerializedName("weight")
                val weight: Any?,
                @SerializedName("width")
                val width: Any?
            ) {
                @Keep
                data class Category(
                    @SerializedName("color")
                    val color: String?,
                    @SerializedName("deleted_at")
                    val deletedAt: Any?,
                    @SerializedName("extra_fields")
                    val extraFields: List<Any?>?,
                    @SerializedName("id")
                    val id: Int?,
                    @SerializedName("image")
                    val image: String?,
                    @SerializedName("name")
                    val name: String?,
                    @SerializedName("thumbnail")
                    val thumbnail: String?
                )
            }
        }

        @Keep
        data class SellerDetails(
            @SerializedName("authorize_net_cid")
            val authorizeNetCid: String?,
            @SerializedName("bio")
            val bio: String?,
            @SerializedName("default_card_id")
            val defaultCardId: String?,
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
            val jwtToken: String?,
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
            val username: String?
        )

        @Keep
        data class ShippingAddress(
            @SerializedName("address_line_2")
            val addressLine2: String?,
            @SerializedName("city")
            val city: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("is_default")
            val isDefault: Boolean?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("phone_number")
            val phoneNumber: String?,
            @SerializedName("pincode")
            val pincode: String?,
            @SerializedName("state")
            val state: String?,
            @SerializedName("street_address")
            val streetAddress: String?,
            @SerializedName("type")
            val type: String?,
            @SerializedName("user_id")
            val userId: Int?
        )
    }
}