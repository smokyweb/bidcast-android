package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class CheckScheduleShowResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("isExists")
        val isExists: Boolean?
    )
}