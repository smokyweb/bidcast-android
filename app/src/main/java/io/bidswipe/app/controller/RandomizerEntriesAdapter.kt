package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.EntriesItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetFreebieObject
import io.bidswipe.app.utils.setHapticClickListener

class RandomizerEntriesAdapter(
	val mList: MutableList<GetFreebieObject.Users?>,
	val mClicks: RecyclerClicks,
) : BaseAdapter<GetFreebieObject.Users?, EntriesItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		EntriesItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<EntriesItemBinding>,
		position: Int,
		item: GetFreebieObject.Users?,
	) {
		with(holder) {

			bind.name.text = item?.userName

			bind.endIcon.setHapticClickListener {

				mClicks.itemClick(position)

			}

//			bind.icon.setImageResource(item?.icon?: draw.ic_document)

		}
	}
}