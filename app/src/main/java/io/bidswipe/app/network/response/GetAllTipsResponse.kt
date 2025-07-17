package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetAllTipsResponse(
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
        @SerializedName("example")
        val example: List<String?>?,
        @SerializedName("tips")
        val tips: List<Tip?>?
    ) {
        @Keep
        data class Tip(
            @SerializedName("description")
            val description: String?,
            @SerializedName("icon")
            val icon: String?,
            @SerializedName("title")
            val title: String?,
            @SerializedName("color")
            val color: String?
        )

    }
}