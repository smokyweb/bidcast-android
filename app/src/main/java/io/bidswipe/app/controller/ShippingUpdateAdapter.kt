package io.bidswipe.app.controller

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShippingUpdateItemBinding
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital

class ShippingUpdateAdapter(
    val mList: MutableList<GetOrderDetailsResponse.Data.ShippingTracking?>,
) : BaseAdapter<GetOrderDetailsResponse.Data.ShippingTracking, ShippingUpdateItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingUpdateItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingUpdateItemBinding>,
        position: Int,
        item: GetOrderDetailsResponse.Data.ShippingTracking?,
    ) {
        with(holder) {

            bind.title.text = item?.title?.asCapital()

            val time = Utils.getTimeStampFromServerTime(item?.createdAt.toString())
            bind.subTitle.text =  Utils.getTimeFromServerTimestamp(
                    time,
            Const.MMM_dd_yyyy_HH_mm,
            )

            if (position == 0) {
                bind.view1.visibility = View.INVISIBLE
                bind.view2.visibility = View.INVISIBLE
            }

            if (position == mList.lastIndex) {
                bind.view.visibility = View.INVISIBLE
                bind.view2.visibility = View.INVISIBLE

                if (item?.title?.lowercase() != "delivered") {
                    bind.view2.isVisible = true
                    bind.icon.setImageResource(R.drawable.stepper_active_item)

                    val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                        bind.view2,
                        PropertyValuesHolder.ofFloat("scaleX", 0.5f),
                        PropertyValuesHolder.ofFloat("scaleY", 0.5f)
                    )
                    scaleDown.duration = 1000
                    scaleDown.repeatMode = ValueAnimator.REVERSE
                    scaleDown.repeatCount = ValueAnimator.INFINITE
                    scaleDown.start()
                }
            } else {
                bind.view.visibility = View.VISIBLE
                bind.view2.visibility = View.INVISIBLE
            }

        }
    }


}