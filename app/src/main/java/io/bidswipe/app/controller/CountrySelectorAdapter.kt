package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.RememberListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.CountryModel
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

class CountrySelectorAdapter(
    mList: MutableList<CountryModel?>,
    private val mClicks: RecyclerClicks
) :
    BaseAdapter<CountryModel?, RememberListItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        RememberListItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<RememberListItemBinding>,
        position: Int,
        item: CountryModel?,
    ) {
        runSafe {
            with(holder) {

                bind.root.setHapticClickListener {
                    mClicks.itemClick(position)
                }

                bind.root.text = buildSpannedString {
                    bold { append(item?.countryName) }
                }

                if (item?.selected == true) {
                    bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.primary))
                    bind.root.setTextColor(ContextCompat.getColor(mCtx, R.color.surface))
                } else {
                    bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.surface))
                    bind.root.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
                }

            }
        }
    }

}