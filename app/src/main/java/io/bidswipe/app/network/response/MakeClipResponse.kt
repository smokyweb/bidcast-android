package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class MakeClipResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("clip_url")
        val clipUrl: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("show_id")
        val showId: String?,
        @SerializedName("user_id")
        val userId: String?
    )
}