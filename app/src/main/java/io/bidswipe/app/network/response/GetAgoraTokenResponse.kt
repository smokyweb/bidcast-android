package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetAgoraTokenResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("channel")
        val channel: String?,
        @SerializedName("expires_at")
        val expiresAt: Int?,
        @SerializedName("token")
        val token: String?,
        @SerializedName("uid")
        val uid: Int?
    )
}