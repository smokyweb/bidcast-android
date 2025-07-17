package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class AboutUsResponse(
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
        @SerializedName("company_name")
        val companyName: String?,
        @SerializedName("contact_email")
        val contactEmail: String?,
        @SerializedName("contact_phone")
        val contactPhone: String?,
        @SerializedName("features")
        val features: List<Feature?>?,
        @SerializedName("impact")
        val impact: List<Impact?>?,
        @SerializedName("logo")
        val logo: String?,
        @SerializedName("mission")
        val mission: String?,
        @SerializedName("platform_name")
        val platformName: String?,
        @SerializedName("social_media")
        val socialMedia: List<SocialMedia?>?,
        @SerializedName("team")
        val team: List<Team?>?
    ) {
        @Keep
        data class Feature(
            @SerializedName("description")
            val description: String?,
            @SerializedName("icon")
            val icon: String?,
            @SerializedName("title")
            val title: String?
        )

        @Keep
        data class Impact(
            @SerializedName("label")
            val label: String?,
            @SerializedName("value")
            val value: String?
        )

        @Keep
        data class SocialMedia(
            @SerializedName("platform")
            val platform: Int?,
            @SerializedName("url")
            val url: Url?
        ) {
            @Keep
            data class Url(
                @SerializedName("platform")
                val platform: String?,
                @SerializedName("url")
                val url: String?
            )
        }

        @Keep
        data class Team(
            @SerializedName("image")
            val image: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("role")
            val role: String?
        )
    }
}