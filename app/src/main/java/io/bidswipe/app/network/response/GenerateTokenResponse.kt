package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GenerateTokenResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?,
) {
    @Keep
    data class Data(
        @SerializedName("app_id")
        val appId: String?,
        @SerializedName("app_sing")
        val appSing: String?,
        @SerializedName("room_id")
        val roomId: String?,
        @SerializedName("token")
        val token: String?,
        @SerializedName("user_id")
        val userId: Int?,
    )
}