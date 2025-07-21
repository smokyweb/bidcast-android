package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CreateShowResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?,
) {
    @Keep
    data class Data(
        @SerializedName("auction_type_id")
        val auctionTypeId: Int?,
        @SerializedName("category_id")
        val categoryId: Int?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("img_thumbnail")
        val imgThumbnail: List<String?>?,
        @SerializedName("product_ids")
        val productIds: List<String?>?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("time")
        val time: String?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("user_id")
        val userId: Int?,
    )
}