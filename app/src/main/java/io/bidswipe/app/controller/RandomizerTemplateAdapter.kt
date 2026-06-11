package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import io.bidswipe.app.R
import io.bidswipe.app.network.response.RandomizerTemplate

class RandomizerTemplateAdapter(
    private var items: MutableList<RandomizerTemplate> = mutableListOf(),
    private val onEdit: (RandomizerTemplate) -> Unit,
    private val onDelete: (RandomizerTemplate) -> Unit,
    private val onRelease: (RandomizerTemplate) -> Unit,
    private val onSelect: ((RandomizerTemplate) -> Unit)? = null
) : RecyclerView.Adapter<RandomizerTemplateAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvTemplateName)
        val typeChip: Chip = view.findViewById(R.id.chipTemplateType)
        val slotCount: TextView = view.findViewById(R.id.tvSlotCount)
        val btnEdit: ImageView = view.findViewById(R.id.btnEditTemplate)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteTemplate)
        val btnRelease: ImageView = view.findViewById(R.id.btnReleaseProducts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_randomizer_template, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name ?: "Untitled"
        holder.typeChip.text = item.typeLabel()
        holder.slotCount.text = "${item.slotCount ?: 0} slots"
        // Basecamp #9960173707 (2026-06-11): always resolve the item at
        // click-time via bindingAdapterPosition so that a RecyclerView rebind
        // triggered between bind and click can never deliver a stale item —
        // the classic "first Edit tap opens Create screen" symptom.
        holder.btnEdit.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onEdit(items[pos])
        }
        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(items[pos])
        }
        holder.btnRelease.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRelease(items[pos])
        }
        holder.itemView.isClickable = onSelect != null
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onSelect?.invoke(items[pos])
        }
        holder.btnDelete.visibility = if (onSelect == null) View.VISIBLE else View.GONE
        holder.btnRelease.visibility = if (onSelect == null) View.VISIBLE else View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newItems: List<RandomizerTemplate>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeById(id: Int?) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
