package io.bidswipe.app.controller

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PromoteItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class PromoteSheetAdapter(
    mList: MutableList<String?>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, PromoteItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PromoteItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PromoteItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }


            val startColor = ContextCompat.getColor(mCtx, R.color.primary)
            val endColor = ContextCompat.getColor(mCtx, R.color.secondary)

            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,  // or TOP_BOTTOM, BL_TR etc
                intArrayOf(startColor, endColor)
            )

            gradientDrawable.cornerRadius = 16f  // optional rounding

            // Apply as background to the CardView (or any view you want)
            bind.root.background = gradientDrawable

        }
    }
}