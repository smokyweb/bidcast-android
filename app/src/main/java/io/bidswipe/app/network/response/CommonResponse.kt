package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CommonResponse(
	@SerializedName("data")
	val `data` : Any? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("status")
	val status : String? ,
)