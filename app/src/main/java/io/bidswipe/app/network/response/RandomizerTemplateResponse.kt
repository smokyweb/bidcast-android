package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName

// ─── Single template (returned in list + detail) ───────────────────────────

data class RandomizerTemplate(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("entry_cost") val entryCost: String?,
    @SerializedName("slot_count") val slotCount: Int?,
    @SerializedName("slots") val slots: List<RandomizerSlot>?
) {
    /** Human-readable label for the 4 type values */
    fun typeLabel(): String = when (type) {
        "product_raffle"       -> "Product Raffle"
        "blind_product_raffle" -> "Blind Product Raffle"
        "buyer_raffle"         -> "Buyer Raffle"
        "wheel_bin_auction"    -> "Wheel BIN / Auction"
        else                   -> type ?: "Unknown"
    }
}

data class RandomizerSlot(
    @SerializedName("id") val id: Int?,
    @SerializedName("position") val position: Int?,
    @SerializedName("color") val color: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("product_id") val productId: Int?,
    @SerializedName("product") val product: SlotProduct?
)

data class SlotProduct(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("quantity") val quantity: Int?
)

// ─── List response ─────────────────────────────────────────────────────────

data class RandomizerTemplateListResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: List<RandomizerTemplate>?
)

// ─── Single-template response (create / get / update) ─────────────────────

data class RandomizerTemplateSingleResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: RandomizerTemplate?,
    @SerializedName("message") val message: String?
)
