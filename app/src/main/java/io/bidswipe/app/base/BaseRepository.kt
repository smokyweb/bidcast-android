package io.bidswipe.app.base

import BaseResponse
import com.google.gson.Gson
import io.bidswipe.app.network.Resource
import io.bidswipe.app.utils.runSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

abstract class BaseRepository {
    val tag = this.javaClass.simpleName.uppercase()
    suspend fun <T> call(call: suspend () -> T) = withContext(Dispatchers.IO) {
        try {
            val mData = call.invoke()

            if (mData is BaseResponse) {
//				Alerts.log(tag , "API SUCCESS => \n${mData}")
            }

            return@withContext Resource.Success(mData)
        } catch (e: Throwable) {
            e.printStackTrace()
            when (e) {
                is HttpException -> {
                    var errorResponse: BaseResponse? = null
                    runSafe {
                        val error = e.response()?.errorBody()!!.charStream().readText()
//						Alerts.log(tag, "API ERROR => \n$error")
                        errorResponse =
                            Gson().fromJson(JSONObject(error).toString(), BaseResponse::class.java)
                    }
                    return@withContext Resource.Error(false, "" + e.code(), errorResponse)
                }

                else -> {
//					Alerts.log(tag, "API ERROR => NO INTERNET \n${e.message}")
                    return@withContext Resource.Error(true, "" + e.message, null)
                }
            }
        }
    }

}