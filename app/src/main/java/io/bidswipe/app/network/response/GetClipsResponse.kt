package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetClipsResponse(
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("perPage")
    val perPage: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?
) {
    data class Data(
        @SerializedName("clip_url")
        val clipUrl: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("is_public")
        val isPublic: String?,
        @SerializedName("show_id")
        val showId: String?,
        @SerializedName("thumbnail_url")
        val thumbnailUrl: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}