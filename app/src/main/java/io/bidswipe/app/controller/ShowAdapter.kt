package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShowItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ShowModel
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.utils.dpToPx

class ShowAdapter(
    mList: MutableList<GetPrepareStepResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetPrepareStepResponse.Data?, ShowItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShowItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShowItemBinding>,
        position: Int,
        item: GetPrepareStepResponse.Data?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.setSchedule.setOnClickListener {
                mClicks.itemClick(position,"schedule")
            }
            
            bind.step.text = "${position + 1}"

            if (item?.selected == true) {
                bind.root.strokeWidth=mCtx.resources.dpToPx(2)
                bind.iconCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        mCtx,
                        R.color.secondary
                    )
                )

            } else {
                bind.root.strokeWidth=mCtx.resources.dpToPx(0)
            }

            if(item?.status=="unlocked"){
                bind.icon.isVisible = true
                bind.step.isVisible = false
                bind.setSchedule.isVisible = false
                bind.icon.setImageDrawable(
                    ContextCompat.getDrawable(
                        mCtx,
                         R.drawable.ic_lock
                    )
                )

                bind.iconCard.setCardBackgroundColor(
                    ContextCompat.getColor(
                        mCtx,
                        R.color.outlineVariant
                    )
                )
            }else{
                bind.icon.isVisible = false
                bind.step.isVisible = true
            }

            bind.subTitle.setHtmlFromString(item?.description ?: "",false)
            bind.title.text = item?.title

        }
    }
}