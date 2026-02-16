package io.bidswipe.app.network.response


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GetOrdersResponse(
    @SerializedName("status")
    val status: String?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?,
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
    @SerializedName("completed_order_count")
    val completedOrderCount: Int?,
    @SerializedName("new_order_count")
    val newOrderCount: Int?,
    @SerializedName("processing_order_count")
    val processingOrderCount: Int?,
) {
    data class Data(
        @SerializedName("card_id")
        val cardId: Any?,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("customer_payment_profile_id")
        val customerPaymentProfileId: Any?,
        @SerializedName("gift_msg")
        val giftMsg: Any?,
        @SerializedName("gift_user_id")
        val giftUserId: Any?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("order_id")
        val orderId: String?,
        @SerializedName("order_source")
        val orderSource: String?,
        @SerializedName("payment_status")
        val paymentStatus: String?,
        @SerializedName("product")
        val product: Product?,
        @SerializedName("product_id")
        val productId: Int?,
        @SerializedName("product_set")
        val productSet: ProductSet?,
        @SerializedName("product_set_id")
        val productSetId: Int?,
        @SerializedName("product_set_item")
        val productSetItem: ProductSetItem?,
        @SerializedName("product_set_item_id")
        val productSetItemId: Int?,
        @SerializedName("product_set_item_unit")
        val productSetItemUnit: ProductSetItemUnit?,
        @SerializedName("product_set_item_unit_id")
        val productSetItemUnitId: Int?,
        @SerializedName("promo_code")
        val promoCode: Any?,
        @SerializedName("send_as_gift")
        val sendAsGift: Boolean?,
        @SerializedName("shipping_address")
        val shippingAddress: String?,
        @SerializedName("shipping_tracking")
        val shippingTracking: List<ShippingTracking?>?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("transaction")
        val transaction: List<Transaction?>?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("user_id")
        val userId: Int?
    ) {
        data class Product(
            @SerializedName("accept_offers")
            val acceptOffers: Boolean?,
            @SerializedName("auction")
            val auction: Boolean?,
            @SerializedName("category")
            val category: Category?,
            @SerializedName("category_id")
            val categoryId: Int?,
            @SerializedName("cost_per_item")
            val costPerItem: Any?,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("flash_sale")
            val flashSale: Boolean?,
            @SerializedName("hazardous_material")
            val hazardousMaterial: Boolean?,
            @SerializedName("height")
            val height: Double?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("images")
            val images: List<String?>?,
            @SerializedName("is_live_bid")
            val isLiveBid: Boolean?,
            @SerializedName("length")
            val length: Double?,
            @SerializedName("mail_class")
            val mailClass: String?,
            @SerializedName("pricing")
            val pricing: String?,
            @SerializedName("processing_category")
            val processingCategory: String?,
            @SerializedName("product_condition")
            val productCondition: String?,
            @SerializedName("product_show")
            val productShow: String?,
            @SerializedName("purchased_quantity")
            val purchasedQuantity: String?,
            @SerializedName("quantity")
            val quantity: String?,
            @SerializedName("reserve_for_live")
            val reserveForLive: Boolean?,
            @SerializedName("shipping_profile_id")
            val shippingProfileId: Any?,
            @SerializedName("sku")
            val sku: String?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("sub_category_id")
            val subCategoryId: Any?,
            @SerializedName("thumbnail")
            val thumbnail: List<String?>?,
            @SerializedName("title")
            val title: String?,
            @SerializedName("type")
            val type: Any?,
            @SerializedName("user_id")
            val userId: Int?,
            @SerializedName("variant")
            val variant: List<Any?>?,
            @SerializedName("videos")
            val videos: List<Any?>?,
            @SerializedName("weight")
            val weight: Double?,
            @SerializedName("width")
            val width: Double?
        ) {
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

        data class ProductSet(
            @SerializedName("auto_randomizer")
            val autoRandomizer: Int?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("is_live_bid")
            val isLiveBid: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("price")
            val price: Int?,
            @SerializedName("quick_spin")
            val quickSpin: Int?,
            @SerializedName("shipping_profile_id")
            val shippingProfileId: Int?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("type")
            val type: String?,
            @SerializedName("user_id")
            val userId: Int?
        )

        data class ProductSetItem(
            @SerializedName("description")
            val description: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("price")
            val price: Int?,
            @SerializedName("product_set_id")
            val productSetId: Int?,
            @SerializedName("quantity")
            val quantity: Int?,
            @SerializedName("sold_quantity")
            val soldQuantity: Int?,
            @SerializedName("status")
            val status: String?
        )

        data class ProductSetItemUnit(
            @SerializedName("description")
            val description: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("price")
            val price: Int?,
            @SerializedName("product_set_item_id")
            val productSetItemId: Int?,
            @SerializedName("status")
            val status: String?
        )

        data class ShippingTracking(
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("order_id")
            val orderId: Int?,
            @SerializedName("title")
            val title: String?
        )

        data class Transaction(
            @SerializedName("account_number")
            val accountNumber: Any?,
            @SerializedName("card_number")
            val cardNumber: String?,
            @SerializedName("charge_id")
            val chargeId: String?,
            @SerializedName("coupon_id")
            val couponId: Any?,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("date")
            val date: String?,
            @SerializedName("discount")
            val discount: Int?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("order_id")
            val orderId: Int?,
            @SerializedName("payment_intent_id")
            val paymentIntentId: String?,
            @SerializedName("payout_id")
            val payoutId: Any?,
            @SerializedName("product_price")
            val productPrice: Double?,
            @SerializedName("promote_show_id")
            val promoteShowId: Any?,
            @SerializedName("seller_id")
            val sellerId: Int?,
            @SerializedName("shipping_charges")
            val shippingCharges: Int?,
            @SerializedName("show_id")
            val showId: Any?,
            @SerializedName("source_type")
            val sourceType: String?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("sub_total")
            val subTotal: Double?,
            @SerializedName("tax_amount")
            val taxAmount: Double?,
            @SerializedName("total")
            val total: String?,
            @SerializedName("type")
            val type: String?,
            @SerializedName("user_id")
            val userId: Int?
        )

        data class User(
            @SerializedName("email")
            val email: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?,
            @SerializedName("username")
            val username: String?
        )
    }
}