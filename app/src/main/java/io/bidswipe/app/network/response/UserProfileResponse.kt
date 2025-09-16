package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class UserProfileResponse(
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
        @SerializedName("bio")
        val bio : String? ,
        @SerializedName("email")
        val email : String? ,
        @SerializedName("default_card")
        val defaultCard : DefaultCard? ,
        @SerializedName("default_shipping_address")
        val defaultShippingAddress : DefaultShippingAddress? ,
        @SerializedName("first_name")
        val firstName : String? ,
        @SerializedName("id")
        val id : Int? ,
        @SerializedName("is_active")
        val isActive : Boolean? ,
        @SerializedName("is_FirstShowCreated")
        val isFirstShowCreated : Boolean? ,
        @SerializedName("last_name")
        val lastName : String? ,
        @SerializedName("name")
        val name : String? ,
        @SerializedName("profile_image")
        val profileImage : String? ,
        @SerializedName("referral_code")
        val referralCode : String? ,
        @SerializedName("role")
        val role : Role? ,
        @SerializedName("role_id")
        val roleId : Int? ,
        @SerializedName("thumbnail")
        val thumbnail : Any? ,
        @SerializedName("username")
        val username : String? ,
        @SerializedName("seller_identity_status")
        val sellerIdentityStatus : String? ,
        @SerializedName("buyer_identity_status")
        val buyerIdentityStatus : String? ,
        @SerializedName("has_shipping_address")
        val hasShippingAddress : Boolean? ,
        @SerializedName("has_card_added")
        val hasCardAdded : Boolean? ,
        @SerializedName("preference")
        val preferences: SettingListResponse.Data?
        ) {

		@Keep
		data class DefaultShippingAddress(
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

		@Keep
		data class DefaultCard(
            @SerializedName("card_id")
            val cardId : String? ,
            @SerializedName("exp_date")
            val expDate : String? ,
            @SerializedName("last4")
            val last4 : String? ,
        )

		@Keep
		data class Role(
			@SerializedName("created_at")
			val createdAt : Any? ,
			@SerializedName("id")
			val id : Int? ,
			@SerializedName("name")
			val name : String? ,
			@SerializedName("updated_at")
			val updatedAt : Any? ,
		)
	}
}