package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetKYCDetailsRespnse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("account_id")
        val accountId: String?,
        @SerializedName("city")
        val city: Any?,
        @SerializedName("country")
        val country: Any?,
        @SerializedName("phone")
        val phone: Any?,
        @SerializedName("postal_code")
        val postalCode: Any?
    )
}