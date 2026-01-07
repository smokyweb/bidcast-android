package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import org.json.JSONObject

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
        val showId: String?,
        @SerializedName("room_id")
        val roomId: String?
    )


    data class Users(
        @SerializedName("email")
        val email: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("username")
        val userName: String?,
        @SerializedName("profile_image")
        val profileImage: Any?
    ){

		companion object{
			fun fromJson(json: JSONObject) = Users(
				email = json.optString("email", null),
				id = json.optInt("id", 0),
				name = json.optString("name", null),
				userName = json.optString("username", null),
				profileImage = json.optString("profile_image", null))
		}

	}


}