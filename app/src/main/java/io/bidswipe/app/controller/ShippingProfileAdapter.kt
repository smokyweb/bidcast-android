package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingProfileItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetShippingProfilesResponse

class ShippingProfileAdapter(
    mList: MutableList<GetShippingProfilesResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetShippingProfilesResponse.Data?, ShippingProfileItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingProfileItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingProfileItemBinding>,
        position: Int,
        item: GetShippingProfilesResponse.Data?,
    ) {
        with(holder) {
            bind.itemName.text = item?.name
            bind.itemWeight.text = buildSpannedString {
                bold { append("Weight: ")}
                append(item?.weight)
            }

            bind.itemScale.text = buildSpannedString {
               bold { append("Scale: ") }
                append(item?.size)
            }

            bind.bundlingOptionYesNo.text = "No"
            bind.additionalWeightYesNo.text = "No"
        }
    }
}


