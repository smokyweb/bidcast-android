package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetShippingAddressResponse(
	@SerializedName("data")
	val `data` : List<Data?>? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("status")
	val status : String? ,
) {
	@Keep
	data class Data(
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
		// MC sub-task cmp4932vk00l13mx1du6mmebo (Trey 2026-05-13): optional
		// second street-address line (apartment / unit / suite). Backend
		// column shipping_addresses.address_line_2 was added the same session.
		@SerializedName("address_line_2")
		val addressLine2 : String? ,
		@SerializedName("city")
		val city : String? ,
		@SerializedName("state")
		val state : String? ,
		@SerializedName("type")
		val type : String? ,
		@SerializedName("user_id")
		val userId : Int? ,
		var selected : Boolean = false ,
	)
}