package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CreateOrderResponse(
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
		@SerializedName("card_id")
		val cardId : String? ,
		@SerializedName("created_at")
		val createdAt : String? ,
		@SerializedName("gift_msg")
		val giftMsg : Any? ,
		@SerializedName("gift_user_id")
		val giftUserId : Any? ,
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("order_id")
		val orderId : String? ,
		@SerializedName("product_id")
		val productId : Int? ,
		@SerializedName("promo_code")
		val promoCode : Any? ,
		@SerializedName("send_as_gift")
		val sendAsGift : Boolean? ,
		@SerializedName("shipping_address")
		val shippingAddress : String? ,
		@SerializedName("status")
		val status : String? ,
		@SerializedName("user_id")
		val userId : Int? ,
	)
}