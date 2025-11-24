package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SortingSelectionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.utils.setHapticClickListener

class SortingOptionAdapter (
	mList: MutableList<LiveMoreOption?>, val mClicks: RecyclerClicks,
) : BaseAdapter<LiveMoreOption?, SortingSelectionItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		SortingSelectionItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<SortingSelectionItemBinding>,
		position: Int,
		item: LiveMoreOption?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.address.text = buildString {
				append(item?.name)
			}

			bind.selectBtn.isChecked = item?.isSelected == true

		}
	}
}