package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R

class IconPickerAdapter(
    private val icons: List<String>,
    private var selectedIcon: String?,
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIconEmoji)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_icon_picker, parent, false))

    override fun getItemCount() = icons.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val emoji = icons[position]
        holder.tvIcon.text = emoji
        val bg = if (emoji == selectedIcon)
            R.drawable.card_8
        else
            R.drawable.card_8
        holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, bg)
        holder.itemView.alpha = if (emoji == selectedIcon) 1f else 0.5f
        holder.itemView.setOnClickListener {
            selectedIcon = emoji
            onPick(emoji)
            notifyDataSetChanged()
        }
    }

    fun setSelected(icon: String?) {
        selectedIcon = icon
        notifyDataSetChanged()
    }
}
