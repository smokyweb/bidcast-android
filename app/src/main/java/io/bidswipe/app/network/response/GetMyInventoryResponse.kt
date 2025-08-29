package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class GetMyInventoryResponse(
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
    val totalPage: Int?,
) {
    @Keep
    data class Data(
        @SerializedName("accept_offers")
        val acceptOffers: Boolean?,
        @SerializedName("category")
        val category: Category?,
        @SerializedName("category_id")
        val categoryId: Int?,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("flash_sale")
        val flashSale: Boolean?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("images")
        val images: List<String?>?,
        @SerializedName("pricing")
        val pricing: Double?,
        @SerializedName("product_show")
        val productShow: String?,
        @SerializedName("purchased_quantity")
        val purchasedQuantity: Int?,
        @SerializedName("quantity")
        val quantity: Int?,
        @SerializedName("width")
        val width: Int?,
        @SerializedName("height")
        val height: Int?,
        @SerializedName("length")
        val length: Int?,
        @SerializedName("weight")
        val weight: Int?,
        @SerializedName("mailClass")
        val mailClass: String?,
        @SerializedName("reserve_for_live")
        val reserveForLive: Boolean?,
        @SerializedName("shipping_profile_id")
        val shippingProfileId: Int?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("sub_category")
        val subCategory:SubCategory?,
        @SerializedName("sub_category_id")
        val subCategoryId: String?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("user_id")
        val userId: Int?,
        var selected: Boolean? = false
    ): Serializable{
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
            val thumbnail: Any?
        ) : Serializable

        @Keep
        data class SubCategory(
            @SerializedName("category_id")
            val categoryId: Int?,
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
        ): Serializable
    }
}