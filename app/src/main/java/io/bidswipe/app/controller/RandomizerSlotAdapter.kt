package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.ui.randomizer.SlotDraft
import io.bidswipe.app.utils.loadUrl

class RandomizerSlotAdapter(
    private var items: MutableList<SlotDraft>,
    private val onSlotTap: (Int) -> Unit  // position index
) : RecyclerView.Adapter<RandomizerSlotAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardSlot)
        val tvPos: TextView = view.findViewById(R.id.tvSlotPosition)
        val tvIcon: TextView = view.findViewById(R.id.tvSlotIcon)
        val tvProduct: TextView = view.findViewById(R.id.tvSlotProduct)
        val ivSlotImage: ImageView = view.findViewById(R.id.ivSlotImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_randomizer_slot, parent, false))

    override fun getItemCount() = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val slot = items[position]
        try {
            holder.card.setCardBackgroundColor(Color.parseColor(slot.color))
        } catch (_: IllegalArgumentException) {
            holder.card.setCardBackgroundColor(Color.parseColor("#339AF0"))
        }
        holder.tvPos.text = "#${slot.position + 1}"
        if (!slot.imageUrl.isNullOrEmpty()) {
            holder.ivSlotImage.visibility = View.VISIBLE
            holder.tvIcon.visibility = View.INVISIBLE
            holder.ivSlotImage.loadUrl(holder.itemView.context, slot.imageUrl!!)
        } else {
            holder.ivSlotImage.visibility = View.GONE
            holder.tvIcon.visibility = View.VISIBLE
            holder.tvIcon.text = slot.icon ?: ""
        }
        holder.tvProduct.text = when {
            slot.productTitle != null -> slot.productTitle
            else                     -> "Tap to configure"
        }
        holder.itemView.setOnClickListener { onSlotTap(position) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newItems: List<SlotDraft>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun notifySlotChanged(index: Int) {
        if (index in 0 until items.size) notifyItemChanged(index)
    }
}
