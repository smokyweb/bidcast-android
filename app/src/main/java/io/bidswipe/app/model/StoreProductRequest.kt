package io.bidswipe.app.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class StoreProductRequest(
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("quantity")
    val quantity: String?,
    @SerializedName("pricing")
    val pricing: String?,
    @SerializedName("flash_sale")
    val flashSale: String?,
    @SerializedName("accept_offers")
    val acceptOffers: String?,
    @SerializedName("reserve_for_live")
    val reserveForLive: String?,
    @SerializedName("shipping_profile_id")
    val shippingProfileId: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("images")
    val images: List<Map<String, String?>>? = null ,
    @SerializedName("sub_category_id")
    val subCategoryId: Int? = null,
    @SerializedName("variant")
    val variant: List<Map<String?, Any?>>? = null,
    @SerializedName("width")
    val width: String? = null,
    @SerializedName("height")
    val height: String? = null,
    @SerializedName("length")
    val length: String? = null,
    @SerializedName("weight")
    val weight: String? = null,
    @SerializedName("mail_class")
    val mailClass: String? = null,
    @SerializedName("processing_category")
    val processingCategory: String? = null,

    )