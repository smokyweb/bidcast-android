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
    private val onRelease: (RandomizerTemplate) -> Unit
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
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.btnRelease.setOnClickListener { onRelease(item) }
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
