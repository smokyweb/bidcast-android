package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetFreebieObject(
    @SerializedName("freebie")
    val freebie: Freebie?,
    @SerializedName("users_list")
    val usersList: List<Users?>?
) {
    data class Freebie(
        @SerializedName("duration")
        val duration: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("product_id")
        val productId: Int?,
        @SerializedName("show_id")
        val showId: String?
    )
    data class Users(
        @SerializedName("email")
        val email: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("profile_image")
        val profileImage: Any?
    )
}