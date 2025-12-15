package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetProductDetailsResponse(
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
		@SerializedName("accept_offers")
		val acceptOffers: Boolean?,
		@SerializedName("auction")
		val auction: Boolean?,
		@SerializedName("category_id")
		val categoryId: Int?,
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
		@SerializedName("length")
		val length: Double?,
		@SerializedName("mail_class")
		val mailClass: String?,
		@SerializedName("offer")
		val offer: Offer?,
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
		@SerializedName("shipping_adress")
		val shippingAdress: ShippingAdress?,
		@SerializedName("shipping_profile_id")
		val shippingProfileId: Int?,
		@SerializedName("sku")
		val sku: String?,
		@SerializedName("status")
		val status: String?,
		@SerializedName("sub_category_id")
		val subCategoryId: Int?,
		@SerializedName("thumbnail")
		val thumbnail: List<String?>?,
		@SerializedName("title")
		val title: String?,
		@SerializedName("type")
		val type: String?,
		@SerializedName("user")
		val user: User?,
		@SerializedName("user_id")
		val userId: Int?,
		@SerializedName("variant")
		val variant: String?,
		@SerializedName("videos")
		val videos: List<String?>?,
		@SerializedName("weight")
		val weight: Double?,
		@SerializedName("width")
		val width: Double?
	) {
		@Keep
		data class Offer(
			@SerializedName("amount")
			val amount: String?,
			@SerializedName("created_at")
			val createdAt: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("product_id")
			val productId: Int?,
			@SerializedName("status")
			val status: String?,
			@SerializedName("user_id")
			val userId: Int?
		)

		@Keep
		data class ShippingAdress(
			@SerializedName("city")
			val city: Any?,
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
			val state: Any?,
			@SerializedName("street_address")
			val streetAddress: String?,
			@SerializedName("type")
			val type: String?,
			@SerializedName("user_id")
			val userId: Int?
		)

		@Keep
		data class User(
			@SerializedName("email")
			val email: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("name")
			val name: String?,
			@SerializedName("profile_image")
			val profileImage: String?,
			@SerializedName("seller_verification")
			val sellerVerification: Boolean?,
			@SerializedName("username")
			val username: String?
		)
	}
}