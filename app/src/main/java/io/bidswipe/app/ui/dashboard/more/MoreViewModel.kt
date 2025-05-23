package io.bidswipe.app.ui.dashboard.more

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.AuthRepository
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import kotlinx.coroutines.launch
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

}