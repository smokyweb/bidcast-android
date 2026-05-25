package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SettingListResponse(
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
		@SerializedName("activity_status")
		val activityStatus: Boolean?,
		@SerializedName("country_of_residence")
		val countryOfResidence: String?,
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
		val instruction: String?,
		@SerializedName("receive_gifts")
		val receiveGifts: Boolean?,
		@SerializedName("save_past_shows")
		val savePastShows: Boolean?,
		@SerializedName("shipping_address")
		val shippingAddress: ShippingAddress?,
		@SerializedName("shipping_address_id")
		val shippingAddressId: Int?,
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
	) {
		data class ShippingAddress(
			@SerializedName("address_line_2")
			val addressLine2: String?,
			@SerializedName("city")
			val city: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("is_default")
			val isDefault: Boolean?,
			@SerializedName("name")
			val name: String?,
			@SerializedName("phone_number")
			val phoneNumber: String?,
			@SerializedName("pincode")
			val pincode: String?,
			@SerializedName("state")
			val state: String?,
			@SerializedName("street_address")
			val streetAddress: String?,
			@SerializedName("type")
			val type: String?,
			@SerializedName("user_id")
			val userId: Int?
		)
	}
}