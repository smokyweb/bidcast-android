package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject


@HiltViewModel
class DashViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {
	var lastIndex = MutableLiveData(0)
	
	private var _logoutResponse = MutableLiveData<Resource<CommonResponse>>()
	val logoutRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _logoutResponse
	
	fun logout(
	) = viewModelScope.launch {
		_logoutResponse.value = repo.logout()
	}
	
	private var _getCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getCategoryRepo: MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getCategoryResponse
	
	fun getCategory(
	) = viewModelScope.launch {
		_getCategoryResponse.value = repo.getCategory()
	}
	
	private var _getLessonResponse = MutableLiveData<Resource<CommonResponse>>()
	val getLessonRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _getLessonResponse
	
	fun getLesson() = viewModelScope.launch {
		_getLessonResponse.value = repo.getLesson()
	}
	
	private var _getProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val getProductRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _getProductResponse
	
	fun getProduct(categoryId: RequestBody?) = viewModelScope.launch {
		_getProductResponse.value = repo.getProduct(categoryId)
	}
	
	private var _storeProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeProductRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _storeProductResponse
	
	fun storeProduct(
		categoryId: RequestBody?,
		title: RequestBody?,
		description: RequestBody?,
		quantity: RequestBody?,
		pricing: RequestBody?,
		flashSale: RequestBody?,
		acceptOffers: RequestBody?,
		reserveForLive: RequestBody?,
		shippingProfileId: RequestBody?,
		status: RequestBody?,
		productImages: List<MultipartBody.Part>?
	) = viewModelScope.launch {
		_storeProductResponse.value = repo.storeProduct(categoryId, title, description, quantity, pricing, flashSale, acceptOffers, reserveForLive, shippingProfileId, status, productImages)
	}
	
	
}