package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPurchaseDetail(
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
		@SerializedName("product")
		val product : Product?,
		@SerializedName("shippingAddress")
		val shippingAddress : ShippingAddress?,
		@SerializedName("shipping_charges")
		val shippingCharges : String?,
		@SerializedName("sub_total")
		val subTotal : String?,
		@SerializedName("tax_amount")
		val taxAmount : String?,
		@SerializedName("tax_percent")
		val taxPercent : String?,
		@SerializedName("total")
		val total : String?,
		@SerializedName("discount_amount")
		val discountAmount : String?,
	) {
		@Keep
		data class Product(
			@SerializedName("accept_offers")
			val acceptOffers : Boolean? ,
			@SerializedName("category_id")
			val categoryId : Int? ,
			@SerializedName("created_at")
			val createdAt : String? ,
			@SerializedName("description")
			val description : String? ,
			@SerializedName("flash_sale")
			val flashSale : Boolean? ,
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("images")
			val images : List<String?>? ,
			@SerializedName("pricing")
			val pricing : String? ,
			@SerializedName("quantity")
			val quantity : Int? ,
			@SerializedName("reserve_for_live")
			val reserveForLive : Boolean? ,
			@SerializedName("shipping_profile_id")
			val shippingProfileId : Int? ,
			@SerializedName("status")
			val status : String? ,
			@SerializedName("title")
			val title : String? ,
			@SerializedName("user_id")
			val userId : Int? ,
		)

		@Keep
		data class ShippingAddress(
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
	}
}