package io.bidswipe.app.model


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class PaymentCardModel(
    @SerializedName("card_number")
    val cardNumber: String?,
    @SerializedName("cvv")
    val cvv: String?,
    @SerializedName("expiration_date")
    val expirationDate: String?
)