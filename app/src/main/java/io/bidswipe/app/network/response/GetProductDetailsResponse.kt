package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetProductDetailsResponse(
	@SerializedName("data")
	val `data` : Data? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("status")
	val status : String? ,
) {
	@Keep
	data class Data(
		@SerializedName("accept_offers")
		val acceptOffers : Boolean? ,
		@SerializedName("category_id")
		val categoryId : Int? ,
		@SerializedName("description")
		val description : String? ,
		@SerializedName("flash_sale")
		val flashSale : Boolean? ,
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("images")
		val images : List<String?>? ,
		@SerializedName("offer")
		val offer : Offer? ,
		@SerializedName("pricing")
		val pricing : String? ,
		@SerializedName("quantity")
		val quantity : Int? ,
		@SerializedName("reserve_for_live")
		val reserveForLive : Boolean? ,
		@SerializedName("shipping_adress")
		val shippingAdress : ShippingAdress? ,
		@SerializedName("shipping_profile_id")
		val shippingProfileId : Int? ,
		@SerializedName("status")
		val status : String? ,
		@SerializedName("title")
		val title : String? ,
		@SerializedName("user")
		val user : User? ,
		@SerializedName("user_id")
		val userId : Int? ,
		@SerializedName("created_at")
		val createdAt : String? ,
	) {
		@Keep
		data class Offer(
			@SerializedName("amount")
			val amount : Double? ,
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("product_id")
			val productId : Int? ,
			@SerializedName("status")
			val status : String? ,
			@SerializedName("user_id")
			val userId : Int? ,
		)

		@Keep
		data class ShippingAdress(
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("is_default")
			val isDefault : Boolean? ,
			@SerializedName("name")
			val name : String? ,
			@SerializedName("phone_number")
			val phoneNumber : String? ,
			@SerializedName("pincode")
			val pincode : String? ,
			@SerializedName("street_address")
			val streetAddress : String? ,
			@SerializedName("type")
			val type : String? ,
			@SerializedName("user_id")
			val userId : Int? ,
		)

		@Keep
		data class User(
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("name")
			val name : String? ,
			@SerializedName("profile_image")
			val profileImage : String? ,
			@SerializedName("seller_verification")
			val sellerVerification : Boolean? ,
			@SerializedName("username")
			val username : String? ,
		)
	}
}