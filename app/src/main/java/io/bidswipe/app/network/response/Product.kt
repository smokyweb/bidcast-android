package io.bidswipe.app.network.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.response.Product.Category
import java.io.Serializable

data class Product(
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
	val height: String?,
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
	val weight: String?,
	@SerializedName("width")
	val width: Int?,
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
		@SerializedName("profileImage")
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