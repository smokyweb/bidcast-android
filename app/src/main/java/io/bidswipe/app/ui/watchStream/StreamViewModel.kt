package io.bidswipe.app.ui.watchStream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.BlockedUnblockedResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateBidResponse
import io.bidswipe.app.network.response.FollowUnfollowResponse
import io.bidswipe.app.network.response.GetReportCategoriesResponse
import io.bidswipe.app.network.response.SellerInfoResponse
import io.bidswipe.app.network.response.SentTipAmountResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
	val repo : DashRepository,
	private val networkMonitor : NetworkMonitor
) : ViewModel() {

	// LiveData to hold the list or individual streams
    private val _streams = MutableLiveData<List<StreamModel>>()

    val streams: LiveData<List<StreamModel>> = _streams

    fun setStreams(newStreams: List<StreamModel>) {
		_streams.value = newStreams
	}

	// Optionally, you can have a LiveData for the currently selected stream
    private val _selectedStream = MutableLiveData<StreamModel>()
    val selectedStream: LiveData<StreamModel> = _selectedStream

    fun selectStream(stream: StreamModel) {
		_selectedStream.value = stream
	}

	private var _createBidResponse = MutableLiveData<Resource<CreateBidResponse>>()
	val createBidRepo : MutableLiveData<Resource<CreateBidResponse>>
		get() = _createBidResponse

	fun createBid(
        showId : RequestBody? ,
        userId : RequestBody? ,
        productId : RequestBody? ,
        bidPrice : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_createBidResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_createBidResponse.value = repo.createBid(showId , userId , productId , bidPrice)
	}

	private var _followUserResponse = MutableLiveData<Resource<FollowUnfollowResponse>>()
	val followUserShowRepo : MutableLiveData<Resource<FollowUnfollowResponse>>
		get() = _followUserResponse

	fun followUser(
        userId : RequestBody? ,
        showId : RequestBody?
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_followUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_followUserResponse.value = repo.followUser(userId, showId)
	}

	private var _sendTipAmountResponse = MutableLiveData<Resource<SentTipAmountResponse>>()
	val sendTipAmountRepo : MutableLiveData<Resource<SentTipAmountResponse>>
		get() = _sendTipAmountResponse

	fun sendTipAmount(
		sellerId : RequestBody ,
		amount : RequestBody ,
		cardNumber : RequestBody?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_sendTipAmountResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_sendTipAmountResponse.value = repo.sendTipAmount(sellerId, amount, cardNumber)
	}

	private var _getSellerInfoResponse = MutableLiveData<Resource<SellerInfoResponse>>()
	val getSellerInfoRepo : MutableLiveData<Resource<SellerInfoResponse>>
		get() = _getSellerInfoResponse

	fun getSellerInfo(
		sellerId : String
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getSellerInfoResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSellerInfoResponse.value = repo.getSellerInfo(sellerId)
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


	private var _getReportCategoriesResponse = MutableLiveData<Resource<GetReportCategoriesResponse>>()
	val getReportCategoriesRepo: MutableLiveData<Resource<GetReportCategoriesResponse>>
		get() = _getReportCategoriesResponse

	fun getReportCategories(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getReportCategoriesResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getReportCategoriesResponse.value = repo.getReportCategories()
	}

	private var _reportSellerResponse = MutableLiveData<Resource<CommonResponse>>()
	val reportSellerRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _reportSellerResponse

	fun reportSeller(
		sellerId: RequestBody,
		categoryId: RequestBody?,
		notes: RequestBody?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_reportSellerResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_reportSellerResponse.value = repo.reportSeller(sellerId, categoryId, notes)
	}

}