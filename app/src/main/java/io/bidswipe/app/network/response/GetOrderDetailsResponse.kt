package io.bidswipe.app.network.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class GetOrderDetailsResponse(
	@SerializedName("data")
	val `data`: Data?,
	@SerializedName("error_type")
	val errorType: String?,
	@SerializedName("message")
	val message: String?,
	@SerializedName("status")
	val status: String?,
) {
	@Keep
	data class Data(
		@SerializedName("card_id")
		val cardId: Any?,  // Any? handles Int (old orders) or String (new orders) without Gson crash
		@SerializedName("created_at")
		val createdAt: String?,
		@SerializedName("gift_msg")
		val giftMsg: String?,
		@SerializedName("gift_user")
		val giftUser: GiftUser?,
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
		@SerializedName("product_set")
		val productSet: ProductSet?,
		@SerializedName("product_set_id")
		val productSetId: Int?,
		@SerializedName("product_set_item_id")
		val productSetItemId: Int?,
		@SerializedName("product_set_item_unit_id")
		val productSetItemUnitId: Int?,
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
		// MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): keep both sides.
		// GitLab (Wave 4 #32/#33): USPS tracking + label fields.
		@SerializedName("tracking_number")
		val trackingNumber: String?,
		@SerializedName("shipping_status")
		val shippingStatus: String?,
		@SerializedName("label_url")
		val labelUrl: String?,
		// GitHub (1552832d): receipt price/shipping/tax breakdown summary.
		@SerializedName("summary")
		val summary: Summary?,
		@SerializedName("user")
		val user: User?,
		@SerializedName("user_id")
		val userId: Int?,
	) {
		@Keep
		data class Summary(
			@SerializedName("product_price")
			val productPrice: Double?,
			@SerializedName("shipping_charge")
			val shippingCharge: Double?,
			@SerializedName("tax_percent")
			val taxPercent: Double?,
			@SerializedName("tax_amount")
			val taxAmount: Double?,
			@SerializedName("sub_total")
			val subTotal: Double?,
			@SerializedName("total")
			val total: Double?,
		)

		@Keep
		data class GiftUser(
			@SerializedName("bio")
			val bio: Any?,
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
			val thumbnail: Any?,
			@SerializedName("username")
			val username: Any?,
		)

		@Keep
		data class Product(
			@SerializedName("accept_offers")
			val acceptOffers: Boolean?,
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
			val thumbnail: List<Any?>?,
			@SerializedName("title")
			val title: String?,
			@SerializedName("user_id")
			val userId: Int?,
		) {
			@Keep
			data class Category(
				@SerializedName("color")
				val color: String?,
				@SerializedName("id")
				val id: Int?,
				@SerializedName("image")
				val image: String?,
				@SerializedName("name")
				val name: String?,
				@SerializedName("thumbnail")
				val thumbnail: String?,
			) : Serializable

		}

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
			val thumbnail: Any?,
			@SerializedName("username")
			val username: String?,
		)

		data class ProductSet(
			@SerializedName("auto_randomizer")
			val autoRandomizer: Int?,
			@SerializedName("created_at")
			val createdAt: String?,
			@SerializedName("description")
			val description: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("is_live_bid")
			val isLiveBid: Int?,
			@SerializedName("items")
			val items: List<Item?>?,
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
			@SerializedName("updated_at")
			val updatedAt: String?,
			@SerializedName("user_id")
			val userId: Int?
		) {
			data class Item(
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
		}

	}
}