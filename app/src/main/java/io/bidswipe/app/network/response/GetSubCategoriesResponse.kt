package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetSubCategoriesResponse(
    @SerializedName("status")
    val status: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("data")
    val `data`: List<Data?>?
) {
    @Keep
    data class Data(
        @SerializedName("id")
        val id: Int?,
        @SerializedName("category_id")
        val categoryId: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("image")
        val image: String?,
        @SerializedName("extra_fields")
        val extraFields: List<ExtraField?>?,
        @SerializedName("thumbnail")
        val thumbnail: String?,
        @SerializedName("color")
        val color: String?
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