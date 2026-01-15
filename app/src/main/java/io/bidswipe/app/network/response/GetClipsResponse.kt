package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetClipsResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("clip_url")
        val clipUrl: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_public")
        val isPublic: String?,
        @SerializedName("show_id")
        val showId: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}