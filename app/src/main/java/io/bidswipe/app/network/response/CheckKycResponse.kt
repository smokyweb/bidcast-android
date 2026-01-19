package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CheckKycResponse(
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
		@SerializedName("created")
		val created : Int? ,
		@SerializedName("expires_at")
		val expiresAt : Int? ,
		@SerializedName("kyc_details")
		val kycDetails : KycDetails? ,
		@SerializedName("kyc_status")
		val kycStatus : String? ,
		@SerializedName("msg")
		val msg : String? ,
		@SerializedName("object")
		val objectX : String? ,
		@SerializedName("res")
		val res : Boolean? ,
		@SerializedName("link")
		val url : String? ,
	) {
		@Keep
		data class KycDetails(
			@SerializedName("account_id")
			val accountId : String? ,
			@SerializedName("bank_id")
			val bankId : String? ,
			@SerializedName("city")
			val city : Any? ,
			@SerializedName("country")
			val country : Any? ,
			@SerializedName("currency")
			val currency : String? ,
			@SerializedName("phone")
			val phone : Any? ,
			@SerializedName("postal_code")
			val postalCode : Any? ,
			@SerializedName("rounting_number")
			val rountingNumber : String? ,
		)
	}
}