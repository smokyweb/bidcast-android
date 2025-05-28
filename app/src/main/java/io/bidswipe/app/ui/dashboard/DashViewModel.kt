package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
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
	
	private var _getLessonResponse = MutableLiveData<Resource<GetLessonsResponse>>()
	val getLessonRepo: MutableLiveData<Resource<GetLessonsResponse>>
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

	private var _getHowToSellStepResponse = MutableLiveData<Resource<GetHowToSellResponse>>()
	val getHowToSellStepRepo: MutableLiveData<Resource<GetHowToSellResponse>>
		get() = _getHowToSellStepResponse

	fun getHowToSellStep() = viewModelScope.launch {
		_getHowToSellStepResponse.value = repo.getHowToSellStep()
	}

	private var _getPrepareStepResponse = MutableLiveData<Resource<GetPrepareStepResponse>>()
	val getPrepareStepRepo: MutableLiveData<Resource<GetPrepareStepResponse>>
		get() = _getPrepareStepResponse

	fun getPrepareStep() = viewModelScope.launch {
		_getPrepareStepResponse.value = repo.getPrepareStep()
	}


	private var _getMyScheduledShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getMyScheduledShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getMyScheduledShowResponse

	fun getMyScheduledShow(
		type : RequestBody? = null
	) = viewModelScope.launch {
		_getMyScheduledShowResponse.value = repo.getMyScheduledShow(type)
	}

	private var _getLiveShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getLiveShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getLiveShowResponse

	fun getLiveShow(
	) = viewModelScope.launch {
		_getLiveShowResponse.value = repo.getLiveShow()
	}

	
}