package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetMailClassesResponse(
    @SerializedName("status")
    val status : String? ,
    @SerializedName("message")
    val message : String? ,
    @SerializedName("error_type")
    val errorType : String? ,
    @SerializedName("data")
    val `data` : Data? ,
) {
	@Keep
	data class Data(
        @SerializedName("mail_classes")
        val mailClasses : List<MailClasses?>? ,
    ) {
		@Keep
		data class MailClasses(
            @SerializedName("label")
            val label : String? ,
            @SerializedName("max_weight_lbs")
            val maxWeightLbs : Double? ,
            @SerializedName("max_length_in")
            val maxLengthIn : Double? ,
            @SerializedName("max_width_in")
            val maxWidthIn : Double? ,
            @SerializedName("max_height_in")
            val maxHeightIn : Double? ,
            @SerializedName("max_length_plus_girth_in")
            val maxLengthPlusGirthIn : Double? ,
            @SerializedName("notes")
            val notes : String? ,
        )
	}
}