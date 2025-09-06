package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class TestResponse(
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
        val auctionTypeId : Int? ,
        @SerializedName("bid_won_user")
        val bidWonUser : Any? ,
        @SerializedName("category")
        val category : Category? ,
        @SerializedName("category_id")
        val categoryId : Int? ,
        @SerializedName("date")
        val date : String? ,
        @SerializedName("highest_bid")
        val highestBid : Any? ,
        @SerializedName("id")
        val id : Int? ,
        @SerializedName("img_thumbnail")
        val imgThumbnail : List<String?>? ,
        @SerializedName("is_live")
        val isLive : Boolean? ,
        @SerializedName("latest_viewer_count")
        val latestViewerCount : Int? ,
        @SerializedName("product_ids")
        val productIds : List<String?>? ,
        @SerializedName("products")
        val products : List<Product?>? ,
        @SerializedName("started_at")
        val startedAt : Any? ,
        @SerializedName("thumbnail")
        val thumbnail : List<String?>? ,
        @SerializedName("time")
        val time : String? ,
        @SerializedName("title")
        val title : String? ,
        @SerializedName("user")
        val user : User? ,
        @SerializedName("user_id")
        val userId : Int? ,
        @SerializedName("viewer_count")
        val viewerCount : Int? ,
    ) {
		@Keep
		data class Category(
            @SerializedName("color")
            val color : String? ,
            @SerializedName("extra_fields")
            val extraFields : Any? ,
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("image")
            val image : String? ,
            @SerializedName("name")
            val name : String? ,
            @SerializedName("thumbnail")
            val thumbnail : Any? ,
        )

		@Keep
		data class Product(
            @SerializedName("accept_offers")
            val acceptOffers : Boolean? ,
            @SerializedName("category_id")
            val categoryId : Int? ,
            @SerializedName("created_at")
            val createdAt : String? ,
            @SerializedName("description")
            val description : String? ,
            @SerializedName("flash_sale")
            val flashSale : Boolean? ,
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("images")
            val images : List<String?>? ,
            @SerializedName("pricing")
            val pricing : Int? ,
            @SerializedName("product_show")
            val productShow : String? ,
            @SerializedName("purchased_quantity")
            val purchasedQuantity : Int? ,
            @SerializedName("quantity")
            val quantity : Int? ,
            @SerializedName("reserve_for_live")
            val reserveForLive : Boolean? ,
            @SerializedName("shipping_profile_id")
            val shippingProfileId : Int? ,
            @SerializedName("status")
            val status : String? ,
            @SerializedName("sub_category_id")
            val subCategoryId : Any? ,
            @SerializedName("thumbnail")
            val thumbnail : List<String?>? ,
            @SerializedName("title")
            val title : String? ,
            @SerializedName("user_id")
            val userId : Int? ,
        )

		@Keep
		data class User(
            @SerializedName("authorize_net_cid")
            val authorizeNetCid : String? ,
            @SerializedName("bio")
            val bio : String? ,
            @SerializedName("default_card_id")
            val defaultCardId : String? ,
            @SerializedName("email")
            val email : String? ,
            @SerializedName("first_name")
            val firstName : String? ,
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("is_active")
            val isActive : Boolean? ,
            @SerializedName("is_followed")
            val isFollowed : Any? ,
            @SerializedName("last_name")
            val lastName : String? ,
            @SerializedName("live_sell_vendor_status")
            val liveSellVendorStatus : String? ,
            @SerializedName("marketplace_vendor_status")
            val marketplaceVendorStatus : String? ,
            @SerializedName("name")
            val name : String? ,
            @SerializedName("profile_image")
            val profileImage : String? ,
            @SerializedName("rating")
            val rating : Any? ,
            @SerializedName("referral_code")
            val referralCode : String? ,
            @SerializedName("role_id")
            val roleId : Int? ,
            @SerializedName("thumbnail")
            val thumbnail : Any? ,
            @SerializedName("username")
            val username : String? ,
        )
	}
}