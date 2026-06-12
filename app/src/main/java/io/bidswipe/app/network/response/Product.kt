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
	// Basecamp #9933973683 return (2026-05-29): flash-sale price + window for
	// explore-page cards. The list endpoint may return these in snake_case (same
	// shape as the product-detail endpoint). Nullable so old/non-flash records
	// don’t break.
	@SerializedName("flash_sale_price")
	val flashSalePrice: Double? = null,
	@SerializedName("flash_sale_ends_at")
	val flashSaleEndsAt: String? = null,
	@SerializedName("hazardousMaterial")
	val hazardousMaterial: Boolean?,
	@SerializedName("height")
	val height: Double?,
	@SerializedName("id")
	val id: Int?,
	@SerializedName("images")
	val images: List<String?>?,
	@SerializedName("length")
	val length: Double?,
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
	@SerializedName(value = "purchasedQuantity", alternate = ["purchased_quantity"])
	val purchasedQuantity: String?,
	@SerializedName("quantity")
	val quantity: String?,
	@SerializedName(value = "reserveForLive", alternate = ["reserve_for_live"])
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
	val weight: Double?,
	@SerializedName("width")
	val width: Double?,
	var selected: Boolean = false,
	var isCurrent: Boolean? = false,
	// Basecamp #9991372302: per-product quantity to sell in this show (default = full stock).
	// Stored on the Product so it survives ViewModel rotation and list-diffing.
	var streamQuantity: Int = 0,
	// Basecamp #9933847997 (2026-05-29): pre-bid fields returned by getShowDetails
	// and the product list on upcoming/live shows.
	@SerializedName("sale_format")
	val saleFormat: String? = null,
	@SerializedName("is_auction")
	val isAuction: Boolean? = null,
	@SerializedName("pre_bid_allowed")
	val preBidAllowed: Boolean? = null,
	@SerializedName("pre_bid_schedule_show_id")
	val preBidScheduleShowId: Int? = null,
	@SerializedName("my_pre_bid")
	val myPreBid: Double? = null,
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


/**
 * Pricing-format classification shared across surfaces (parity with the PWA /
 * iOS). A product is a *Live Auction* when [Product.auction],
 * [Product.reserveForLive], or [Product.isAuction] is true, or when a sale
 * format text flag resolves to auction/live auction. This keeps older rows
 * whose only format signal is `type=live` out of the Buy Now tab.
 *
 * Basecamp #9954326658 (format parity).
 */
fun Product.isLiveAuctionFormat(): Boolean {
	if (auction == true || reserveForLive == true || isAuction == true) return true
	if (type?.toString().isLiveAuctionText()) return true
	if (saleFormat.isLiveAuctionText()) return true
	return false
}

private fun String?.isLiveAuctionText(): Boolean {
	val normalized = this
		?.lowercase()
		?.replace("-", "_")
		?.replace(" ", "_")
		.orEmpty()
	return normalized in setOf("live", "auction", "live_auction", "reserve_for_live", "reserveforlive")
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
