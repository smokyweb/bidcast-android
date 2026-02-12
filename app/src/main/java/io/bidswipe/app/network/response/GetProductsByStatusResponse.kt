package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetProductsByStatusResponse(
	@SerializedName("currentPage")
	val currentPage : Int? ,
	@SerializedName("data")
	val `data` : List<Data?>? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("perPage")
	val perPage : Int? ,
	@SerializedName("status")
	val status : String? ,
	@SerializedName("total")
	val total : Int? ,
	@SerializedName("totalPage")
	val totalPage : Int? ,
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
		@SerializedName("product_set_item")
		val productSetItem: ProductSetItem?,
		@SerializedName("product_set_item_unit")
		val productSetItemUnit: ProductSetItemUnit?,
		@SerializedName("promo_code")
		val promoCode: Any?,
		@SerializedName("send_as_gift")
		val sendAsGift: Boolean?,
		@SerializedName("shipping_address")
		val shippingAddress: String?,
		@SerializedName("status")
		val status: String?,
		@SerializedName("user")
		val user: User?,
		@SerializedName("user_id")
		val userId: Int?
	) {
		data class Product(
			@SerializedName("acceptOffers")
			val acceptOffers: Boolean?,
			@SerializedName("auction")
			val auction: Boolean?,
			@SerializedName("bid_count")
			val bidCount: Int?,
			@SerializedName("category")
			val category: Category?,
			@SerializedName("createdAt")
			val createdAt: String?,
			@SerializedName("description")
			val description: String?,
			@SerializedName("flashSale")
			val flashSale: Boolean?,
			@SerializedName("hazardousMaterial")
			val hazardousMaterial: Boolean?,
			@SerializedName("height")
			val height: Double?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("images")
			val images: List<String?>?,
			@SerializedName("length")
			val length: Double?,
			@SerializedName("mailClass")
			val mailClass: String?,
			@SerializedName("pricing")
			val pricing: String?,
			@SerializedName("processingCategory")
			val processingCategory: String?,
			@SerializedName("productCondition")
			val productCondition: String?,
			@SerializedName("productShow")
			val productShow: String?,
			@SerializedName("purchasedQuantity")
			val purchasedQuantity: String?,
			@SerializedName("quantity")
			val quantity: String?,
			@SerializedName("reserveForLive")
			val reserveForLive: Boolean?,
			@SerializedName("shippingProfileId")
			val shippingProfileId: Int?,
			@SerializedName("sku")
			val sku: String?,
			@SerializedName("status")
			val status: String?,
			@SerializedName("subCategoryId")
			val subCategoryId: Any?,
			@SerializedName("thumbnail")
			val thumbnail: List<String?>?,
			@SerializedName("title")
			val title: String?,
			@SerializedName("type")
			val type: Any?,
			@SerializedName("user")
			val user: User?,
			@SerializedName("userId")
			val userId: Int?,
			@SerializedName("variant")
			val variant: List<Any?>?,
			@SerializedName("videos")
			val videos: List<String?>?,
			@SerializedName("weight")
			val weight: Double?,
			@SerializedName("width")
			val width: Double?
		) {
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
				val thumbnail: String?
			)

			data class User(
				@SerializedName("email")
				val email: String?,
				@SerializedName("id")
				val id: Int?,
				@SerializedName("name")
				val name: String?,
				@SerializedName("profileImage")
				val profileImage: String?,
				@SerializedName("username")
				val username: String?
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
			val userId: Int?,
			@SerializedName("seller")
			val seller: Seller?,
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

		data class Seller(
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