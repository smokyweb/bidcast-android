package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class TestResponse(
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("perPage")
    val perPage: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?
) {
    @Keep
    data class Data(
        @SerializedName("acceptOffers")
        val acceptOffers: Boolean?,
        @SerializedName("auction")
        val auction: Boolean?,
        @SerializedName("bid_count")
        val bidCount: Int?,
        @SerializedName("category")
        val category: Category?,
        @SerializedName("createdAt")
        val createdAt: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("flashSale")
        val flashSale: Boolean?,
        @SerializedName("hazardousMaterial")
        val hazardousMaterial: Boolean?,
        @SerializedName("height")
        val height: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("images")
        val images: List<String?>?,
        @SerializedName("length")
        val length: Int?,
        @SerializedName("mailClass")
        val mailClass: String?,
        @SerializedName("pricing")
        val pricing: String?,
        @SerializedName("processingCategory")
        val processingCategory: String?,
        @SerializedName("productCondition")
        val productCondition: String?,
        @SerializedName("productShow")
        val productShow: String?,
        @SerializedName("purchasedQuantity")
        val purchasedQuantity: String?,
        @SerializedName("quantity")
        val quantity: String?,
        @SerializedName("reserveForLive")
        val reserveForLive: Boolean?,
        @SerializedName("shippingProfileId")
        val shippingProfileId: Int?,
        @SerializedName("sku")
        val sku: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("subCategoryId")
        val subCategoryId: Any?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("type")
        val type: Any?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("userId")
        val userId: Int?,
        @SerializedName("variant")
        val variant: Any?,
        @SerializedName("videos")
        val videos: List<String?>?,
        @SerializedName("weight")
        val weight: Int?,
        @SerializedName("width")
        val width: Int?
    ) {
        @Keep
        data class Category(
            @SerializedName("color")
            val color: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("image")
            val image: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("thumbnail")
            val thumbnail: String?
        )

        @Keep
        data class User(
            @SerializedName("email")
            val email: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profileImage")
            val profileImage: String?,
            @SerializedName("username")
            val username: Any?
        )
    }
}