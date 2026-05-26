package io.bidswipe.app.network.request

import com.google.gson.annotations.SerializedName

data class RandomizerTemplateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("entry_cost") val entryCost: Double? = null,
    @SerializedName("slot_count") val slotCount: Int,
    @SerializedName("slots") val slots: List<RandomizerSlotRequest>
)

data class RandomizerSlotRequest(
    @SerializedName("position") val position: Int,
    @SerializedName("color") val color: String,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("product_id") val productId: Int? = null
)

data class AttachTemplateRequest(
    @SerializedName("template_id") val templateId: Int
)
