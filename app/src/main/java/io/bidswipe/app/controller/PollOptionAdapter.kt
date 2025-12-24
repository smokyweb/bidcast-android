package io.bidswipe.app.controller

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.VariantItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PollOptionModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class PollOptionAdapter (
	val mList : MutableList<PollOptionModel?>,
	val mClicks : RecyclerClicks,
) : BaseAdapter<PollOptionModel , VariantItemBinding>(mList) {

	var holderList = mutableListOf<BaseViewHolder<VariantItemBinding>>()

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		VariantItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<VariantItemBinding> ,
		position : Int ,
		item : PollOptionModel? ,
	) {
		holderList.add(holder)
		with(holder) {
			bind.title.text = item?.title?.asCapital()
			bind.quantityBox.isVisible = true
			bind.quantity.inputType = InputType.TYPE_CLASS_TEXT
			bind.radioGroup.isVisible = false
			bind.quantity.setHint(item?.hint)
			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
		}
	}

	fun getAllVariantData() : List<String> {
		val result = mutableListOf<String>()

		for (i in 0 until itemCount) {
			val viewHolder = holderList[i]
			val variant = mList[i]

			viewHolder.let { holder ->
				variant?.let {
					val value = holder.bind.quantity.value()
					if (value.isNotEmpty()){
						result.add(value)
					}
				}
			}
		}
		return result

	}


}