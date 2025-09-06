package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPaymentCardsResponse(
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
        @SerializedName("customerProfileId")
        val customerProfileId : String? ,
        @SerializedName("description")
        val description : String? ,
        @SerializedName("email")
        val email : String? ,
        @SerializedName("merchantCustomerId")
        val merchantCustomerId : String? ,
        @SerializedName("paymentProfiles")
        val paymentProfiles : List<PaymentProfile?>? ,
        @SerializedName("profileType")
        val profileType : String? ,
    ) {
		@Keep
		data class PaymentProfile(
			@SerializedName("customerPaymentProfileId")
			val customerPaymentProfileId : String? ,
			@SerializedName("customerType")
			val customerType : String? ,
			@SerializedName("payment")
			val payment : Payment? ,
			@SerializedName("is_default")
			val isDefault : Boolean? ,
			var selected : Boolean? = false ,
		) {
			@Keep
			data class Payment(
                @SerializedName("creditCard")
                val creditCard : CreditCard? ,
            ) {
				@Keep
				data class CreditCard(
                    @SerializedName("cardNumber")
                    val cardNumber : String? ,
                    @SerializedName("cardType")
                    val cardType : String? ,
                    @SerializedName("expirationDate")
                    val expirationDate : String? ,
                )
			}
		}
	}
}