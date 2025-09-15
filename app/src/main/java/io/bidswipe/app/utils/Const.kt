package io.bidswipe.app.utils

import android.Manifest
import android.os.Build
import com.zeugmasolutions.localehelper.Locales
import io.bidswipe.app.model.LangModel
import io.bidswipe.app.model.LiveMoreOption
import java.util.Locale

object Const {

	const val BASE_URL = "https://backend.bidcast.betaplanets.com"


	const val STRIPE_KEY =
		"pk_test_51RQLxjQEbmPLLc7GaDeFTplB9lwTK5t9ZvpHVd1CtK4XtWsmktQvN3hoZW0ZZ0kSu0PFJ6R63D9X3PSMAq8tg5Sh00Vzh05MeU"

	//NOTIFICATION CONST
	const val CHANNEL_NAME = "Base Project"
	const val CHANNEL_ID = "base_project"

	const val APP_ID = 1005763407
	const val APP_SIGN = "73678be720c3ea2d871376882d27d21d5c2bc891363547424458f9febc8bf423"

	const val SERVER_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
	const val DD_MM_YYYY_HH_MM_SS = "dd-MM-yyyy HH:mm:ss"
	const val DD_MMMM_YYYY = "dd MMMM yyyy"

	//APP PERMISSIONS
	private val COMMON_PERMS = arrayOf(
		Manifest.permission.INTERNET ,
		Manifest.permission.CAMERA ,
		Manifest.permission.ACCESS_WIFI_STATE ,
		Manifest.permission.ACCESS_NETWORK_STATE ,
		Manifest.permission.RECORD_AUDIO ,
		Manifest.permission.BLUETOOTH ,
		Manifest.permission.MODIFY_AUDIO_SETTINGS ,
		Manifest.permission.READ_PHONE_STATE ,

		)

	val VERSION_PERMS = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
			Manifest.permission.POST_NOTIFICATIONS ,
		)

		else -> arrayOf()
	}

	val STR_PERMS = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
			Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED ,
			Manifest.permission.READ_MEDIA_IMAGES
		)

		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
			Manifest.permission.READ_MEDIA_IMAGES
		)

		Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> arrayOf(
			Manifest.permission.READ_EXTERNAL_STORAGE ,
			Manifest.permission.WRITE_EXTERNAL_STORAGE

		)

		else -> arrayOf(
			Manifest.permission.READ_EXTERNAL_STORAGE ,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		)
	}

	val LOC_PERMS = arrayOf(
		Manifest.permission.ACCESS_FINE_LOCATION ,
		Manifest.permission.ACCESS_COARSE_LOCATION ,
	)

	val CAMERA_PERMS = arrayOf(
		Manifest.permission.CAMERA ,
	)

	val PERMISSIONS = COMMON_PERMS + VERSION_PERMS + STR_PERMS

	val languages = listOf(
		LangModel("English" , Locales.English) ,
		LangModel("Chinese" , Locale("zh" , "")) ,
		LangModel("Spanish" , Locales.Spanish) ,
		LangModel("Hindi" , Locales.Hindi) ,
		LangModel("Portuguese" , Locales.Portuguese) ,
		LangModel("Arabic" , Locales.Arabic) ,
		LangModel("Russian" , Locales.Russian) ,
		LangModel("Japanese" , Locales.Japanese) ,
		LangModel("Vietnamese" , Locales.Vietnamese) ,
		LangModel("Korean" , Locales.Korean)
	).sortedBy { it.title }


	val liveMoreMenu = mutableListOf(
		LiveMoreOption("End Show" , true , draw.ic_end) ,
//		LiveMoreOption("Clone item" , false , draw.ic_copy) ,
		LiveMoreOption("Tip Setting" , false , draw.ic_dollar) ,
//		LiveMoreOption("Multicast" , false , draw.ic_multicast) ,
//		LiveMoreOption("Add Coupons" , false , draw.ic_coupon) ,
		LiveMoreOption("Raid" , false , draw.ic_people) ,
//		LiveMoreOption("Create Poll" , false , draw.ic_poll)
	)

}
