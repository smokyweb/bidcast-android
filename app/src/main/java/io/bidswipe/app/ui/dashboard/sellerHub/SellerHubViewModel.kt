package io.bidswipe.app.ui.dashboard.sellerHub

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FetchSellerVerificationResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.StorePhoneNumberResponse
import io.bidswipe.app.network.response.StoreSellerIdResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class SellerHubViewModel@Inject constructor(val repo: DashRepository) : ViewModel() {


    private var _getMyScheduledShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
    val getMyScheduledShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
        get() = _getMyScheduledShowResponse

    fun getMyScheduledShow(
        type : RequestBody? = null
    ) = viewModelScope.launch {
        _getMyScheduledShowResponse.value = repo.getMyScheduledShow(type)
    }

    private var _fetchSellerVerificationResponse = MutableLiveData<Resource<FetchSellerVerificationResponse>>()
    val fetchSellerVerificationRepo: MutableLiveData<Resource<FetchSellerVerificationResponse>>
        get() = _fetchSellerVerificationResponse

    fun fetchSellerVerification(
    ) = viewModelScope.launch {
        _fetchSellerVerificationResponse.value = repo.fetchSellerVerification()
    }

    private var _getMyInventoryResponse = MutableLiveData<Resource<GetMyInventoryResponse>>()
    val getMyInventoryRepo: MutableLiveData<Resource<GetMyInventoryResponse>>
        get() = _getMyInventoryResponse

    fun getMyInventory(
        status : RequestBody?,
        page : RequestBody?
    ) = viewModelScope.launch {
        _getMyInventoryResponse.value = repo.getMyInventory(status,page)
    }

    private var _getOrderListingResponse = MutableLiveData<Resource<GetOrdersResponse>>()
    val getOrderListingRepo: MutableLiveData<Resource<GetOrdersResponse>>
        get() = _getOrderListingResponse

    fun getOrderListing(
        type: RequestBody?,
    ) = viewModelScope.launch {
        _getOrderListingResponse.value = repo.getOrderListing(type)
    }

    private var _offerListResponse = MutableLiveData<Resource<GetOffersResponse>>()
    val offerListRepo: MutableLiveData<Resource<GetOffersResponse>>
        get() = _offerListResponse

    fun offerList(
        page: Int ? = null
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

    private var _storeSellerIdResponse = MutableLiveData<Resource<StoreSellerIdResponse>>()
    val storeSellerIdRepo: MutableLiveData<Resource<StoreSellerIdResponse>>
        get() = _storeSellerIdResponse

    fun storeSellerId(
        idCard : MultipartBody.Part,
        image: MultipartBody.Part
    ) = viewModelScope.launch {
        _storeSellerIdResponse.value = repo.storeSellerId(idCard,image)
    }

    private var _storePhoneNumberResponse = MutableLiveData<Resource<StorePhoneNumberResponse>>()
    val storePhoneNumberRepo: MutableLiveData<Resource<StorePhoneNumberResponse>>
        get() = _storePhoneNumberResponse

    fun storePhoneNumber(
        phoneNumber : RequestBody?
    ) = viewModelScope.launch {
        _storePhoneNumberResponse.value = repo.storePhoneNumber(phoneNumber)
    }

    private var _verifyNumberOtpResponse = MutableLiveData<Resource<CommonResponse>>()
    val verifyNumberOtpRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _verifyNumberOtpResponse

    fun verifyNumberOtp(
        otp : RequestBody?
    ) = viewModelScope.launch {
        _verifyNumberOtpResponse.value = repo.verifyNumberOtp(otp)
    }

    private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
    val getPaymentCardRepo: MutableLiveData<Resource<GetPaymentCardsResponse>>
        get() = _getPaymentCardResponse

    fun getPaymentCard(
    ) = viewModelScope.launch {
        _getPaymentCardResponse.value = repo.getPaymentCard()
    }

    private var _storePaymentMethodResponse = MutableLiveData<Resource<CommonResponse>>()
    val storePaymentMethodRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _storePaymentMethodResponse

    fun storePaymentMethod(
        cardToken : RequestBody?
    ) = viewModelScope.launch {
        _storePaymentMethodResponse.value = repo.storePaymentMethod(cardToken)
    }

}