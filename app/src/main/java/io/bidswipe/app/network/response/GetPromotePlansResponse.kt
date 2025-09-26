package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPromotePlansResponse(
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
        @SerializedName("colors")
        val colors: Colors?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("gradient_colors")
        val gradientColors: String?,
        @SerializedName("icon")
        val icon: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("price")
        val price: String?,
        @SerializedName("sub_title")
        val subTitle: String?,
        @SerializedName("title")
        val title: String?
    ) {
        @Keep
        data class Colors(
            @SerializedName("end")
            val end: String?,
            @SerializedName("start")
            val start: String?
        )
    }
}