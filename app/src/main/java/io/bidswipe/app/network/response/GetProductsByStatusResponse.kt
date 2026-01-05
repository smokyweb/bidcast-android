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
	@Keep
	data class Data(
		@SerializedName("card_id")
		val cardId : String? ,
		@SerializedName("created_at")
		val createdAt : String? ,
		@SerializedName("gift_msg")
		val giftMsg : String? ,
		@SerializedName("gift_user_id")
		val giftUserId : Int? ,
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("order_id")
		val orderId : String? ,
		@SerializedName("product")
		val product : Product? ,
		@SerializedName("product_id")
		val productId : Int? ,
		@SerializedName("promo_code")
		val promoCode : String? ,
		@SerializedName("send_as_gift")
		val sendAsGift : Boolean? ,
		@SerializedName("shipping_address")
		val shippingAddress : String? ,
		@SerializedName("status")
		val status : String? ,
		@SerializedName("user")
		val user : User? ,
		@SerializedName("user_id")
		val userId : Int? ,
	) {

		@Keep
		data class User(
			@SerializedName("bio")
			val bio : String? ,
			@SerializedName("email")
			val email : String? ,
			@SerializedName("first_name")
			val firstName : String? ,
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("is_active")
			val isActive : Boolean? ,
			@SerializedName("last_name")
			val lastName : String? ,
			@SerializedName("name")
			val name : String? ,
			@SerializedName("profile_image")
			val profileImage : String? ,
			@SerializedName("referral_code")
			val referralCode : String? ,
			@SerializedName("role_id")
			val roleId : Int? ,
			@SerializedName("thumbnail")
			val thumbnail : Any? ,
			@SerializedName("username")
			val username : String? ,
		)
	}
}