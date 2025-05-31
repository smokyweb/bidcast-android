package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetShippingAddressResponse(
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
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
        @SerializedName("street_address")
        val streetAddress: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}