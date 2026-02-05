package io.bidswipe.app.network.response


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class GetSurpriseProductsResponse(
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
    @Parcelize
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
        val items: List<Item?>?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("price")
        val price: Double?,
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
        var selected:Boolean?=false
    ): Parcelable {
        @Parcelize
        data class Item(
            @SerializedName("description")
            val description: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("product_set_id")
            val productSetId: Int?,
            @SerializedName("quantity")
            val quantity: Int?,
            @SerializedName("sold_quantity")
            val soldQuantity: Int?,
            @SerializedName("status")
            val status: String?,
            @SerializedName("units")
            val units: List<Unit?>?
        ): Parcelable {
            @Parcelize
            data class Unit(
                @SerializedName("description")
                val description: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("name")
                val name: String?,
                @SerializedName("price")
                val price: Double?,
                @SerializedName("product_set_item_id")
                val productSetItemId: Int?,
                @SerializedName("status")
                val status: String?
            ): Parcelable
        }
    }
}