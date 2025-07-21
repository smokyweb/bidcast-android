package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetUserProfileResponse(
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
        @SerializedName("bio")
        val bio: String?,
        @SerializedName("email")
        val email: String?,
        @SerializedName("first_name")
        val firstName: String?,
        @SerializedName("follower_count")
        val followerCount: Int?,
        @SerializedName("following_count")
        val followingCount: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_following")
        val isFollowing: Boolean?,
        @SerializedName("last_name")
        val lastName: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("role_id")
        val roleId: Int?,
        @SerializedName("username")
        val username: String?,
    )
}