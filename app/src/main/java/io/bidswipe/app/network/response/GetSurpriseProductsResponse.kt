package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetSurpriseProductsResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("perPage")
    val perPage: Int?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?
) {
    data class Data(
        @SerializedName("auto_randomizer")
        val autoRandomizer: Int?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("items")
        val items: List<Item?>?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("price")
        val price: Int?,
        @SerializedName("quick_spin")
        val quickSpin: Int?,
        @SerializedName("shipping_profile_id")
        val shippingProfileId: Int?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("user_id")
        val userId: Int?
    ) {
        data class Item(
            @SerializedName("description")
            val description: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("product_surprise_id")
            val productSurpriseId: Int?,
            @SerializedName("quantity")
            val quantity: Int?
        )
    }
}