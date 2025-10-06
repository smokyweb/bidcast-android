package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetTipAmountResponse(
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
        @SerializedName("summary")
        val summary: Summary?,
        @SerializedName("tips")
        val tips: List<Tip?>?
    ) {
        @Keep
        data class Summary(
            @SerializedName("today_tips")
            val todayTips: Int?,
            @SerializedName("total_tips")
            val totalTips: String?
        )

        @Keep
        data class Tip(
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("total")
            val total: String?,
            @SerializedName("user")
            val user: User?,
            @SerializedName("user_id")
            val userId: Int?
        ) {
            @Keep
            data class User(
                @SerializedName("email")
                val email: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("name")
                val name: String?,
                @SerializedName("profile_image")
                val profileImage: String?
            )
        }
    }
}