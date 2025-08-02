package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetCategoryResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?,
) {
    @Keep
    data class Data(
        @SerializedName("color")
        val color: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("image")
        val image: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("thumbnail")
        val thumbnail: String?,
        @SerializedName("extra_fields")
        val extraFields: List<ExtraField?>?,
    ) {
        @Keep
        data class ExtraField(
            @SerializedName("label")
            val label: String?,
            @SerializedName("type")
            val type: String?,
            @SerializedName("options")
            val options: List<String?>?
        )
    }
}