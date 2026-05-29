package io.bidswipe.app.network.response


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// M1 (2026-05-28): response for GET api/get-tip-setting?schedule_show_id=<id>.
// Backend (ApiController@getTipSetting) returns the saved per-show tip message
// and show-in-live-chat toggle so the seller's Tip Settings sheet can prefill
// on open. show_in_live_chat defaults to true server-side when no row exists.
@Keep
data class GetTipSettingResponse(
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
        @SerializedName("schedule_show_id")
        val scheduleShowId: Int?,
        @SerializedName("tip_message")
        val tipMessage: String?,
        @SerializedName("show_in_live_chat")
        val showInLiveChat: Boolean?
    )
}
