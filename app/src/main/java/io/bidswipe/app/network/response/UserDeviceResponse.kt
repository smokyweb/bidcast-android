package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class UserDeviceResponse(
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
        @SerializedName("app_version")
        val appVersion: String?,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("device_token")
        val deviceToken: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_user_loggedin")
        val isUserLoggedin: String?,
        @SerializedName("platform")
        val platform: String?,
        @SerializedName("time_zone")
        val timeZone: String?,
        @SerializedName("updated_at")
        val updatedAt: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}