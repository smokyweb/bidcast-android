package io.bidswipe.app.ui.randomizer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.request.RandomizerSlotRequest
import io.bidswipe.app.network.request.RandomizerTemplateRequest
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.RandomizerTemplate
import io.bidswipe.app.network.response.RandomizerTemplateListResponse
import io.bidswipe.app.network.response.RandomizerTemplateSingleResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RandomizerViewModel @Inject constructor(
    val repo: DashRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    // ── List ──────────────────────────────────────────────────────────────────

    private val _listResponse = MutableLiveData<Resource<RandomizerTemplateListResponse>>()
    val listResponse: MutableLiveData<Resource<RandomizerTemplateListResponse>> get() = _listResponse

    fun loadTemplates() = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _listResponse.value = NO_INTERNET_ERROR; return@launch }
        _listResponse.value = repo.getRandomizerTemplates()
    }

    // ── Single ────────────────────────────────────────────────────────────────

    private val _singleResponse = MutableLiveData<Resource<RandomizerTemplateSingleResponse>>()
    val singleResponse: MutableLiveData<Resource<RandomizerTemplateSingleResponse>> get() = _singleResponse

    fun loadTemplate(id: Int) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _singleResponse.value = NO_INTERNET_ERROR; return@launch }
        _singleResponse.value = repo.getRandomizerTemplate(id)
    }

    // ── Create ────────────────────────────────────────────────────────────────

    private val _createResponse = MutableLiveData<Resource<RandomizerTemplateSingleResponse>>()
    val createResponse: MutableLiveData<Resource<RandomizerTemplateSingleResponse>> get() = _createResponse

    fun createTemplate(body: RandomizerTemplateRequest) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _createResponse.value = NO_INTERNET_ERROR; return@launch }
        _createResponse.value = repo.createRandomizerTemplate(body)
    }

    // ── Update ────────────────────────────────────────────────────────────────

    private val _updateResponse = MutableLiveData<Resource<RandomizerTemplateSingleResponse>>()
    val updateResponse: MutableLiveData<Resource<RandomizerTemplateSingleResponse>> get() = _updateResponse

    fun updateTemplate(id: Int, body: RandomizerTemplateRequest) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _updateResponse.value = NO_INTERNET_ERROR; return@launch }
        _updateResponse.value = repo.updateRandomizerTemplate(id, body)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private val _deleteResponse = MutableLiveData<Resource<CommonResponse>>()
    val deleteResponse: MutableLiveData<Resource<CommonResponse>> get() = _deleteResponse

    fun deleteTemplate(id: Int) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _deleteResponse.value = NO_INTERNET_ERROR; return@launch }
        _deleteResponse.value = repo.deleteRandomizerTemplate(id)
    }

    // ── Release products ──────────────────────────────────────────────────────

    private val _releaseResponse = MutableLiveData<Resource<CommonResponse>>()
    val releaseResponse: MutableLiveData<Resource<CommonResponse>> get() = _releaseResponse

    fun releaseProducts(id: Int) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _releaseResponse.value = NO_INTERNET_ERROR; return@launch }
        _releaseResponse.value = repo.releaseTemplateProducts(id)
    }

    // ── Duplicate ─────────────────────────────────────────────────────────────

    private val _duplicateResponse = MutableLiveData<Resource<RandomizerTemplateSingleResponse>>()
    val duplicateResponse: MutableLiveData<Resource<RandomizerTemplateSingleResponse>> get() = _duplicateResponse

    fun duplicateTemplate(id: Int) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _duplicateResponse.value = NO_INTERNET_ERROR; return@launch }
        _duplicateResponse.value = repo.duplicateTemplate(id)
    }

    // ── Show attach/detach ────────────────────────────────────────────────────

    private val _attachResponse = MutableLiveData<Resource<CommonResponse>>()
    val attachResponse: MutableLiveData<Resource<CommonResponse>> get() = _attachResponse

    fun attachToShow(showId: String, templateId: Int) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _attachResponse.value = NO_INTERNET_ERROR; return@launch }
        _attachResponse.value = repo.attachRandomizerTemplate(showId, templateId)
    }

    fun detachFromShow(showId: String) = viewModelScope.launch {
        if (!networkMonitor.hasInternet()) { _attachResponse.value = NO_INTERNET_ERROR; return@launch }
        _attachResponse.value = repo.detachRandomizerTemplate(showId)
    }

    // ── Builder state (held while user edits) ─────────────────────────────────

    /** The template being edited. null = creating new. */
    var editingTemplate: RandomizerTemplate? = null

    /** Mutable working copy of slots for the builder UI */
    var builderSlots: MutableList<SlotDraft> = mutableListOf()

    fun initBuilderSlots(count: Int, existing: List<SlotDraft> = emptyList()) {
        builderSlots = MutableList(count) { i ->
            existing.getOrNull(i) ?: SlotDraft(
                position = i,
                color = PALETTE_COLORS[i % PALETTE_COLORS.size],
                icon = PALETTE_ICONS[i % PALETTE_ICONS.size]
            )
        }
    }

    fun toSlotRequests(): List<RandomizerSlotRequest> = builderSlots.map {
        RandomizerSlotRequest(
            position = it.position,
            color = it.color,
            icon = it.icon,
            productId = it.productId
        )
    }

    companion object {
        val PALETTE_COLORS = listOf(
            "#FF6B6B", "#FFA94D", "#FFD43B", "#82C91E",
            "#51CF66", "#20C997", "#22B8CF", "#339AF0",
            "#5C7CFA", "#845EF7", "#CC5DE8", "#F06595"
        )
        val PALETTE_ICONS = listOf(
            "🎁", "🎉", "💰", "🏆", "⭐", "🎯",
            "💎", "🎪", "🍀", "🔥", "⚡", "👑"
        )
        val TYPE_LABELS = listOf(
            "Product Raffle",
            "Blind Product Raffle",
            "Buyer Raffle",
            "Wheel BIN / Auction"
        )
        val TYPE_VALUES = listOf(
            "product_raffle",
            "blind_product_raffle",
            "buyer_raffle",
            "wheel_bin_auction"
        )
    }
}

/** Mutable slot draft held in the builder while user edits */
data class SlotDraft(
    val position: Int,
    var color: String,
    var icon: String?,
    var productId: Int? = null,
    var productTitle: String? = null,
    var productThumbnail: String? = null
)
