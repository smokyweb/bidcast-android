package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class FetchReferralResponse(
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
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("name")
		val name : String? ,
		@SerializedName("referral_code")
		val referralCode : String? ,
		@SerializedName("total_earnings")
		val totalEarnings : Int? ,
		@SerializedName("total_referred")
		val totalReferred : Int? ,
		@SerializedName("username")
		val username : Any? ,
	)
}