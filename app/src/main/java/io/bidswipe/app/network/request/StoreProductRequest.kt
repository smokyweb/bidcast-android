package io.bidswipe.app.network.request

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class StoreProductRequest(
	@SerializedName("category_id")
	val categoryId : String? ,
	@SerializedName("title")
	val title : String? ,
	@SerializedName("description")
	val description : String? ,
	@SerializedName("quantity")
	val quantity : String? ,
	@SerializedName("pricing")
	val pricing : String? ,
	@SerializedName("flash_sale")
	val flashSale : Boolean? ,
	@SerializedName("accept_offers")
	val acceptOffers : Boolean? ,
	@SerializedName("reserve_for_live")
	val reserveForLive : Boolean? ,
	@SerializedName("shipping_profile_id")
	val shippingProfileId : String? ,
	@SerializedName("status")
	val status : String? ,
	@SerializedName("images")
	val images : List<Map<String , String?>>? = null ,
	@SerializedName("videos")
	val videos : List<Map<String , String?>>? = null ,
	@SerializedName("sub_category_id")
	val subCategoryId : Int? = null ,
	@SerializedName("variant")
	val variant : List<Map<String? , Any?>>? = null ,
	@SerializedName("width")
	val width : String? = null ,
	@SerializedName("height")
	val height : String? = null ,
	@SerializedName("length")
	val length : String? = null ,
	@SerializedName("weight")
	val weight : String? = null ,
	@SerializedName("mail_class")
	val mailClass : String? = null ,
	@SerializedName("processing_category")
	val processingCategory : String? = null ,
	@SerializedName("product_condition")
	val productCondition : String? = null ,
	@SerializedName("hazardous_material")
	val hazardousMaterial : Boolean? = null ,
	@SerializedName("sku")
	val sku : String? = null,
	@SerializedName("cost_per_item")
	val costPerItem : String? = null ,
	)