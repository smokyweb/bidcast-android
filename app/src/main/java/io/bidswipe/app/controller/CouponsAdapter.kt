package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CouponsListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCouponsResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney

class CouponsAdapter(
    mList: MutableList<GetCouponsResponse.Data?>,
    val mClicks: RecyclerClicks
) : BaseAdapter<GetCouponsResponse.Data?, CouponsListItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        CouponsListItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<CouponsListItemBinding>,
        position: Int,
        item: GetCouponsResponse.Data?,
    ) {
        with(holder) {

            bind.couponCode.text = item?.coupon?.name ?: "N/A"
            bind.percentage.text = if (item?.coupon?.type == "percentage") {
                (item?.coupon?.value ?: "0").toString() + "% \nOFF"
            } else {
                (item?.coupon?.value ?: "0").toString().asMoney() + " \nOFF"
            }

            bind.desc.text = item?.coupon?.description ?: "N/A"

            val time = Utils.getTimeStampFromServerTime(item?.coupon?.expDate ?: "")
            bind.validUpto.text = Utils.getTimeFromServerTimestamp(
                time,
                Const.MMM_dd_yyyy_HH_mm,
            )

        }
    }
}