package io.bidswipe.app.network

import BaseResponse

sealed class Resource<out T> {

	data class Success<out T>(val value : T) : Resource<T>()

	data class Error(
		val isNetworkError : Boolean ,
		val errorCode : String? ,
		val errorResponse : BaseResponse? ,
	) : Resource<Nothing>()
}