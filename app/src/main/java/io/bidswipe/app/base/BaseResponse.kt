package io.bidswipe.app.base

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

open class BaseResponse(
	@Keep
	@SerializedName("message")
	val message: String? = null,
	@Keep
	@SerializedName("status")
	val status: String? = null,
	@Keep
	@SerializedName("success-code")
	val successCode: Int? = null,
	@Keep
	@SerializedName("error_type")
	val errorType: String? = null,
	@Keep
	@SerializedName("total_records")
	val totalRecords: Int? = null,
	@Keep
	@SerializedName("total_pages")
	val totalPages: Int? = null,
	@Keep
	@SerializedName("current_page")
	val currentPage: Int? = null,
	@Keep
	@SerializedName("per_page")
	val perPage: Int? = null,
)