package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetHowToSellResponse(
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
		@SerializedName("description")
		val description : String? ,
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("image")
		val image : String? ,
		@SerializedName("title")
		val title : String? ,
	)
}