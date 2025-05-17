package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.AuthRepository
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject


@HiltViewModel
class DashViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {
    var lastIndex =MutableLiveData(0)

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



}