package io.bidswipe.app.ui.dashboard.sellerProfile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetUserProfileResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject


@HiltViewModel
class SellerViewModel  @Inject constructor(val repo: DashRepository) : ViewModel() {

    private var _getProfileByIdResponse = MutableLiveData<Resource<GetUserProfileResponse>>()
    val getProfileByIdShowRepo: MutableLiveData<Resource<GetUserProfileResponse>>
        get() = _getProfileByIdResponse

    fun getProfileById(
        userId : RequestBody?
    ) = viewModelScope.launch {
        _getProfileByIdResponse.value = repo.getProfileById(userId)
    }


    private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
    val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
        get() = _getUserProductsResponse

    fun getUserProducts(
    ) = viewModelScope.launch {
        _getUserProductsResponse.value = repo.getUserProducts()
    }


    private var _followUserResponse = MutableLiveData<Resource<CommonResponse>>()
    val followUserShowRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _followUserResponse

    fun followUser(
        userId : RequestBody?
    ) = viewModelScope.launch {
        _followUserResponse.value = repo.followUser(userId)
    }

    private var _notifyLiveUserResponse = MutableLiveData<Resource<CommonResponse>>()
    val notifyLiveUserRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _notifyLiveUserResponse

    fun notifyLiveUser(
        liveUserId : RequestBody?
    ) = viewModelScope.launch {
        _notifyLiveUserResponse.value = repo.notifyLiveUser(liveUserId)
    }

}