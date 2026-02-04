package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SurpriseSetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.setHapticClickListener

class SurpriseProductAdapter(
    val from: String = "",
    val mList: MutableList<GetSurpriseProductsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetSurpriseProductsResponse.Data?, SurpriseSetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SurpriseSetItemBinding.inflate(inflater, parent, false)

    @SuppressLint("SetTextI18n")
    override fun onBind(
        holder: BaseViewHolder<SurpriseSetItemBinding>,
        position: Int,
        item: GetSurpriseProductsResponse.Data?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                if (from == "freebie") {
                    mClicks.itemClick(position, "freebie")
                } else {
                    mClicks.itemClick(position, "select")
                }
            }

            bind.manage.setHapticClickListener {
                mClicks.itemClick(position, "manage")
            }

            bind.setForNext.setHapticClickListener {
                mClicks.itemClick(position, "set_next")
            }

            bind.buttonLayout.isVisible = from != "freebie"

            bind.desc.text = buildString {
                append(item?.description)
            }

            bind.productName.text = item?.name?.asCapital()

            bind.price.isVisible = item?.type == "buy_it_now"
            bind.price.text = buildString {
                append(item?.price?.toString()?.asMoney())
            }

            item?.totalQuantity=item.items?.sumOf { it?.quantity?:0 }
            bind.stepProgress.max = item?.totalQuantity ?: 0
            bind.stepProgress.progress = (item?.soldQuantity ?: 0)
            bind.itemsLeftText.text = buildString {
                append((item?.totalQuantity ?: 0) - (item?.soldQuantity ?: 0))
                append("/")
                append(item?.totalQuantity?:0)
                append(" left")
            }
        }
    }
}
