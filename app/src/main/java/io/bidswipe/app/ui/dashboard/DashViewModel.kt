package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CheckKycResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.network.response.GenerateTokenResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import io.bidswipe.app.network.response.UserDeviceResponse
import io.bidswipe.app.network.response.UserProfileResponse
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


	private var _getLiveShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getLiveShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getLiveShowResponse

	fun getLiveShow(
		type : RequestBody?,
		category: RequestBody? = null
	) = viewModelScope.launch {
		_getLiveShowResponse.value = repo.getLiveShow(type, category)
	}

	private var _offerListResponse = MutableLiveData<Resource<GetOffersResponse>>()
	val offerListRepo: MutableLiveData<Resource<GetOffersResponse>>
		get() = _offerListResponse

	fun offerList(
		page: Int
	) = viewModelScope.launch {
		_offerListResponse.value = repo.offerList(page)
	}

	private var _offerUpdateStatusResponse = MutableLiveData<Resource<UpdateOfferResponse>>()
	val offerUpdateStatusRepo: MutableLiveData<Resource<UpdateOfferResponse>>
		get() = _offerUpdateStatusResponse

	fun offerUpdateStatus(
		offerId : RequestBody?,
		status: RequestBody?
	) = viewModelScope.launch {
		_offerUpdateStatusResponse.value = repo.offerUpdateStatus(offerId,status)
	}

	private var _addPaymentCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val addPaymentCardRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _addPaymentCardResponse

	fun addPaymentCard(
		cardToken : RequestBody?
	) = viewModelScope.launch {
		_addPaymentCardResponse.value = repo.addPaymentCard(cardToken)
	}



	private var _generateTokenResponse = MutableLiveData<Resource<GenerateTokenResponse>>()
	val generateTokenRepo: MutableLiveData<Resource<GenerateTokenResponse>>
		get() = _generateTokenResponse

	fun generateToken(
		showId : RequestBody?
		) = viewModelScope.launch {
		_generateTokenResponse.value = repo.generateToken(showId)
	}

	private var _storeDeviceDetailsResponse = MutableLiveData<Resource<UserDeviceResponse>>()
	val storeDeviceDetailsRepo: MutableLiveData<Resource<UserDeviceResponse>>
		get() = _storeDeviceDetailsResponse

	fun storeDeviceDetails(
		deviceToken : RequestBody?
	) = viewModelScope.launch {
		_storeDeviceDetailsResponse.value = repo.storeDeviceDetails(deviceToken)
	}

	private var _updateLiveStatusResponse = MutableLiveData<Resource<UpdateLiveStatusResponse>>()
	val updateLiveStatusRepo: MutableLiveData<Resource<UpdateLiveStatusResponse>>
		get() = _updateLiveStatusResponse

	fun updateLiveStatus(
		showId: RequestBody?,
		isLive: RequestBody?
	) = viewModelScope.launch {
		_updateLiveStatusResponse.value = repo.updateLiveStatus(showId,isLive)
	}


	/*private var _updateLiveStatusResponse = MutableLiveData<Resource<UpdateLiveStatusResponse>>()
	val updateLiveStatusRepo: MutableLiveData<Resource<UpdateLiveStatusResponse>>
		get() = _updateLiveStatusResponse

	fun updateLiveStatus(
		showId: RequestBody?,
		isLive: RequestBody?
	) = viewModelScope.launch {
		_updateLiveStatusResponse.value = repo.(showId,isLive)
	}*/



	private var _getUserProfileResponse = MutableLiveData<Resource<UserProfileResponse>>()
	val getUserProfileRepo: MutableLiveData<Resource<UserProfileResponse>>
		get() = _getUserProfileResponse

	fun getUserProfile(
	) = viewModelScope.launch {
		_getUserProfileResponse.value = repo.getUserProfile()
	}

	private var _fetchBidsResponse = MutableLiveData<Resource<FetchBidResponse>>()
	val fetchBidsRepo: MutableLiveData<Resource<FetchBidResponse>>
		get() = _fetchBidsResponse

	fun fetchBids(
	) = viewModelScope.launch {
		_fetchBidsResponse.value = repo.fetchBids()
	}

	private var _getPurchasedProductsByStatusResponse = MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getPurchasedProductsByStatusRepo: MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getPurchasedProductsByStatusResponse

	fun getPurchasedProductsByStatus(
		type: RequestBody?,
		page : RequestBody?
	) = viewModelScope.launch {
		_getPurchasedProductsByStatusResponse.value = repo.getProductsByStatus(type,page)
	}

	private var _getSavedProductsByStatusResponse = MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getSavedProductsByStatusRepo: MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getSavedProductsByStatusResponse

	fun getSavedProductsByStatus(
		type: RequestBody?,
		page : RequestBody?
	) = viewModelScope.launch {
		_getSavedProductsByStatusResponse.value = repo.getProductsByStatus(type,page)
	}

	private var _checkKycResponse = MutableLiveData<Resource<CheckKycResponse>>()
	val checkKycRepo: MutableLiveData<Resource<CheckKycResponse>>
		get() = _checkKycResponse

	fun checkKyc(
	) = viewModelScope.launch {
		_checkKycResponse.value = repo.checkKyc()
	}


	private var _updateProfileResponse = MutableLiveData<Resource<CommonResponse>>()
	val updateProfileRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _updateProfileResponse

	fun updateProfile(
		firstName: RequestBody,
		lastName: RequestBody,
		image: MultipartBody.Part?,
		userName: RequestBody,
		bio: RequestBody
	) = viewModelScope.launch {
		_updateProfileResponse.value = repo.updateProfile(firstName, lastName,image, userName, bio)
	}

	private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
	val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
		get() = _getUserProductsResponse

	fun getUserProducts(
		userId : RequestBody? = null
	) = viewModelScope.launch {
		_getUserProductsResponse.value = repo.getUserProducts(userId)
	}

}