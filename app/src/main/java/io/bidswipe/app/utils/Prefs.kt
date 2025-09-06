package io.bidswipe.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.bidswipe.app.model.RememberModel
import io.bidswipe.app.network.response.LoginResponse
import androidx.core.content.edit

class Prefs(ctx : Context) {

	companion object {
		const val SHARED_PREF = "bid_swipe_App"
		const val REMEMBER_PREFS = "bid_swipe_remember"
		const val USER_EMAIL = "user_email"
		const val PUSH_TOKEN = "pushToken"
		const val TOKEN = "token"
		const val FIRST_LOGIN = "firstLogin"
		const val USER = "user"
		const val REM_NODE = "rem_node"
		const val LANGUAGE = "Locale.Helper.Selected.Language"
		const val LOCALE_LANGUAGE = "en"

	}

	private val mPrefs = ctx.getSharedPreferences(SHARED_PREF , Context.MODE_PRIVATE)
	private val rememberPrefs = ctx.getSharedPreferences(REMEMBER_PREFS , Context.MODE_PRIVATE)


	fun clear() {
		mPrefs.edit { clear() }
	}

	fun putString(key : String , value : String) {
		mPrefs.edit { putString(key , value) }
	}

	fun getString(key : String) = mPrefs.getString(key , "").toString()

	fun token() = mPrefs.getString(TOKEN , "").toString()

	fun fcmToken() = mPrefs.getString(PUSH_TOKEN , "").toString()

	fun userEmail() = mPrefs.getString(USER_EMAIL , "").toString()

	fun getUserData() = try {
		Gson().fromJson(mPrefs.getString(USER , "").toString() , LoginResponse.Data::class.java)
	} catch (e : Exception) {
		null
	}

	fun getUsers() : MutableList<RememberModel> {
		val mList = mutableListOf<RememberModel>()
		val data = rememberPrefs.getString(REM_NODE , "").toString()

		if (data.isNotEmpty()) {
			mList.addAll(Gson().fromJson(data , object : TypeToken<List<RememberModel>>() {}.type))
		}
		return mList
	}

	fun saveUsers(list : MutableList<RememberModel>) {
		rememberPrefs.edit { putString(REM_NODE , Gson().toJson(list)) }
	}

	fun localeLanguage() = mPrefs.getString(LOCALE_LANGUAGE , "").toString()

	fun isFirstLogin() = rememberPrefs.getBoolean(FIRST_LOGIN , true)

	fun saveFirstLogin(value : Boolean) = rememberPrefs.edit { putBoolean(FIRST_LOGIN , value) }


}