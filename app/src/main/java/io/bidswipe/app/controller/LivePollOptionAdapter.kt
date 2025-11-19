package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PollOptionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PollModel
import io.bidswipe.app.utils.setHapticClickListener

class LivePollOptionAdapter(
	val mList : MutableList<PollModel.PollOption>,
	val mClicks : RecyclerClicks,
	val canVote: Boolean = true
) : BaseAdapter<PollModel.PollOption , PollOptionItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		PollOptionItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<PollOptionItemBinding> ,
		position : Int ,
		item : PollModel.PollOption? ,
	) {
		with(holder) {
			item?.let { option ->
				bind.optionText.text = option.text
				bind.optionPercent.text = "${option.percentage}%"
				bind.optionProgress.progress = option.percentage
				bind.optionCount.text = "${option.voteCount} ${if (option.voteCount == 1) "vote" else "votes"}"

				/*// Show selected icon if user voted for this option
				bind.selectedIcon.isVisible = option.isSelected

				// Make clickable only if user hasn't voted and voting is allowed
				bind.root.isClickable = canVote && !option.isSelected
				bind.root.isFocusable = canVote && !option.isSelected
*/
				bind.option.setHapticClickListener {
						mClicks.itemClick(position)
				}
			}
		}
	}

}