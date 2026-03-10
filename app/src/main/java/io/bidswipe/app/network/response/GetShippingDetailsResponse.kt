package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetShippingDetailsResponse(
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
        @SerializedName("domestic_shipment_setting")
        val domesticShipmentSetting: DomesticShipmentSetting?,
        @SerializedName("free_pickup")
        val freePickup: Boolean?,
        @SerializedName("shipping_address")
        val shippingAddress: SettingListResponse.Data.ShippingAddress?,
        @SerializedName("instruction")
        val instruction: String?,
        @SerializedName("shipping_profiles_count")
        val shippingProfilesCount: Int?
    ) {
        data class DomesticShipmentSetting(
            @SerializedName("also_apply_schedule_show")
            val alsoApplyScheduleShow: Boolean?,
            @SerializedName("domestic_shipment_form_1_to_5_lbs")
            val domesticShipmentForm1To5Lbs: Any?,
            @SerializedName("domestic_shipment_over_5_lbs")
            val domesticShipmentOver5Lbs: Any?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("shipping_costs")
            val shippingCosts: String?,
            @SerializedName("shipping_cost_also_apply_schedule_show")
            val shippingCostsAlsoApplyToScheduledShows: Boolean?,
            @SerializedName("user_id")
            val userId: Int?,
            @SerializedName("usps_first_class_mail_letter")
            val uspsFirstClassMailLetter: Boolean?
        )
    }
}