package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TipsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetTipAmountResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class TipsAdapter(
	mList : MutableList<GetTipAmountResponse.Data.Tip?> , val mClicks : RecyclerClicks
) : BaseAdapter< GetTipAmountResponse.Data.Tip , TipsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TipsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder: BaseViewHolder<TipsItemBinding>,
		position: Int,
		item: GetTipAmountResponse.Data.Tip?
	) {
		with(holder) {

			bind.userName.text = item?.user?.name
//			bind.date.text = item?.createdAt

			bind.date.text = buildString {
				append(	Utils.getFormattedDateTime(
					Const.SERVER_TIME_FORMAT ,
					"MM/dd/yyyy",
					item?.createdAt.toString()
				))
				append(Const.BULLET)
				append(Utils.getFormattedDateTime(
					Const.SERVER_TIME_FORMAT ,
					"hh:mm a",
					item?.createdAt.toString()
				))
			}

			bind.amount.text = item?.total.toString().asMoney()

			bind.icon.loadUrl(mCtx, item?.user?.profileImage ?:"")

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
		}

	}
}