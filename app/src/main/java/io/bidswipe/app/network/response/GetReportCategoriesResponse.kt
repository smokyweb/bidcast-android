package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetReportCategoriesResponse(
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
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("updated_at")
        val updatedAt: String?
    )
}