package io.bidswipe.app.network.response


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

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
        @SerializedName("additionalWeight")
        val additionalWeight: Boolean?,
        @SerializedName("height")
        val height: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("increment_weight")
        val incrementWeight: String?,
        @SerializedName("increment_weight_scale")
        val incrementWeightScale: String?,
        @SerializedName("length")
        val length: String?,
        @SerializedName("max_item_unit")
        val maxItemUnit: String?,
        @SerializedName("maxItems")
        val maxItems: Boolean?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("scale")
        val scale: String?,
        @SerializedName("size")
        val size: String?,
        @SerializedName("user_id")
        val userId: Int?,
        @SerializedName("weight")
        val weight: String?,
        @SerializedName("width")
        val width: String?
    )
}