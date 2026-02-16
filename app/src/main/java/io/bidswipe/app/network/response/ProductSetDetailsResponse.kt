package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class ProductSetDetailsResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("auto_randomizer")
        val autoRandomizer: Int?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_live_bid")
        val isLiveBid: Int?,
        @SerializedName("items")
        val items: List<GetSurpriseProductsResponse.Data.Item?>?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("price")
        val price: Int?,
        @SerializedName("quick_spin")
        val quickSpin: Int?,
        @SerializedName("shipping_profile_id")
        val shippingProfileId: Int?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("user_id")
        val userId: Int?,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("updated_at")
        val updatedAt: String?
    )
}