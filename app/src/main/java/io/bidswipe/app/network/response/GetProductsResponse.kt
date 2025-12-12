package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class GetProductsResponse(
	@SerializedName("currentPage")
	val currentPage: Int?,
	@SerializedName("data")
	val `data`: List<Data?>?,
	@SerializedName("error_type")
	val errorType: String?,
	@SerializedName("message")
	val message: String?,
	@SerializedName("perPage")
	val perPage: Int?,
	@SerializedName("status")
	val status: String?,
	@SerializedName("total")
	val total: Int?,
	@SerializedName("totalPage")
	val totalPage: Int?
) {
	@Keep
	data class Data(
		@SerializedName("bids")
		val bids: Int?,
		@SerializedName("category")
		val category: String?,
		@SerializedName("condition")
		val condition: String?,
		@SerializedName("id")
		val id: Int?,
		@SerializedName("image")
		val image: String?,
		@SerializedName("price")
		val price: String?,
		@SerializedName("quantity")
		val quantity: String?,
		@SerializedName("seller_id")
		val sellerId: Int?,
		@SerializedName("seller_name")
		val sellerName: String?,
		@SerializedName("status")
		val status: String?,
		@SerializedName("thumbanail")
		val thumbanail: String?,
		@SerializedName("title")
		val title: String?,
		var selected : Boolean = false

	): Serializable
}