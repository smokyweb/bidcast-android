package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetUSPSboxDimensionsResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("great _for")
        val greatFor: String?,
        @SerializedName("height")
        val height: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("length")
        val length: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("shipping_price")
        val shippingPrice: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("unit")
        val unit: String?,
        @SerializedName("width")
        val width: String?
    )
}