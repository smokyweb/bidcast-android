package io.bidswipe.app.ui.sellerHub

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
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.network.response.GetPromoteToolsResponse
import io.bidswipe.app.network.response.GetTipAmountResponse
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.network.response.PayoutHistoryResponse
import io.bidswipe.app.network.response.SalesAnalyticsResponse
import io.bidswipe.app.network.response.SellerAnalyticsResponse
import io.bidswipe.app.network.response.SellerStatusResponse
import io.bidswipe.app.network.response.StorePhoneNumberResponse
import io.bidswipe.app.network.response.StoreSellerIdResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import io.bidswipe.app.network.response.VisitorsAnalyticsResponse
import io.bidswipe.app.network.response.WalletInfoResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class SellerHubViewModel @Inject constructor(
	val repo : DashRepository,
	private val networkMonitor : NetworkMonitor
) : ViewModel() {

	private var _getMyScheduledShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getMyScheduledShowRepo : MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getMyScheduledShowResponse

	fun getMyScheduledShow(
        type : RequestBody? = null ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getMyScheduledShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getMyScheduledShowResponse.value = repo.getMyScheduledShow(type)
	}

	private var _fetchSellerVerificationResponse = MutableLiveData<Resource<FetchSellerVerificationResponse>>()
	val fetchSellerVerificationRepo : MutableLiveData<Resource<FetchSellerVerificationResponse>>
		get() = _fetchSellerVerificationResponse

	fun fetchSellerVerification(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_fetchSellerVerificationResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fetchSellerVerificationResponse.value = repo.fetchSellerVerification()
	}

	private var _getMyInventoryResponse = MutableLiveData<Resource<GetMyInventoryResponse>>()
	val getMyInventoryRepo : MutableLiveData<Resource<GetMyInventoryResponse>>
		get() = _getMyInventoryResponse

	fun getMyInventory(
        status : RequestBody? ,
        page : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getMyInventoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getMyInventoryResponse.value = repo.getMyInventory(status , page)
	}

	private var _getOrderListingResponse = MutableLiveData<Resource<GetOrdersResponse>>()
	val getOrderListingRepo : MutableLiveData<Resource<GetOrdersResponse>>
		get() = _getOrderListingResponse

	fun getOrderListing(
		page: RequestBody?,
		type : RequestBody? ,
		search : RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getOrderListingResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getOrderListingResponse.value = repo.getOrderListing(page,type, search)
	}

	private var _offerListResponse = MutableLiveData<Resource<GetOffersResponse>>()
	val offerListRepo : MutableLiveData<Resource<GetOffersResponse>>
		get() = _offerListResponse

	fun offerList(
        page : Int? = null ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_offerListResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_offerListResponse.value = repo.offerList(page)
	}

	private var _offerUpdateStatusResponse = MutableLiveData<Resource<UpdateOfferResponse>>()
	val offerUpdateStatusRepo : MutableLiveData<Resource<UpdateOfferResponse>>
		get() = _offerUpdateStatusResponse

	fun offerUpdateStatus(
        offerId : RequestBody? ,
        status : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_offerUpdateStatusResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_offerUpdateStatusResponse.value = repo.offerUpdateStatus(offerId , status)
	}

	private var _storeSellerIdResponse = MutableLiveData<Resource<StoreSellerIdResponse>>()
	val storeSellerIdRepo : MutableLiveData<Resource<StoreSellerIdResponse>>
		get() = _storeSellerIdResponse

	fun storeSellerId(
        idCard : MultipartBody.Part ,
        image : MultipartBody.Part ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeSellerIdResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeSellerIdResponse.value = repo.storeSellerId(idCard , image)
	}

	private var _storePhoneNumberResponse = MutableLiveData<Resource<StorePhoneNumberResponse>>()
	val storePhoneNumberRepo : MutableLiveData<Resource<StorePhoneNumberResponse>>
		get() = _storePhoneNumberResponse

	fun storePhoneNumber(
        phoneNumber : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storePhoneNumberResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storePhoneNumberResponse.value = repo.storePhoneNumber(phoneNumber)
	}

	private var _verifyNumberOtpResponse = MutableLiveData<Resource<CommonResponse>>()
	val verifyNumberOtpRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _verifyNumberOtpResponse

	fun verifyNumberOtp(
        otp : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_verifyNumberOtpResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_verifyNumberOtpResponse.value = repo.verifyNumberOtp(otp)
	}

	private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
	val getPaymentCardRepo : MutableLiveData<Resource<GetPaymentCardsResponse>>
		get() = _getPaymentCardResponse

	fun getPaymentCard(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPaymentCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPaymentCardResponse.value = repo.getPaymentCard()
	}

	private var _storePaymentMethodResponse = MutableLiveData<Resource<CommonResponse>>()
	val storePaymentMethodRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _storePaymentMethodResponse

	fun storePaymentMethod(
        cardToken : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storePaymentMethodResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storePaymentMethodResponse.value = repo.storePaymentMethod(cardToken)
	}

	private var _getKYCDetailsResponse = MutableLiveData<Resource<GetKYCDetailsRespnse>>()
	val getKYCDetailsRepo : MutableLiveData<Resource<GetKYCDetailsRespnse>>
		get() = _getKYCDetailsResponse

	fun getKYCDetails(
        cardToken : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getKYCDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getKYCDetailsResponse.value = repo.getKYCDetails()
	}

	private var _checkKycResponse = MutableLiveData<Resource<CheckKycResponse>>()
	val checkKycRepo : MutableLiveData<Resource<CheckKycResponse>>
		get() = _checkKycResponse

	fun checkKyc(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_checkKycResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_checkKycResponse.value = repo.checkKyc()
	}

	private var _fundTransferResponse = MutableLiveData<Resource<CheckKycResponse>>()
	val fundTransferRepo : MutableLiveData<Resource<CheckKycResponse>>
		get() = _fundTransferResponse

	fun fundTransfer(
        amount : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_fundTransferResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fundTransferResponse.value = repo.fundTransfer(amount)
	}

	private var _getPayoutHistoryResponse = MutableLiveData<Resource<PayoutHistoryResponse>>()
	val getPayoutHistoryRepo : MutableLiveData<Resource<PayoutHistoryResponse>>
		get() = _getPayoutHistoryResponse

	fun getPayoutHistory(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPayoutHistoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPayoutHistoryResponse.value = repo.getPayoutHistory()
	}

	private var _getTransactionsHistoryResponse = MutableLiveData<Resource<GetTransactionsHistoryResponse>>()
	val getTransactionsHistoryRepo : MutableLiveData<Resource<GetTransactionsHistoryResponse>>
		get() = _getTransactionsHistoryResponse

	fun getTransactionsHistory(
        page : RequestBody? ,
        status : RequestBody? = null
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getTransactionsHistoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getTransactionsHistoryResponse.value = repo.getTransactionsHistory(page,status)
	}

	private var _updateProfileResponse = MutableLiveData<Resource<CommonResponse>>()
	val updateProfileRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _updateProfileResponse

	fun updateProfile(
        firstName : RequestBody ,
        lastName : RequestBody ,
        image : MultipartBody.Part? ,
        userName : RequestBody ,
        bio : RequestBody ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_updateProfileResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_updateProfileResponse.value = repo.updateProfile(firstName , lastName , image , userName , bio)
	}

	private var _fetchReferralResponse = MutableLiveData<Resource<FetchReferralResponse>>()
	val fetchReferralRepo : MutableLiveData<Resource<FetchReferralResponse>>
		get() = _fetchReferralResponse

	fun fetchReferral(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_fetchReferralResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fetchReferralResponse.value = repo.fetchReferral()
	}

	private var _storeSellerVerificationResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeSellerVerificationRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _storeSellerVerificationResponse

	fun storeSellerVerification(
        id : MultipartBody.Part? ,
        image : MultipartBody.Part? ,
        phoneVerification : RequestBody ,
        cardId : RequestBody ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeSellerVerificationResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeSellerVerificationResponse.value = repo.storeSellerVerification(id , image , phoneVerification , cardId)
	}

	private var _getSellerStatusResponse = MutableLiveData<Resource<SellerStatusResponse>>()
	val getSellerStatusRepo : MutableLiveData<Resource<SellerStatusResponse>>
		get() = _getSellerStatusResponse

	fun getSellerStatus(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getSellerStatusResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSellerStatusResponse.value = repo.getSellerStatus()
	}

	private var _getPremierShopResponse = MutableLiveData<Resource<GetPremierShopResponse>>()
	val getPremierShopRepo : MutableLiveData<Resource<GetPremierShopResponse>>
		get() = _getPremierShopResponse

	fun getPremierShop(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPremierShopResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPremierShopResponse.value = repo.getPremierShop()
	}

	private var _getPromoteToolsResponse = MutableLiveData<Resource<GetPromoteToolsResponse>>()
	val getPromoteToolsRepo : MutableLiveData<Resource<GetPromoteToolsResponse>>
		get() = _getPromoteToolsResponse

	fun getPromoteTools(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPromoteToolsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPromoteToolsResponse.value = repo.getPromoteTools()
	}

	private var _payoutResponse = MutableLiveData<Resource<CommonResponse>>()
	val payoutRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _payoutResponse

	fun payout(
		amount : RequestBody,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_payoutResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_payoutResponse.value = repo.payout(amount)
	}

	private var _applyPremierShopResponse = MutableLiveData<Resource<CommonResponse>>()
	val applyPremierShopRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _applyPremierShopResponse

	fun applyPremierShop(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_applyPremierShopResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_applyPremierShopResponse.value = repo.applyPremierShop()
	}

	private var _walletInfoResponse = MutableLiveData<Resource<WalletInfoResponse>>()
	val walletInfoRepo : MutableLiveData<Resource<WalletInfoResponse>>
		get() = _walletInfoResponse

	fun walletInfo(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_walletInfoResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_walletInfoResponse.value = repo.walletInfo()
	}

	private var getTipAmountResponse = MutableLiveData<Resource<GetTipAmountResponse>>()
	val getTipAmountRepo : MutableLiveData<Resource<GetTipAmountResponse>>
		get() = getTipAmountResponse

	fun getTipAmount(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			getTipAmountResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		getTipAmountResponse.value = repo.getTipAmount()
	}

	private var getSellerAnalyticsResponse = MutableLiveData<Resource<SellerAnalyticsResponse>>()
	val getSellerAnalyticsRepo : MutableLiveData<Resource<SellerAnalyticsResponse>>
		get() = getSellerAnalyticsResponse

	fun getSellerAnalytics(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			getSellerAnalyticsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		getSellerAnalyticsResponse.value = repo.getSellerAnalytics()
	}

	private var getVisitorsAnalyticsResponse = MutableLiveData<Resource<VisitorsAnalyticsResponse>>()
	val getVisitorsAnalyticsRepo : MutableLiveData<Resource<VisitorsAnalyticsResponse>>
		get() = getVisitorsAnalyticsResponse

	fun getVisitorsAnalytics(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			getVisitorsAnalyticsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		getVisitorsAnalyticsResponse.value = repo.getVisitorsAnalytics()
	}

	private var getSalesPerformanceResponse = MutableLiveData<Resource<SalesAnalyticsResponse>>()
	val getSalesPerformanceRepo : MutableLiveData<Resource<SalesAnalyticsResponse>>
		get() = getSalesPerformanceResponse

	fun getSalesPerformance(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			getSalesPerformanceResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		getSalesPerformanceResponse.value = repo.getSalesPerformance()
	}

}