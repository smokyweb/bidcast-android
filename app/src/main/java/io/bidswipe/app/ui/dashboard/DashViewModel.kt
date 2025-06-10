package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.GenerateTokenResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
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


	private var _getLiveShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getLiveShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getLiveShowResponse

	fun getLiveShow(
	) = viewModelScope.launch {
		_getLiveShowResponse.value = repo.getLiveShow()
	}

	private var _getProductDetailsResponse = MutableLiveData<Resource<GetProductDetailsResponse>>()
	val getProductDetailsRepo: MutableLiveData<Resource<GetProductDetailsResponse>>
		get() = _getProductDetailsResponse

	fun getProductDetails(
		productId : RequestBody?
	) = viewModelScope.launch {
		_getProductDetailsResponse.value = repo.getProductDetails(productId)
	}

	private var _makeOfferResponse = MutableLiveData<Resource<CommonResponse>>()
	val makeOfferRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _makeOfferResponse

	fun makeOffer(
		amount : RequestBody?,
		productId : RequestBody?
	) = viewModelScope.launch {
		_makeOfferResponse.value = repo.makeOffer(amount,productId)
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

	private var _getShippingAddressResponse = MutableLiveData<Resource<GetShippingAddressResponse>>()
	val getShippingAddressRepo: MutableLiveData<Resource<GetShippingAddressResponse>>
		get() = _getShippingAddressResponse

	fun getShippingAddress() = viewModelScope.launch {
		_getShippingAddressResponse.value = repo.getShippingAddress()
	}

	private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
	val getPaymentCardRepo: MutableLiveData<Resource<GetPaymentCardsResponse>>
		get() = _getPaymentCardResponse

	fun getPaymentCard(
	) = viewModelScope.launch {
		_getPaymentCardResponse.value = repo.getPaymentCard()
	}


	private var _getPurchaseProductResponse = MutableLiveData<Resource<GetPurchaseDetail>>()
	val getPurchaseProductRepo: MutableLiveData<Resource<GetPurchaseDetail>>
		get() = _getPurchaseProductResponse

	fun getPurchaseProduct(
		shippingId : RequestBody?,
		productId : RequestBody?
	) = viewModelScope.launch {
		_getPurchaseProductResponse.value = repo.getPurchaseProduct(shippingId,productId)
	}

	private var _createOrderResponse = MutableLiveData<Resource<CommonResponse>>()
	val createOrderRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _createOrderResponse

	fun createOrder(
		shippingId : RequestBody?,
		productId : RequestBody?,
		cardId : RequestBody?,
		promoCode : RequestBody?,
		sendAsGift : RequestBody?,
		giftUserId : RequestBody?,
		giftMsg : RequestBody?,
		shippingCharges : RequestBody?,
		taxAmount : RequestBody?,
		subTotal : RequestBody?,
		total : RequestBody?,
		discount : RequestBody? =null
	) = viewModelScope.launch {
		_createOrderResponse.value = repo.createOrder(shippingId,productId,cardId,promoCode,sendAsGift,giftUserId,giftMsg,shippingCharges,taxAmount,subTotal,total,discount)
	}

	private var _generateTokenResponse = MutableLiveData<Resource<GenerateTokenResponse>>()
	val generateTokenRepo: MutableLiveData<Resource<GenerateTokenResponse>>
		get() = _generateTokenResponse

	fun generateToken(
		showId : RequestBody?
		) = viewModelScope.launch {
		_generateTokenResponse.value = repo.generateToken(showId)
	}

	
}