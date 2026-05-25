package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class UserProfileResponse(
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
        @SerializedName("bio")
        val bio: String?,
        @SerializedName("buyer_identity_status")
        val buyerIdentityStatus: Any?,
        @SerializedName("coupon_count")
        val couponCount: Int?,
        @SerializedName("default_card")
        val defaultCard: DefaultCard?,
        @SerializedName("default_card_id")
        val defaultCardId: String?,
        @SerializedName("default_shipping_address")
        val defaultShippingAddress: DefaultShippingAddress?,
        @SerializedName("email")
        val email: String?,
        @SerializedName("first_name")
        val firstName: String?,
        @SerializedName("has_card_added")
        val hasCardAdded: Boolean?,
        @SerializedName("has_shipping_address")
        val hasShippingAddress: Boolean?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_active")
        val isActive: Boolean?,
        @SerializedName("is_FirstShowCreated")
        val isFirstShowCreated: Boolean?,
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
        @SerializedName("preference")
        val preference: Preference?,
        @SerializedName("profile_image")
        val profileImage: String?,
        @SerializedName("profile_visits")
        val profileVisits: Int?,
        @SerializedName("referral_code")
        val referralCode: String?,
        @SerializedName("role")
        val role: Role?,
        @SerializedName("role_id")
        val roleId: Int?,
        @SerializedName("seller_identity_status")
        val sellerIdentityStatus: String?,
        @SerializedName("thumbnail")
        val thumbnail: String?,
        @SerializedName("username")
        val username: String?,
        @SerializedName("vacation_mode")
        val vacationMode: String?,
        @SerializedName("wallet_amount")
        val walletAmount: Int?
    ) {
        data class DefaultCard(
            @SerializedName("card_holder_name")
            val cardHolderName: String?,
            @SerializedName("card_id")
            val cardId: String?,
            @SerializedName("exp_month")
            val expMonth: Int?,
            @SerializedName("exp_year")
            val expYear: Int?,
            @SerializedName("last4")
            val last4: String?
        )

        // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitLab's
        // Preference class (new in Trey's Wave settings-profile track) +
        // GitHub's address_line_2 field on DefaultShippingAddress.
        data class Preference(
            @SerializedName("activity_status")
            val activityStatus: Boolean?,
            @SerializedName("country_of_residence")
            val countryOfResidence: Any?,
            @SerializedName("direct_message")
            val directMessage: Boolean?,
            @SerializedName("enable_clips")
            val enableClips: Boolean?,
            @SerializedName("enable_private_entry")
            val enablePrivateEntry: Boolean?,
            @SerializedName("free_shipping")
            val freeShipping: Boolean?,
            @SerializedName("haptic_feedback")
            val hapticFeedback: Boolean?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("instruction")
            val instruction: Any?,
            @SerializedName("receive_gifts")
            val receiveGifts: Boolean?,
            @SerializedName("save_past_shows")
            val savePastShows: Boolean?,
            @SerializedName("shipping_address_id")
            val shippingAddressId: Any?,
            @SerializedName("show_reward_status")
            val showRewardStatus: Boolean?,
            @SerializedName("show_seller_tools")
            val showSellerTools: Boolean?,
            @SerializedName("suggest_my_account")
            val suggestMyAccount: Boolean?,
            @SerializedName("sync_phone_contacts")
            val syncPhoneContacts: Boolean?,
            @SerializedName("user_id")
            val userId: Int?
        )
        @Keep
        data class DefaultShippingAddress(
            @SerializedName("address_line_2")
            val addressLine2 : String? ,
            @SerializedName("id")
            val id : Int? ,
            @SerializedName("is_default")
            val isDefault : Boolean? ,
            @SerializedName("name")
            val name : String? ,
            @SerializedName("phone_number")
            val phoneNumber : String? ,
            @SerializedName("pincode")
            val pincode : String? ,
            @SerializedName("street_address")
            val streetAddress : String? ,
            @SerializedName("type")
            val type : String? ,
            @SerializedName("user_id")
            val userId : Int? ,
        )

        data class Role(
            @SerializedName("created_at")
            val createdAt: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("updated_at")
            val updatedAt: Any?
        )
    }
}