package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ReferralBenefitsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.draw

class ReferralBenefitsAdapter(
    mList: MutableList<BenefitItem>
) : BaseAdapter<BenefitItem?, ReferralBenefitsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ReferralBenefitsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ReferralBenefitsItemBinding>,
        position: Int,
        item: BenefitItem?
    ) {
        with(holder) {
            bind.icon.setImageResource(item?.iconRes?: draw.ic_people)
            bind.content.text = buildSpannedString {
                bold {
                    append(item?.title)
                    append(": ")
            }
            append(item?.description)}
        }
    }
}


data class BenefitItem(
    val iconRes: Int,
    val title: String,
    val description: String
)
