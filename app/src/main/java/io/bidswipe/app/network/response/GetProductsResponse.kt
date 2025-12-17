package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class GetProductsResponse(
	@SerializedName("currentPage")
	val currentPage: Int?,
	@SerializedName("data")
	val products : List<Product?>?,
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
)