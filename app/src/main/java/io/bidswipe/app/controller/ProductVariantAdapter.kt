package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.VariantItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.utils.asCapital

class ProductVariantAdapter(
	val mList: MutableList<GetCategoryResponse.Data.ExtraField?>,
	val mClicks: RecyclerClicks,
) : BaseAdapter<GetCategoryResponse.Data.ExtraField, VariantItemBinding>(mList) {

	var holderList = mutableListOf<BaseViewHolder<VariantItemBinding>>()
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		VariantItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<VariantItemBinding>,
		position: Int,
		item: GetCategoryResponse.Data.ExtraField?,
	) {
		holderList.add(holder)
		with(holder) {
			bind.title.text = item?.label?.asCapital()
			if (item?.type == "text"){
				bind.quantityBox.isVisible = true
				bind.radioGroup.isVisible = false
				bind.quantity.setHint("Enter ${item.label}")
			}else if(item?.type == "radio"){
				bind.radioGroup.isVisible = true
				bind.quantityBox.isVisible = false
				bind.radioGroup.removeAllViews()
				item.options?.forEach { option ->
					val radioButton = RadioButton(mCtx).apply {
						id = option.hashCode()
						text = option
					}
					bind.radioGroup.addView(radioButton)
				}
			}
			else{
				bind.quantityBox.isVisible = false
				bind.radioGroup.isVisible = false
			}

			bind.root.setOnClickListener {

				mClicks.itemClick(position)

			}

		}
	}

	fun getAllVariantData(): List<Pair<String, String>> {
		val result = mutableListOf<Pair<String, String>>()

		for (i in 0 until itemCount) {
			val viewHolder = holderList[i]
			val variant = mList[i]

			viewHolder?.let { holder ->
				variant?.let {
					val value = when (it.type) {
						"text" -> holder.bind.quantity.text?.toString() ?: ""
						"radio" -> {
							val selectedId = holder.bind.radioGroup.checkedRadioButtonId
							if (selectedId != -1) {
								holder.bind.radioGroup.findViewById<RadioButton>(selectedId).text.toString()
							} else ""
						}
						else -> ""
					}
					result.add(Pair(it.label ?: "", value))
				}
			}
		}
		return result
	}

}