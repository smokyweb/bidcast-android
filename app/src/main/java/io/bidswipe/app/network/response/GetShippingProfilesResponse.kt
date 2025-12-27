package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetShippingProfilesResponse(
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
        @SerializedName("id")
        val id: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("size")
        val size: String?,
        @SerializedName("user_id")
        val userId: Int?,
        @SerializedName("weight")
        val weight: String?,
        @SerializedName("maxItems")
        val maxItems: Boolean?,
        @SerializedName("additionalWeight")
        val additionalWeight: Boolean?
    )
}