package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class LoginResponse(
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
        @SerializedName("email")
        val email: String?,
        @SerializedName("first_name")
        val firstName: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("last_name")
        val lastName: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("role")
        val role: Role?,
        @SerializedName("role_id")
        val roleId: String?,
        @SerializedName("token")
        val token: String?
    ) {
        @Keep
        data class Role(
            @SerializedName("created_at")
            val createdAt: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("updated_at")
            val updatedAt: Any?
        )
    }
}