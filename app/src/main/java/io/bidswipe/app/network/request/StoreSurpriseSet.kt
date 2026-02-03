package io.bidswipe.app.network.request


import com.google.gson.annotations.SerializedName

data class StoreSurpriseSet(
    @SerializedName("type")
    val type: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("price")
    val price: Double?,
    @SerializedName("shipping_profile_id")
    val shippingProfileId: Int?,
    @SerializedName("items")
    val items: List<SurpriseProductModel?>?,
    @SerializedName("auto_randomizer")
val autoRandomizer: Int?,
    @SerializedName("quick_spin")
    val quickSpin: Int?,
)

data class SurpriseProductModel(
    @SerializedName("description")
    val description: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("quantity")
    val quantity: Int?
)
