package io.bidswipe.app.base

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.bidswipe.app.network.Resource
import io.bidswipe.app.utils.Alerts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

abstract class BaseRepository {
	val tag = this.javaClass.simpleName.uppercase()
	suspend fun <T> call(call : suspend () -> T) = withContext(Dispatchers.IO) {
		try {
			val mData = call.invoke()

			if (mData is BaseResponse) {
				Alerts.log(tag , "API SUCCESS")
			}

			return@withContext Resource.Success(mData)
		} catch (e : Throwable) {
			e.printStackTrace()

			when (e) {
				is UnknownHostException -> {
					Alerts.log(tag , "ERROR: No internet connection - UnknownHostException")
					return@withContext Resource.Error(
						true ,
						"NO_INTERNET" ,
						BaseResponse(message = "No internet connection. Please check your network.")
					)
				}

				is SocketTimeoutException -> {
					Alerts.log(tag , "ERROR: Request timeout - SocketTimeoutException")
					return@withContext Resource.Error(
						true ,
						"TIMEOUT" ,
						BaseResponse(message = "Connection timeout. Please try again.")
					)
				}

				is SSLException -> {
					Alerts.log(tag , "ERROR: SSL/Certificate error - ${e.message}")
					return@withContext Resource.Error(
						false ,
						"SSL_ERROR" ,
						BaseResponse(message = "Security error. Please update the app.")
					)
				}

				is IOException -> {
					Alerts.log(tag , "ERROR: Network I/O error - ${e.message}")
					return@withContext Resource.Error(
						true ,
						"NETWORK_ERROR" ,
						BaseResponse(message = "Network error. Please check your connection.")
					)
				}

				is HttpException -> {
					val code = e.code()
					Alerts.log(tag , "ERROR: HTTP $code - ${e.message()}")

					var errorResponse : BaseResponse?
					try {
						val errorBody = e.response()?.errorBody()?.string()
						if (! errorBody.isNullOrEmpty()) {
							try {
								val jsonObject = JSONObject(errorBody)
								errorResponse = Gson().fromJson(jsonObject.toString() , BaseResponse::class.java)
							} catch (jsonError : JSONException) {
								Alerts.log(tag , "ERROR: Failed to parse error JSON - ${jsonError.message}")
								errorResponse = BaseResponse(message = getHttpErrorMessage(code))
							}
						} else {
							errorResponse = BaseResponse(message = getHttpErrorMessage(code))
						}
					} catch (parseError : Exception) {
						Alerts.log(tag , "ERROR: Failed to read error body - ${parseError.message}")
						errorResponse = BaseResponse(message = getHttpErrorMessage(code))
					}

					return@withContext Resource.Error(false , "" + code , errorResponse)
				}

				is JsonSyntaxException -> {
					Alerts.log(tag , "ERROR: Invalid JSON response - ${e.message}")
					return@withContext Resource.Error(
						false ,
						"JSON_ERROR" ,
						BaseResponse(
							message = "Invalid response from server. Please try again or contact support." ,
							errorType = "JSON_PARSE_ERROR"
						)
					)
				}

				else -> {
					Alerts.log(tag , "ERROR: Unexpected error - ${e.javaClass.simpleName}: ${e.message}")
					return@withContext Resource.Error(
						false ,
						"UNKNOWN_ERROR" ,
						BaseResponse(message = "Unexpected error: ${e.localizedMessage ?: "Unknown error occurred"}")
					)
				}
			}
		}
	}

	private fun getHttpErrorMessage(code : Int) : String {
		return when (code) {
			400 -> "Invalid request. Please check your input."
			401 -> "Session expired. Please login again."
			403 -> "Access denied."
			404 -> "Resource not found."
			413 -> "File too large. Please choose a smaller file."
			422 -> "Invalid data provided."
			429 -> "Too many requests. Please wait a moment."
			500 -> "Server error. Please try again later."
			502 , 503 -> "Service temporarily unavailable."
			504 -> "Gateway timeout. Please try again."
			else -> "Error occurred (Code: $code)"
		}
	}
}