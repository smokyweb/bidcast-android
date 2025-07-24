package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class FetchSellerVerificationResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?,
) {
    @Keep
    data class Data(
        @SerializedName("card_details")
        val cardDetails: CardDetails?,
        @SerializedName("card_id")
        val cardId: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("id_card")
        val idCard: String?,
        @SerializedName("image")
        val image: String?,
        @SerializedName("number_otp_verified")
        val numberOtpVerified: Int?,
        @SerializedName("otp")
        val otp: String?,
        @SerializedName("phone_number")
        val phoneNumber: String?,
        @SerializedName("reason")
        val reason: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("user_id")
        val userId: Int?,
    ) {
        @Keep
        data class CardDetails(
            @SerializedName("exp_month")
            val expMonth: Int?,
            @SerializedName("exp_year")
            val expYear: Int?,
            @SerializedName("last4")
            val last4: String?,
        )
    }
}