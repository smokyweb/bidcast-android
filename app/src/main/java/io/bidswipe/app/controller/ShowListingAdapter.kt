package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShowListingItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ShowListingAdapter(
	val mList: MutableList<GetMyShowResponse.Data?>, val mClick: RecyclerClicks,
) : BaseAdapter<GetMyShowResponse.Data?, ShowListingItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ShowListingItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ShowListingItemBinding>,
		position: Int,
		item: GetMyShowResponse.Data?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClick.itemClick(position, "click")
			}

			bind.name.text = item?.title?.asCapital()

			bind.category.text = item?.category?.name?.asCapital()

			bind.time.text = buildString {
				append(
					Utils.getFormattedDateTime(
						"yyyy-mm-dd",
						"mm-dd-yyyy",
						item?.date.toString()
					)
				)
				append(" ")
				append(Const.BULLET)
				append(" ")
				append(
					Utils.getFormattedDateTime(
						"HH:mm:ss",
						"hh:mm a",
						item?.time.toString()
					)
				)
			}

			bind.sales.text = buildString {
				append((item?.totalSalesAmount ?: 0).toString().asMoney())
				append(" sales ")
				append(Const.BULLET + " ")
				append(item?.totalOrders ?: 0)
				append(" orders")
			}

			bind.image.loadUrl(mCtx, item?.imgThumbnail?.first() ?: "")

		}
	}

}