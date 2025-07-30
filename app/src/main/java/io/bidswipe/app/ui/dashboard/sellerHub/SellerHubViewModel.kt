package io.bidswipe.app.ui.dashboard.sellerHub

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CheckKycResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FetchReferralResponse
import io.bidswipe.app.network.response.FetchSellerVerificationResponse
import io.bidswipe.app.network.response.GetKYCDetailsRespnse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.network.response.SellerStatusResponse
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

    private var _getKYCDetailsResponse = MutableLiveData<Resource<GetKYCDetailsRespnse>>()
    val getKYCDetailsRepo: MutableLiveData<Resource<GetKYCDetailsRespnse>>
        get() = _getKYCDetailsResponse

    fun getKYCDetails(
        cardToken : RequestBody?
    ) = viewModelScope.launch {
        _getKYCDetailsResponse.value = repo.getKYCDetails()
    }

    private var _checkKycResponse = MutableLiveData<Resource<CheckKycResponse>>()
    val checkKycRepo: MutableLiveData<Resource<CheckKycResponse>>
        get() = _checkKycResponse

    fun checkKyc(
    ) = viewModelScope.launch {
        _checkKycResponse.value = repo.checkKyc()
    }

    private var _fundTransferResponse = MutableLiveData<Resource<CheckKycResponse>>()
    val fundTransferRepo: MutableLiveData<Resource<CheckKycResponse>>
        get() = _fundTransferResponse

    fun fundTransfer(
        amount : RequestBody?
    ) = viewModelScope.launch {
        _fundTransferResponse.value = repo.fundTransfer(amount)
    }

    private var _getPayoutHistoryResponse = MutableLiveData<Resource<CommonResponse>>()
    val getPayoutHistoryRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _getPayoutHistoryResponse

    fun getPayoutHistory(
    ) = viewModelScope.launch {
        _getPayoutHistoryResponse.value = repo.getPayoutHistory()
    }

    private var _getTransactionsHistoryResponse = MutableLiveData<Resource<GetTransactionsHistoryResponse>>()
    val getTransactionsHistoryRepo: MutableLiveData<Resource<GetTransactionsHistoryResponse>>
        get() = _getTransactionsHistoryResponse

    fun getTransactionsHistory(
        page : RequestBody?
    ) = viewModelScope.launch {
        _getTransactionsHistoryResponse.value = repo.getTransactionsHistory(page)
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

    private var _fetchReferralResponse = MutableLiveData<Resource<FetchReferralResponse>>()
    val fetchReferralRepo: MutableLiveData<Resource<FetchReferralResponse>>
        get() = _fetchReferralResponse

    fun fetchReferral(
    ) = viewModelScope.launch {
        _fetchReferralResponse.value = repo.fetchReferral()
    }

    private var _storeSellerVerificationResponse = MutableLiveData<Resource<CommonResponse>>()
    val storeSellerVerificationRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _storeSellerVerificationResponse

    fun storeSellerVerification(
        id: MultipartBody.Part?,
        image: MultipartBody.Part?,
        phoneVerification: RequestBody,
        cardId: RequestBody
    ) = viewModelScope.launch {
        _storeSellerVerificationResponse.value = repo.storeSellerVerification(id,image,phoneVerification,cardId)
    }

    private var _getSellerStatusResponse = MutableLiveData<Resource<SellerStatusResponse>>()
    val getSellerStatusRepo: MutableLiveData<Resource<SellerStatusResponse>>
        get() = _getSellerStatusResponse

    fun getSellerStatus(
    ) = viewModelScope.launch {
        _getSellerStatusResponse.value = repo.getSellerStatus()
    }

}