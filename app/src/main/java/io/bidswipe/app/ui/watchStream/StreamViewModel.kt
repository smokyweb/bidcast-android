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
import io.bidswipe.app.network.response.GetShowDetailsResponse
import io.bidswipe.app.network.response.MakeClipResponse
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
	val streamsList = MutableLiveData<MutableList<StreamModel>>()

    val streams: LiveData<MutableList<StreamModel>> = streamsList

    fun setStreams(newStreams: MutableList<StreamModel>) {
		streamsList.value = newStreams
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

	private var _getClipResponse = MutableLiveData<Resource<MakeClipResponse>>()
	val getClipRepo: MutableLiveData<Resource<MakeClipResponse>>
		get() = _getClipResponse

	fun getClip(
		roomId : RequestBody?,
		// Basecamp #9929851737 (2026-05-26): optional clip duration.
		// Null = backend default (60s). Bounded 5..300 server-side.
		durationSec: RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getClipResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getClipResponse.value = repo.getClip(roomId, durationSec)
	}

	// Basecamp #9937970358 (2026-05-28): REST fallback for buyer show view.
	// When the buyer joins a live show, the entire UI (username, rating,
	// products, chat, tip, share, your details buttons) is hydrated from the
	// socket event `room_create_get`. If that event never arrives (race
	// condition, socket disconnect, server hiccup), the buyer is stuck on the
	// XML's static defaults: literal "Username", hardcoded ★5.0, no bottom
	// UI. Adding a REST fallback so WatchStreamFragment can hydrate even when
	// the socket path silently fails.
	private var _getShowDetailsResponse = MutableLiveData<Resource<GetShowDetailsResponse>>()
	val getShowDetailsRepo: MutableLiveData<Resource<GetShowDetailsResponse>>
		get() = _getShowDetailsResponse

	fun getShowDetails(
		showId: String
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getShowDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getShowDetailsResponse.value = repo.getShowDetails(showId)
	}

	fun clearShowDetailsResult() {
		_getShowDetailsResponse.value = null
	}

	// Basecamp #9933847997 (2026-05-29): in-show pre-bid via Retrofit
	private var _placePrebidResponse = MutableLiveData<Resource<io.bidswipe.app.network.response.PreBidResponse>>()
	val placePrebidRepo: MutableLiveData<Resource<io.bidswipe.app.network.response.PreBidResponse>>
		get() = _placePrebidResponse

	fun placePrebid(productId: Int, amount: Double, scheduleShowId: Int? = null) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_placePrebidResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_placePrebidResponse.value = repo.placePrebid(productId, amount, scheduleShowId)
	}

	private var _getHighestPreBidResponse = MutableLiveData<Resource<io.bidswipe.app.network.response.PreBidHighestResponse>>()
	val getHighestPreBidRepo: MutableLiveData<Resource<io.bidswipe.app.network.response.PreBidHighestResponse>>
		get() = _getHighestPreBidResponse

	fun getHighestPreBid(productId: Int) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getHighestPreBidResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getHighestPreBidResponse.value = repo.getHighestPreBid(productId)
	}

	// Basecamp #9943368953 (2026-05-29): live-show chat history via REST
	private var _chatHistoryResponse = MutableLiveData<Resource<io.bidswipe.app.network.response.ChatHistoryResponse>>()
	val chatHistoryRepo: MutableLiveData<Resource<io.bidswipe.app.network.response.ChatHistoryResponse>>
		get() = _chatHistoryResponse

	fun getChatHistory(roomId: String) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_chatHistoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_chatHistoryResponse.value = repo.getChatHistory(roomId)
	}
}