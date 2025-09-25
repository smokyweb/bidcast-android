package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.utils.string
import java.io.Serializable

@Keep
data class GetMyShowResponse(
	@SerializedName("currentPage")
	val currentPage : Int? ,
	@SerializedName("data")
	val `data` : List<Data?>? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("perPage")
	val perPage : Int? ,
	@SerializedName("status")
	val status : String? ,
	@SerializedName("total")
	val total : Int? ,
	@SerializedName("totalPage")
	val totalPage : Int? ,
) {
	@Keep
	data class Data(
		@SerializedName("auction_type_id")
        val auctionTypeId: Int?,
		@SerializedName("category")
        val category: Category?,
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
		@SerializedName("product_ids")
        val productIds: List<String?>?,
        @SerializedName("products")
        val products: List<Product?>?,
		@SerializedName("room_id")
        val roomId: String?,
		@SerializedName("thumbnail")
        val thumbnail: List<String?>?,
		@SerializedName("time")
        val time: String?,
		@SerializedName("title")
        val title: String?,
		@SerializedName("user")
        val user: User?,
		@SerializedName("user_id")
        val userId: Int?,
		@SerializedName("viewer_count")
        val viewerCount: Int?,
    ) : Serializable {
		@Keep
		data class Category(
			@SerializedName("color")
			val color : String? ,
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("image")
			val image : String? ,
			@SerializedName("name")
			val name : String? ,
			@SerializedName("thumbnail")
			val thumbnail : String? ,
        ) : Serializable

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
            val height: Int?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("images")
            val images: List<String?>?,
            @SerializedName("length")
            val length: Int?,
            @SerializedName("mail_class")
            val mailClass: String?,
            @SerializedName("pricing")
            val pricing: String?,
            @SerializedName("processing_category")
            val processingCategory: String?,
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
            val weight: Int?,
            @SerializedName("width")
            val width: Int?
        ) : Serializable {

            fun toLiveShowProduct(): LiveShowModelOld.Product {
                return LiveShowModelOld.Product(
                    category = this.categoryId?.toString(),
                    id = this.id?.toString(),
                    image = this.images?.firstOrNull() ?: "",
                    status = this.status ?: "live",
                    name = this.title,
                    isCurrent = false,
                    price = this.pricing
                )
            }
        }

		@Keep
		data class User(
            @SerializedName("bio")
            val bio : String? ,
            @SerializedName("email")
            val email : String? ,
            @SerializedName("first_name")
            val firstName : String? ,
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("is_active")
            val isActive : Boolean? ,
            @SerializedName("last_name")
            val lastName : String? ,
            @SerializedName("name")
            val name : String? ,
            @SerializedName("profile_image")
            val profileImage : String? ,
            @SerializedName("rating")
            val rating: String?,
            @SerializedName("referral_code")
            val referralCode : String? ,
            @SerializedName("role_id")
            val roleId : Int? ,
            @SerializedName("thumbnail")
            val thumbnail : String? ,
            @SerializedName("username")
            val username : String? ,
            @SerializedName("is_followed")
            val isFollowed : Boolean? ,
        ) : Serializable
	}
}