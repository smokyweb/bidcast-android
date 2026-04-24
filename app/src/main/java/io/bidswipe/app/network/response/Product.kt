package io.bidswipe.app.network.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.response.Product.Category
import java.io.Serializable

// MC task cmobr8v240061fjhgeiatidu7 — Android port of the iOS PWA live-shop
// products decode fix. Backend/PWA serializes these fields in snake_case;
// previously the Gson annotations here used camelCase, so live-room products
// dropped most of their metadata when decoded on Android just like iOS.
data class Product(
	@SerializedName("accept_offers")
	val acceptOffers: Boolean?,
	@SerializedName("auction")
	val auction: Boolean?,
	@SerializedName("bid_count")
	val bidCount: Int?,
	@SerializedName("category")
	val category: Category?,
	@SerializedName("created_at")
	val createdAt: String?,
	@SerializedName("description")
	val description: String?,
	@SerializedName("flash_sale")
	val flashSale: Boolean?,
	@SerializedName("hazardous_material")
	val hazardousMaterial: Boolean?,
	@SerializedName("height")
	val height: Double?,
	@SerializedName("id")
	val id: Int?,
	@SerializedName("images")
	val images: List<String?>?,
	@SerializedName("length")
	val length: Double?,
	@SerializedName("mail_class")
	val mailClass: String?,
	@SerializedName("pricing")
	val pricing: String?,
	@SerializedName("processing_category")
	val processingCategory: String?,
	@SerializedName("product_condition")
	val productCondition: String?,
	@SerializedName("product_show")
	val productShow: String?,
	@SerializedName("purchased_quantity")
	val purchasedQuantity: String?,
	@SerializedName("quantity")
	val quantity: String?,
	@SerializedName("reserve_for_live")
	val reserveForLive: Boolean?,
	@SerializedName("shipping_profile_id")
	val shippingProfileId: Int?,
	@SerializedName("sku")
	val sku: String?,
	@SerializedName("status")
	val status: String?,
	@SerializedName("sub_category_id")
	val subCategoryId: Any?,
	@SerializedName("thumbnail")
	val thumbnail: List<String?>?,
	@SerializedName("title")
	val title: String?,
	@SerializedName("type")
	val type: Any?,
	@SerializedName("user")
	val user: User?,
	@SerializedName("user_id")
	val userId: Int?,
	@SerializedName("variant")
	val variant: Any?,
	@SerializedName("videos")
	val videos: List<String?>?,
	@SerializedName("weight")
	val weight: Double?,
	@SerializedName("width")
	val width: Double?,
	var selected: Boolean = false,
	var isCurrent: Boolean? = false,
) : Serializable {

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
	) : Serializable

	@Keep
	data class User(
		@SerializedName("email")
		val email: String?,
		@SerializedName("id")
		val id: Int?,
		@SerializedName("name")
		val name: String?,
		@SerializedName("profile_image")
		val profileImage: String?,
		@SerializedName("username")
		val username: Any?
	) : Serializable
}


fun Product.toLiveShowProduct() = LiveShowModel.Product(
	category = this.category?.toLiveShowCategory(),
	id = this.id?.toString(),
	image = this.images?.firstOrNull() ?: "",
	status = this.status ?: "live",
	name = this.title,
	isCurrent = false,
	price = this.pricing
)

fun Category.toLiveShowCategory(): LiveShowModel.Category {
	return LiveShowModel.Category(
		id = this.id,
		image = this.image ?: "",
		name = this.name,
		thumbnail = this.thumbnail ?: ""
	)

}