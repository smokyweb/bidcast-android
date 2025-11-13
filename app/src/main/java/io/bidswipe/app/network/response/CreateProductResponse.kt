package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class CreateProductResponse(
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
        val pricing: String?,
        @SerializedName("quantity")
        val quantity: String?,
        @SerializedName("reserve_for_live")
        val reserveForLive: Boolean?,
        @SerializedName("shipping_profile_id")
        val shippingProfileId: Int?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("sub_category")
        val subCategory: SubCategory?,
        @SerializedName("sub_category_id")
        val subCategoryId: Int?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("user_id")
        val userId: Int?,
        @SerializedName("variant")
        val variant: List<Variant?>?
    ) {
        @Keep
        data class Category(
            @SerializedName("color")
            val color: String?,
            @SerializedName("deleted_at")
            val deletedAt: Any?,
            @SerializedName("extra_fields")
            val extraFields: List<Any?>?,
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
        data class SubCategory(
            @SerializedName("category_id")
            val categoryId: Int?,
            @SerializedName("color")
            val color: String?,
            @SerializedName("deleted_at")
            val deletedAt: Any?,
            @SerializedName("extra_fields")
            val extraFields: List<Any?>?,
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
        data class Variant(
            @SerializedName("title")
            val title: String?,
            @SerializedName("value")
            val value: String?
        )
    }
}