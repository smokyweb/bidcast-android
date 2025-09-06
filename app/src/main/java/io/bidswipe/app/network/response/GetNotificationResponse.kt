package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetNotificationResponse(
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
		@SerializedName("created_at")
		val createdAt : String? ,
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("is_seen")
		val isSeen : Int? ,
		@SerializedName("message")
		val message : String? ,
		@SerializedName("receiver_id")
		val receiverId : Int? ,
		@SerializedName("sender_id")
		val senderId : Int? ,
		@SerializedName("title")
		val title : String? ,
		@SerializedName("type")
		val type : String? ,
		@SerializedName("updated_at")
		val updatedAt : String? ,
	)
}