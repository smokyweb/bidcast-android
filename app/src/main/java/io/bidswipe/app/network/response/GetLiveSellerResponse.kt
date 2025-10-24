package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetLiveSellerResponse(
    @SerializedName("status")
    val status: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("data")
    val `data`: List<Data?>?
){
    @Keep
    data class Data(
        @SerializedName("id")
        val id: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("email")
        val email: String?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("room_id")
        val roomId: String?
    )
}