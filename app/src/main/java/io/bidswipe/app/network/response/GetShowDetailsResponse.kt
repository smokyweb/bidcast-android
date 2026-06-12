package io.bidswipe.app.network.response


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class GetShowDetailsResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("auction_type_id")
        val auctionTypeId: Int?,
        @SerializedName("auction")
        val auction: Auction?,
        @SerializedName("category")
        val category: Category?,
        @SerializedName("category_id")
        val categoryId: Int?,
        @SerializedName("sub_category_id")
        val subCategoryId: Int?,
        @SerializedName("sub_category")
        val subCategory: SubCategory?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("img_thumbnail")
        val imgThumbnail: List<String?>?,
        @SerializedName("is_explicit")
        val isExplicit: Boolean?,
        // Basecamp #9933883175 (2026-05-27): seller-controlled verified-buyers-only gate.
        @SerializedName("is_verified_only")
        val isVerifiedOnly: Boolean?,
        @SerializedName("is_live")
        val isLive: Boolean?,
        @SerializedName("is_promote")
        val isPromote: String?,
        @SerializedName("is_repeat")
        val isRepeat: Boolean?,
        @SerializedName("language")
        val language: String?,
        @SerializedName("latest_viewer_count")
        val latestViewerCount: Int?,
        @SerializedName("product_ids")
        val productIds: List<String?>?,
        // Basecamp #9991372302: per-product stream quantities. The server stores
        // this column as a JSON-ENCODED STRING (e.g. "{\"1016\":2}"), verified
        // against the live API — so it must be received as a String and parsed.
        // May be null/absent for shows created before this feature.
        @SerializedName("product_stream_quantities")
        val productStreamQuantities: String?,
        @SerializedName("products")
        val products: List<Product?>?,
        @SerializedName("promote_show_id")
        val promoteShowId: Any?,
        @SerializedName("promoted_at")
        val promotedAt: Any?,
        @SerializedName("recording_resource_id")
        val recordingResourceId: Any?,
        @SerializedName("recording_sid")
        val recordingSid: Any?,
        @SerializedName("repeat_value")
        val repeatValue: String?,
        @SerializedName("rtc_token")
        val rtcToken: String?,
        @SerializedName("share_count")
        val shareCount: Int?,
        @SerializedName("show_discoverability")
        val showDiscoverability: String?,
        @SerializedName("started_at")
        val startedAt: Any?,
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
        @SerializedName("total_orders")
        val totalOrders: Int?,
        @SerializedName("total_sales_amount")
        val totalSalesAmount: Double?,
    ) {
        @Keep
        data class Auction(
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("name")
            val name : String? ,
        )
        data class Category(
            @SerializedName("color")
            val color: String?,
            @SerializedName("deleted_at")
            val deletedAt: Any?,
            @SerializedName("extra_fields")
            val extraFields: List<Any?>?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("image")
            val image: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("thumbnail")
            val thumbnail: String?
        )

        data class SubCategory(
            @SerializedName("category_id")
            val categoryId: Int?,
            @SerializedName("color")
            val color: String?,
            @SerializedName("deleted_at")
            val deletedAt: Any?,
            @SerializedName("extra_fields")
            val extraFields: List<Any?>?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("image")
            val image: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("thumbnail")
            val thumbnail: String?
        )


        data class User(
            @SerializedName("authorize_net_cid")
            val authorizeNetCid: String?,
            @SerializedName("bio")
            val bio: Any?,
            @SerializedName("default_card_id")
            val defaultCardId: String?,
            @SerializedName("email")
            val email: String?,
            @SerializedName("first_name")
            val firstName: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("is_active")
            val isActive: Boolean?,
            @SerializedName("is_FirsttimeLogin")
            val isFirsttimeLogin: Boolean?,
            @SerializedName("jwt_token")
            val jwtToken: String?,
            @SerializedName("last_name")
            val lastName: String?,
            @SerializedName("live_sell_vendor_status")
            val liveSellVendorStatus: String?,
            @SerializedName("marketplace_vendor_status")
            val marketplaceVendorStatus: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?,
            @SerializedName("profile_visits")
            val profileVisits: Int?,
            @SerializedName("referral_code")
            val referralCode: String?,
            @SerializedName("role_id")
            val roleId: Int?,
            @SerializedName("thumbnail")
            val thumbnail: String?,
            @SerializedName("username")
            val username: String?,
            @SerializedName("vacation_mode")
            val vacationMode: String?,
            @SerializedName("rating")
            val rating: String?,
        )
    }
}