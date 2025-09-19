package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import io.bidswipe.app.model.LiveShowModelOld
import java.io.Serializable

@Keep
data class CreateShowResponse(
	@SerializedName("data")
	val `data` : Data? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("status")
	val status : String? ,
) {

	@Keep
	data class Data(
		@SerializedName("auction_type_id")
		val auctionTypeId: Int?,
		@SerializedName("category_id")
		val categoryId: Int?,
		@SerializedName("date")
		val date: String?,
		@SerializedName("id")
		val id: Int?,
		@SerializedName("img_thumbnail")
		val imgThumbnail: List<String?>?,
		@SerializedName("is_live")
		val isLive: Boolean?,
		@SerializedName("latest_viewer_count")
		val latestViewerCount: Int?,
		@SerializedName("product_ids")
		val productIds: List<String?>?,
		@SerializedName("products")
		val products: List<Product?>?,
		@SerializedName("started_at")
		val startedAt: Any?,
		@SerializedName("thumbnail")
		val thumbnail: List<String?>?,
		@SerializedName("time")
		val time: String?,
		@SerializedName("title")
		val title: String?,
		@SerializedName("user_id")
		val userId: Int?,
		@SerializedName("viewer_count")
		val viewerCount: Int?
	) {

		@Keep
		data class Product(
			@SerializedName("accept_offers")
			val acceptOffers: Boolean?,
			@SerializedName("category_id")
			val categoryId: Int?,
			@SerializedName("created_at")
			val createdAt: String?,
			@SerializedName("description")
			val description: String?,
			@SerializedName("flash_sale")
			val flashSale: Boolean?,
			@SerializedName("height")
			val height: Any?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("images")
			val images: List<String?>?,
			@SerializedName("length")
			val length: Any?,
			@SerializedName("mail_class")
			val mailClass: Any?,
			@SerializedName("pricing")
			val pricing: Int?,
			@SerializedName("processing_category")
			val processingCategory: Any?,
			@SerializedName("product_show")
			val productShow: String?,
			@SerializedName("purchased_quantity")
			val purchasedQuantity: Int?,
			@SerializedName("quantity")
			val quantity: Int?,
			@SerializedName("reserve_for_live")
			val reserveForLive: Boolean?,
			@SerializedName("shipping_profile_id")
			val shippingProfileId: Int?,
			@SerializedName("status")
			val status: String?,
			@SerializedName("sub_category_id")
			val subCategoryId: Int?,
			@SerializedName("thumbnail")
			val thumbnail: List<String?>?,
			@SerializedName("title")
			val title: String?,
			@SerializedName("user_id")
			val userId: Int?,
			@SerializedName("variant")
			val variant: Any?,
			@SerializedName("weight")
			val weight: Any?,
			@SerializedName("width")
			val width: Any?
		){
			fun toLiveShowProduct() : LiveShowModelOld.Product {
				return LiveShowModelOld.Product(
					category = this.categoryId?.toString() ,
					id = this.id?.toString() ,
					image = this.images?.firstOrNull() ?: "" ,
					status = this.status ?: "live" ,
					name = this.title ,
					isCurrent = false ,
					price = this.pricing?.toString()
				)
			}
		}

	}
}