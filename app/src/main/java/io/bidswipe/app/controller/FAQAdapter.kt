package io.bidswipe.app.controller

import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.FaqItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

class FAQAdapter(mList : MutableList<FAQResponse.Data?> , private val mClicks : RecyclerClicks) :
	BaseAdapter<FAQResponse.Data? , FaqItemBinding>(mList) {

	private var selectedPosition = - 1

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		FaqItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<FaqItemBinding> ,
		position : Int ,
		item : FAQResponse.Data? ,
	) {

		runSafe {
			with(holder) {

				bind.question.text = item?.question
				bind.answer.text = Html.fromHtml(item?.answer)

                bind.root.setHapticClickListener {
					mClicks.itemClick(position)
				}

				if (item?.selected == true) {
					if (selectedPosition == position) {
						selectedPosition = - 1
						bind.expandView.collapse()
						bind.answerLayout.isVisible = false
					} else {
						bind.answerLayout.isVisible = true
						bind.expandView.expand()
						selectedPosition = position
					}

				} else {
					if (bind.expandView.isExpanded) {
						bind.expandView.collapse()
						bind.answerLayout.isVisible = false
					}
				}

				bind.expandView.setOnExpansionUpdateListener { expantionFraction , state ->
					if (item?.selected == true) {
						bind.view.rotation = expantionFraction * 90F
					} else {
						bind.view.rotation = expantionFraction * 0F
					}
				}
			}
		}

	}
}