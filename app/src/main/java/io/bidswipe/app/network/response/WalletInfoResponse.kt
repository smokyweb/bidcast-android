package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class WalletInfoResponse(
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
        @SerializedName("avaiable_balance")
        val avaiableBalance: Double?,
        @SerializedName("avaiable_for_payout")
        val avaiableForPayout: Double?,
        @SerializedName("processing")
        val processing: Double?
    )
}