package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R

class ColorSwatchAdapter(
    private val colors: List<String>,
    private var selectedColor: String,
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<ColorSwatchAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val swatch: View = view.findViewById(R.id.vColorSwatch)
        val checkMark: ImageView = view.findViewById(R.id.ivColorCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_color_swatch, parent, false))

    override fun getItemCount() = colors.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val hex = colors[position]
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        try {
            shape.setColor(Color.parseColor(hex))
        } catch (_: IllegalArgumentException) {
            shape.setColor(Color.GRAY)
        }
        holder.swatch.background = shape
        holder.checkMark.visibility = if (hex == selectedColor) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener {
            selectedColor = hex
            onPick(hex)
            notifyDataSetChanged()
        }
    }

    fun setSelected(color: String) {
        selectedColor = color
        notifyDataSetChanged()
    }
}
