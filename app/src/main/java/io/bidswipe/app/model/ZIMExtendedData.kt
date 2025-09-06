package io.bidswipe.app.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ZIMExtendedData(
	@SerializedName("userImage")
	val userImage : String? = null ,

	@SerializedName("userId")
	val userId : String? = null ,

	@SerializedName("userName")
	val userName : String? = null ,
) {
	fun toJson() : String {
		return gson.toJson(this)
	}

	companion object {
		private val gson = Gson()

		fun fromJson(json : String) : ZIMExtendedData {
			return gson.fromJson(json , ZIMExtendedData::class.java)
		}
	}
}
