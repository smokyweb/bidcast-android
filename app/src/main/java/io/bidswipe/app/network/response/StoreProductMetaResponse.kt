package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class StoreProductMetaResponse(
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
        @SerializedName("images")
        val images: List<Image?>?,
        @SerializedName("videos")
        val videos: List<Video?>?
    ) {
        @Keep
        data class Image(
            @SerializedName("images")
            val images: String?,
            @SerializedName("thumbnail")
            val thumbnail: String?
        )

        @Keep
        data class Video(
            @SerializedName("videos")
            val videos: String?
        )
    }
}