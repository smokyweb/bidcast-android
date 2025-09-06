package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetBlockedUsersResponse(
    @SerializedName("status")
    val status : Boolean ,
    @SerializedName("message")
    val message : String? ,
    @SerializedName("error_type")
    val errorType : String? ,
    @SerializedName("data")
    val `data` : Data? ,
) {
	@Keep
	data class Data(
        @SerializedName("blocked_by_me")
        val blockedByMe : List<BlockedByMe?>? ,
        @SerializedName("blocked_me")
        val blockedMe : List<BlockedByMe?>? ,
    ) {
		@Keep
		data class BlockedByMe(
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("name")
            val name : String? ,
            @SerializedName("image")
            val image : String? ,
        )
	}
}