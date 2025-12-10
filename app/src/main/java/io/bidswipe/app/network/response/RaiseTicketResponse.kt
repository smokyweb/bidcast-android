package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class RaiseTicketResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("message")
        val message: String?,
        @SerializedName("product_id")
        val productId: String?,
        @SerializedName("subject")
        val subject: String?,
        @SerializedName("updated_at")
        val updatedAt: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}