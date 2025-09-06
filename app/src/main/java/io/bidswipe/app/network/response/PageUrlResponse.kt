package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class PageUrlResponse(
	@SerializedName("status")
	val status : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("data")
	val `data` : Data? ,
) {
	@Keep
	data class Data(
		@SerializedName("url")
		val url : String? ,
	)
}