package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.zerobranch.layout.SwipeLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class InventoryAdapter(
	mList: MutableList<GetMyInventoryResponse.Data?>,
	private val isSelectionMode: Boolean,
	val mClicks: RecyclerClicks,
) : BaseAdapter<GetMyInventoryResponse.Data?, InventoryItemBinding>(mList) {

	val posList = mutableListOf<Int>()

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		InventoryItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<InventoryItemBinding>,
		position: Int,
		item: GetMyInventoryResponse.Data?,
	) {
		with(holder) {

			bind.statusCard.isVisible = item?.status == "inactive"
			bind.bidsCount.isVisible = item?.status == "inactive"

			bind.productName.text = item?.title?.asCapital()
			bind.prodSubTitle.text = item?.category?.name

			if (item?.images?.isNotEmpty() == true){
				bind.productImage.loadUrl(mCtx, item.images.get(0)?:"", placeHolder = R.drawable.placeholder_rect)
			}

			bind.price.text = (item?.pricing ?: "0").asMoney()
			bind.stockCount.text =buildString {
				append("Stock: ")
				append(item?.quantity ?:0)
			}

			bind.swipeLayout.close()

			bind.swipeLayout.setOnActionsListener(object : SwipeLayout.SwipeActionsListener {
				override fun onOpen(direction: Int, isContinuous: Boolean) {
					if (posList.isNotEmpty()) {
						val posi = posList.first()
						posList.clear()
						notifyItemChanged(posi)
					}
					posList.add(position)
				}

				override fun onClose() {
					posList.remove(position)
				}

			})

			if (isSelectionMode) {
				if (item?.selected == true) {
					bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.secondaryContainer))
					bind.root.strokeWidth = 2
					bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
				} else {
					bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.surface))
					bind.root.strokeWidth = 0
				}

				bind.click.setHapticClickListener {
					mClicks.itemClick(position, "toggle")
				}
			} else {
//				bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, R.color.surface))
				bind.root.strokeWidth = 0
				bind.click.setHapticClickListener {
					mClicks.itemClick(position)
				}

				bind.deleteNotification.setHapticClickListener {
					mClicks.itemClick(position, "delete")
				}
			}

			bind.click.setOnLongClickListener {
				mClicks.itemClick(position, "longClick")
				true
			}

		}
	}

}