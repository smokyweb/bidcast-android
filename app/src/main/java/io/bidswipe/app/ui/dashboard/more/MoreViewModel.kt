package io.bidswipe.app.ui.dashboard.more

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
import io.bidswipe.app.network.response.SetDefaultAddressResponse
import io.bidswipe.app.network.response.SettingListResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {

	private var _aboutUsResponse = MutableLiveData<Resource<AboutUsResponse>>()
	val aboutUsRepo: MutableLiveData<Resource<AboutUsResponse>>
		get() = _aboutUsResponse

	fun aboutUs(
	) = viewModelScope.launch {
		_aboutUsResponse.value = repo.aboutUs()
	}

	private var _getTermsConditionsResponse = MutableLiveData<Resource<TermsConditionResponse>>()
	val getTermsConditionsRepo: MutableLiveData<Resource<TermsConditionResponse>>
		get() = _getTermsConditionsResponse


	fun getTermsConditions(
	) = viewModelScope.launch {
		_getTermsConditionsResponse.value = repo.getTermsConditions()
	}

	private var _getPrivacyPolicyResponse = MutableLiveData<Resource<TermsConditionResponse>>()
	val getPrivacyPolicyRepo: MutableLiveData<Resource<TermsConditionResponse>>
		get() = _getPrivacyPolicyResponse

	fun getPrivacyPolicy(
	) = viewModelScope.launch {
		_getPrivacyPolicyResponse.value = repo.getPrivacyPolicy()
	}

	private var _getFAQResponse = MutableLiveData<Resource<FAQResponse>>()
	val getFAQRepo: MutableLiveData<Resource<FAQResponse>>
		get() = _getFAQResponse

	fun getFAQ() = viewModelScope.launch {
		_getFAQResponse.value = repo.getFAQ()
	}

	private var _contactUsResponse = MutableLiveData<Resource<CommonResponse>>()
    val contactUsRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _contactUsResponse

    fun contactUs(
        name : RequestBody?,
        email: RequestBody?,
        subject: RequestBody?,
        message : RequestBody?
    ) = viewModelScope.launch {
        _contactUsResponse.value = repo.contactUs(name,email,subject,message)
    }

    private var _addShippingAddressResponse = MutableLiveData<Resource<CommonResponse>>()
	val addShippingAddressRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _addShippingAddressResponse

	fun addShippingAddress(
		type: RequestBody?,
		name: RequestBody?,
		phoneNumber: RequestBody?,
		streetAddress: RequestBody?,
		pinCode: RequestBody?
	) = viewModelScope.launch {
		_addShippingAddressResponse.value = repo.addShippingAddress(type, name, phoneNumber, streetAddress, pinCode)
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

	private var _settingsListResponse = MutableLiveData<Resource<SettingListResponse>>()
	val settingsListRepo: MutableLiveData<Resource<SettingListResponse>>
		get() = _settingsListResponse

	fun settingsList(
	) = viewModelScope.launch {
		_settingsListResponse.value = repo.settingsList()
	}

	private var _settingsStoreResponse = MutableLiveData<Resource<CommonResponse>>()
	val settingsStoreRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _settingsStoreResponse

	fun settingsStore(
		countryOfResidence: RequestBody?,
		directMessage: RequestBody?,
		receiveGifts: RequestBody?,
		enablePrivateEntry: RequestBody?,
		showRewardStatus: RequestBody?,
		showSellerTools: RequestBody?,
		enableClips: RequestBody?,
		savePastShows: RequestBody?,
		activityStatus: RequestBody?,
		syncPhoneContacts: RequestBody?,
		suggestMyAccount: RequestBody?,
		hapticFeedback: RequestBody?,
	) = viewModelScope.launch {
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
			hapticFeedback
		)
	}

	private var _storeBuyerIdentityResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeBuyerIdentityRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _storeBuyerIdentityResponse

	fun storeBuyerIdentity(
		image :  MultipartBody.Part?
	) = viewModelScope.launch {
		_storeBuyerIdentityResponse.value = repo.storeBuyerIdentity(image)
	}

	private var _fetchBuyerIdentityResponse = MutableLiveData<Resource<GetBuyerIdentityResponse>>()
	val fetchBuyerIdentityRepo: MutableLiveData<Resource<GetBuyerIdentityResponse>>
		get() = _fetchBuyerIdentityResponse

	fun fetchBuyerIdentity(
	) = viewModelScope.launch {
		_fetchBuyerIdentityResponse.value = repo.fetchBuyerIdentity()
	}

	private var _getNotificationResponse = MutableLiveData<Resource<GetNotificationResponse>>()
	val getNotificationRepo: MutableLiveData<Resource<GetNotificationResponse>>
		get() = _getNotificationResponse

	fun getNotification(
	) = viewModelScope.launch {
		_getNotificationResponse.value = repo.getNotification()
	}

	private var _deleteNotificationResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteNotificationRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteNotificationResponse

	fun deleteNotification(
		id : RequestBody?
	) = viewModelScope.launch {
		_deleteNotificationResponse.value = repo.deleteNotification(id)
	}

	private var _setDefaultShippingAddressResponse = MutableLiveData<Resource<SetDefaultAddressResponse>>()
	val setDefaultShippingAddressRepo: MutableLiveData<Resource<SetDefaultAddressResponse>>
		get() = _setDefaultShippingAddressResponse

	fun setDefaultShippingAddress(
		addressId : RequestBody?
	) = viewModelScope.launch {
		_setDefaultShippingAddressResponse.value = repo.setDefaultShippingAddress(addressId)
	}


	private var _setDefaultCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val setDefaultCardRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _setDefaultCardResponse

	fun setDefaultCard(
		cardId : RequestBody?
	) = viewModelScope.launch {
		_setDefaultCardResponse.value = repo.setDefaultCard(cardId)
	}

	private var _deleteCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteCardRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteCardResponse

	fun deleteCard(
		cardId : RequestBody?
	) = viewModelScope.launch {
		_deleteCardResponse.value = repo.deleteCard(cardId)
	}

	private var _deleteAddressResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteAddressRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteAddressResponse

	fun deleteAddress(
		addressId : RequestBody?
	) = viewModelScope.launch {
		_deleteAddressResponse.value = repo.deleteAddress(addressId)
	}


}