package io.bidswipe.app.ui.more

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.GetBuyerIdentityResponse
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.GetStatesResponse
import io.bidswipe.app.network.response.SetDefaultAddressResponse
import io.bidswipe.app.network.response.SettingListResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
	val repo: DashRepository,
	private val networkMonitor: NetworkMonitor
) : ViewModel() {

	private var _aboutUsResponse = MutableLiveData<Resource<AboutUsResponse>>()
	val aboutUsRepo: MutableLiveData<Resource<AboutUsResponse>>
		get() = _aboutUsResponse

	fun aboutUs(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_aboutUsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_aboutUsResponse.value = repo.aboutUs()
	}

	private var _getTermsConditionsResponse = MutableLiveData<Resource<TermsConditionResponse>>()
	val getTermsConditionsRepo: MutableLiveData<Resource<TermsConditionResponse>>
		get() = _getTermsConditionsResponse


	fun getTermsConditions(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getTermsConditionsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getTermsConditionsResponse.value = repo.getTermsConditions()
	}

	private var _getPrivacyPolicyResponse = MutableLiveData<Resource<TermsConditionResponse>>()
	val getPrivacyPolicyRepo: MutableLiveData<Resource<TermsConditionResponse>>
		get() = _getPrivacyPolicyResponse

	fun getPrivacyPolicy(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPrivacyPolicyResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPrivacyPolicyResponse.value = repo.getPrivacyPolicy()
	}

	private var _getFAQResponse = MutableLiveData<Resource<FAQResponse>>()
	val getFAQRepo: MutableLiveData<Resource<FAQResponse>>
		get() = _getFAQResponse

	fun getFAQ() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getFAQResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getFAQResponse.value = repo.getFAQ()
	}

	private var _contactUsResponse = MutableLiveData<Resource<CommonResponse>>()
	val contactUsRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _contactUsResponse

	fun contactUs(
		name: RequestBody?,
		email: RequestBody?,
		subject: RequestBody?,
		message: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_contactUsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_contactUsResponse.value = repo.contactUs(name, email, subject, message)
	}

	private var _addShippingAddressResponse = MutableLiveData<Resource<CommonResponse>>()
	val addShippingAddressRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _addShippingAddressResponse

	fun addShippingAddress(
		type: RequestBody?,
		name: RequestBody?,
		phoneNumber: RequestBody?,
		streetAddress: RequestBody?,
		pinCode: RequestBody?,
		city: RequestBody?,
		state: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_addShippingAddressResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_addShippingAddressResponse.value =
			repo.addShippingAddress(type, name, phoneNumber, streetAddress, pinCode, city, state)
	}

	private var _getShippingAddressResponse =
		MutableLiveData<Resource<GetShippingAddressResponse>>()
	val getShippingAddressRepo: MutableLiveData<Resource<GetShippingAddressResponse>>
		get() = _getShippingAddressResponse

	fun getShippingAddress() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getShippingAddressResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getShippingAddressResponse.value = repo.getShippingAddress()
	}

	private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
	val getPaymentCardRepo: MutableLiveData<Resource<GetPaymentCardsResponse>>
		get() = _getPaymentCardResponse

	fun getPaymentCard(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPaymentCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPaymentCardResponse.value = repo.getPaymentCard()
	}

	private var _settingsListResponse = MutableLiveData<Resource<SettingListResponse>>()
	val settingsListRepo: MutableLiveData<Resource<SettingListResponse>>
		get() = _settingsListResponse

	fun settingsList(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_settingsListResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_settingsListResponse.value = repo.settingsList()
	}

	private var _settingsStoreResponse = MutableLiveData<Resource<CommonResponse>>()
	val settingsStoreRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _settingsStoreResponse

	fun settingsStore(
		countryOfResidence: RequestBody? = null,
		directMessage: RequestBody? = null,
		receiveGifts: RequestBody? = null,
		enablePrivateEntry: RequestBody? = null,
		showRewardStatus: RequestBody? = null,
		showSellerTools: RequestBody? = null,
		enableClips: RequestBody? = null,
		savePastShows: RequestBody? = null,
		activityStatus: RequestBody? = null,
		syncPhoneContacts: RequestBody? = null,
		suggestMyAccount: RequestBody? = null,
		hapticFeedback: RequestBody? = null,
		freeShipping: RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_settingsStoreResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_settingsStoreResponse.value = repo.settingsStore(
			countryOfResidence,
			directMessage,
			receiveGifts,
			enablePrivateEntry,
			showRewardStatus,
			showSellerTools,
			enableClips,
			savePastShows,
			activityStatus,
			syncPhoneContacts,
			suggestMyAccount,
			hapticFeedback,
			freeShipping
		)
	}

	private var _storeBuyerIdentityResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeBuyerIdentityRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _storeBuyerIdentityResponse

	fun storeBuyerIdentity(
		image: MultipartBody.Part?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeBuyerIdentityResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeBuyerIdentityResponse.value = repo.storeBuyerIdentity(image)
	}

	private var _fetchBuyerIdentityResponse = MutableLiveData<Resource<GetBuyerIdentityResponse>>()
	val fetchBuyerIdentityRepo: MutableLiveData<Resource<GetBuyerIdentityResponse>>
		get() = _fetchBuyerIdentityResponse

	fun fetchBuyerIdentity(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_fetchBuyerIdentityResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fetchBuyerIdentityResponse.value = repo.fetchBuyerIdentity()
	}

	private var _getNotificationResponse = MutableLiveData<Resource<GetNotificationResponse>>()
	val getNotificationRepo: MutableLiveData<Resource<GetNotificationResponse>>
		get() = _getNotificationResponse

	fun getNotification(
		page: String?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getNotificationResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getNotificationResponse.value = repo.getNotification(page)
	}

	private var _deleteNotificationResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteNotificationRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteNotificationResponse

	fun deleteNotification(
		id: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_deleteNotificationResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_deleteNotificationResponse.value = repo.deleteNotification(id)
	}

	private var _setDefaultShippingAddressResponse =
		MutableLiveData<Resource<SetDefaultAddressResponse>>()
	val setDefaultShippingAddressRepo: MutableLiveData<Resource<SetDefaultAddressResponse>>
		get() = _setDefaultShippingAddressResponse

	fun setDefaultShippingAddress(
		addressId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_setDefaultShippingAddressResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_setDefaultShippingAddressResponse.value = repo.setDefaultShippingAddress(addressId)
	}

	private var _setDefaultCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val setDefaultCardRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _setDefaultCardResponse

	fun setDefaultCard(
		cardId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_setDefaultCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_setDefaultCardResponse.value = repo.setDefaultCard(cardId)
	}

	private var _deleteCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteCardRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteCardResponse

	fun deleteCard(
		cardId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_deleteCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_deleteCardResponse.value = repo.deleteCard(cardId)
	}

	private var _deleteAddressResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteAddressRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteAddressResponse

	fun deleteAddress(
		addressId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_deleteAddressResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_deleteAddressResponse.value = repo.deleteAddress(addressId)
	}

	private var _getStatesResponse = MutableLiveData<Resource<GetStatesResponse>>()
	val getStatesRepo: MutableLiveData<Resource<GetStatesResponse>>
		get() = _getStatesResponse

	fun getStates() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getStatesResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getStatesResponse.value = repo.getStates()
	}


}