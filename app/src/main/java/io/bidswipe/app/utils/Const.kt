package io.bidswipe.app.utils

import android.Manifest
import android.os.Build
import com.zeugmasolutions.localehelper.Locales
import io.bidswipe.app.base.BaseResponse
import io.bidswipe.app.model.LangModel
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.network.Resource
import java.util.Locale

object Const {

    const val BASE_URL = "https://backend.bidcast.betaplanets.com"
    val SOCKET_URL = "https://node.bidcast.betaplanets.com/"

    const val STRIPE_KEY_TEST = "sk_test_51SjiEtQzmy9jx34KJkQK0ng6qgLis0Hs6WlFOreR7PFGZDgAmywdukVujQEZP9ypqkpxANtz06vAvB6cGRyx8rCf00Bj2cGaVU"

    const val STRIPE_KEY = "pk_test_51SjiEtQzmy9jx34KXrnMJIwqLx5IfCN69oZsNCptlyBfChq7NrJVc8OjS5q16nvnuobjjp3Run8icoXQHn0D9eVG00nJyhk9zM"

    //NOTIFICATION CONST
    const val CHANNEL_NAME = "Base Project"
    const val CHANNEL_ID = "base_project"

    const val APP_ID = 1005763407
    const val APP_SIGN = "73678be720c3ea2d871376882d27d21d5c2bc891363547424458f9febc8bf423"

    //DOLBY iO CONST
    const val ACCOUNT_ID = "227tmE"
    const val PUBLISHING_TOKEN = "3355b11d1201319117ffc14cef3f55ffe9c1054651735e8a03b0d09608a4127e"
    const val TOKEN_ID = "14730126"

    //AGORA CONST
    const val APP_ID_AGORA = "6a0ab77ee15943df94524201d6c93877"

    const val SERVER_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DD_MM_YYYY_HH_MM_SS = "dd-MM-yyyy HH:mm:ss"
    const val YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
    const val DD_MMMM_YYYY = "dd MMMM yyyy"
    const val MMM_dd_yyyy_HH_mm = "MMM dd, yyyy • HH:mm"

    const val BULLET = "•"

    val NO_INTERNET_ERROR = Resource.Error(
        isNetworkError = true,
        errorCode = "NO_INTERNET",
        errorResponse = BaseResponse(message = "No internet connection. Please check your network.")
    )

    //APP PERMISSIONS
    private val COMMON_PERMS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.READ_PHONE_STATE,
    )

    val VERSION_PERMS = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
        )

        else -> arrayOf()
    }

    val STR_PERMS = when {

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> arrayOf(
//			Manifest.permission.READ_EXTERNAL_STORAGE ,
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val LOC_PERMS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val CAMERA_PERMS = arrayOf(
        Manifest.permission.CAMERA
    )

    val PERMISSIONS = COMMON_PERMS + VERSION_PERMS + STR_PERMS

    val languages = listOf(
        LangModel("English", Locales.English),
        LangModel("Chinese", Locale("zh", "")),
        LangModel("Spanish", Locales.Spanish),
        LangModel("Hindi", Locales.Hindi),
        LangModel("Portuguese", Locales.Portuguese),
        LangModel("Arabic", Locales.Arabic),
        LangModel("Russian", Locales.Russian),
        LangModel("Japanese", Locales.Japanese),
        LangModel("Vietnamese", Locales.Vietnamese),
        LangModel("Korean", Locales.Korean)
    ).sortedBy { it.title }


    val liveMoreMenu = mutableListOf(
        LiveMoreOption("End Show", true, draw.ic_end),
//		LiveMoreOption("Clone item" , false , draw.ic_copy) ,
        LiveMoreOption("Tip Setting", false, draw.ic_dollar),
//		LiveMoreOption("Multicast" , false , draw.ic_multicast) ,
//		LiveMoreOption("Add Coupons" , false , draw.ic_coupon) ,
        LiveMoreOption("Raid", false, draw.ic_people),
        LiveMoreOption("Create Poll", false, draw.ic_poll),
        LiveMoreOption("Randomizer", false, draw.ic_spin_wheel),
        // Basecamp #9934001770 (2026-05-27): co-host pairing.
        LiveMoreOption("Pair Device", false, draw.ic_people),
    )

    val dimensionScales = listOf("Inch", "Feet", "Centimeter", "Meter")
    val weightScales= listOf("Pound" , "Ounce" , "Gram", "Kilogram" )

}
