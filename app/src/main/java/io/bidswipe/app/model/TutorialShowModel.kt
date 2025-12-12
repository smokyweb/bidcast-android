package io.bidswipe.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TutorialShowModel(
	var showTitle : String? = null ,
	var categoryId : String? = null ,
	var actionId : String? = null ,
	var thumbnail : String? = null ,
	var productIds : String? = null ,
	var repeatMode : String = "",
	var repeatType : String = "",
	var explicitContent : String = "",
	var primaryLanguage : String = "",
	var discoverability : String = ""
) : Parcelable
