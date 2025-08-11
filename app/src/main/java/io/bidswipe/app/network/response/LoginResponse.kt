package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class LoginResponse(
    @SerializedName("status")
    val status: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("data")
    val `data`: Data?,
) {
    @Keep
    data class Data(
        @SerializedName("id")
        val id: Int?,
        @SerializedName("role_id")
        val roleId: String?,
        @SerializedName("first_name")
        val firstName: String?,
        @SerializedName("last_name")
        val lastName: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("email")
        val email: String?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("is_FirsttimeLogin")
        val isFirsttimeLogin: Boolean?,
        @SerializedName("token")
        val token: String?,
        @SerializedName("role")
        val role: Role?,
    ) {
        @Keep
        data class Role(
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("created_at")
            val createdAt: Any?,
            @SerializedName("updated_at")
            val updatedAt: Any?,
        )
    }
}