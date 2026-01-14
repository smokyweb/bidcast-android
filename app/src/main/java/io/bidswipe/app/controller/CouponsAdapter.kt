package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CouponsListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCouponsResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.clr

class CouponsAdapter(
    mList: MutableList<GetCouponsResponse.Data?>,
    val mClicks: RecyclerClicks,
    val from: String? = ""
) : BaseAdapter<GetCouponsResponse.Data?, CouponsListItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        CouponsListItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<CouponsListItemBinding>,
        position: Int,
        item: GetCouponsResponse.Data?,
    ) {
        with(holder) {
            if (from == "buy_now") {
                bind.apply.isVisible = true
                bind.root.setOnClickListener {
                    mClicks.itemClick(position, item?.coupon?.id.toString())
                }

                if (item?.isSelected == true) {
                    bind.apply.text = "Applied"
                    bind.apply.setTextColor(ContextCompat.getColor(mCtx, clr.success))
                } else {
                    bind.apply.text = "Apply"
                    bind.apply.setTextColor(ContextCompat.getColor(mCtx, clr.primary))
                }
            }

            bind.couponCode.text = item?.coupon?.name ?: "N/A"
            bind.percentage.text = if (item?.coupon?.type == "percentage") {
                (item.coupon.value ?: "0").toString() + "% \nOFF"
            } else {
                (item?.coupon?.value ?: "0").toString().asMoney() + " \nOFF"
            }

            bind.desc.text = item?.coupon?.description ?: "N/A"
            bind.minAmount.text = buildSpannedString {
                bold {
                    append("Min Amount: ")
                }
                append((item?.coupon?.minAmount ?: 0).toString().asMoney())
            }

            val time = Utils.getTimeStampFromServerTime(item?.coupon?.expDate ?: "")
            bind.validUpto.text = buildSpannedString {
                bold {
                    append("Valid till: ")
                }
                append(
                    Utils.getTimeFromServerTimestamp(
                        time,
                        Const.MMM_dd_yyyy_HH_mm,
                    )
                )
            }

        }
    }
}