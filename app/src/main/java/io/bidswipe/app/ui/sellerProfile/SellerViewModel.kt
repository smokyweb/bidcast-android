package io.bidswipe.app.ui.sellerProfile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.BlockedUnblockedResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FollowUnfollowResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetRatingResponse
import io.bidswipe.app.network.response.GetUserProfileResponse
import io.bidswipe.app.network.response.SentTipAmountResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class SellerViewModel @Inject constructor(
	val repo: DashRepository,
	private val networkMonitor: NetworkMonitor
) : ViewModel() {

	private var _getProfileByIdResponse = MutableLiveData<Resource<GetUserProfileResponse>>()
	val getProfileByIdShowRepo: MutableLiveData<Resource<GetUserProfileResponse>>
		get() = _getProfileByIdResponse

	fun getProfileById(
		userId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getProfileByIdResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getProfileByIdResponse.value = repo.getProfileById(userId)
	}

	private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
	val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
		get() = _getUserProductsResponse

	fun getUserProducts(
		userId: RequestBody? = null,
		categoryId: RequestBody? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getUserProductsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getUserProductsResponse.value = repo.getUserProducts(userId, categoryId)
	}

	private var _followUserResponse = MutableLiveData<Resource<FollowUnfollowResponse>>()
	val followUserShowRepo: MutableLiveData<Resource<FollowUnfollowResponse>>
		get() = _followUserResponse

	fun followUser(
		userId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_followUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_followUserResponse.value = repo.followUser(userId)
	}

	private var _notifyLiveUserResponse = MutableLiveData<Resource<CommonResponse>>()
	val notifyLiveUserRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _notifyLiveUserResponse

	fun notifyLiveUser(
		liveUserId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_notifyLiveUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_notifyLiveUserResponse.value = repo.notifyLiveUser(liveUserId)
	}

	private var _getMyScheduledShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getMyScheduledShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getMyScheduledShowResponse

	fun getMyScheduledShow(
		type: RequestBody? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getMyScheduledShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getMyScheduledShowResponse.value = repo.getMyScheduledShow(type)
	}

	private var _getSellerRatingResponse = MutableLiveData<Resource<GetRatingResponse>>()
	val getSellerRatingRepo: MutableLiveData<Resource<GetRatingResponse>>
		get() = _getSellerRatingResponse

	fun getSellerRating(
		sellerId: String?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getSellerRatingResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSellerRatingResponse.value = repo.getSellerRating(sellerId)
	}

	private var _blockUnblockUserResponse = MutableLiveData<Resource<BlockedUnblockedResponse>>()
	val blockUnblockUserRepo: MutableLiveData<Resource<BlockedUnblockedResponse>>
		get() = _blockUnblockUserResponse

	fun blockUnblockUser(
		blockedID: RequestBody,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_blockUnblockUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_blockUnblockUserResponse.value = repo.blockUnblockUser(blockedID)
	}

	private var _sendTipAmountResponse = MutableLiveData<Resource<SentTipAmountResponse>>()
	val sendTipAmountRepo: MutableLiveData<Resource<SentTipAmountResponse>>
		get() = _sendTipAmountResponse

	fun sendTipAmount(
		sellerId: RequestBody,
		amount: RequestBody,
		cardNumber: RequestBody?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_sendTipAmountResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_sendTipAmountResponse.value = repo.sendTipAmount(sellerId, amount, cardNumber)
	}

}