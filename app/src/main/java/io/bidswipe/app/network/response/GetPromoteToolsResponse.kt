package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPromoteToolsResponse(
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
        @SerializedName("features")
        val features: List<Feature?>?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("promote_details")
        val promoteDetails: String?,
        @SerializedName("promote_title")
        val promoteTitle: String?,
        @SerializedName("show_details")
        val showDetails: String?,
        @SerializedName("show_icon")
        val showIcon: String?,
        @SerializedName("show_options")
        val showOptions: ShowOptions?,
        @SerializedName("show_title")
        val showTitle: String?
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
        data class ShowOptions(
            @SerializedName("Followers")
            val followers: Int?,
            @SerializedName("Shows")
            val shows: Int?,
            @SerializedName("Views")
            val views: Int?
        )
    }
}