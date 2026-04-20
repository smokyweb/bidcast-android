package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShowItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.utils.asHtml
import io.bidswipe.app.utils.setHapticClickListener

class ShowAdapter(
	var mList: MutableList<GetPrepareStepResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetPrepareStepResponse.Data?, ShowItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) = ShowItemBinding.inflate(inflater, parent, false)

	override fun onBind(holder: BaseViewHolder<ShowItemBinding>, position: Int, item: GetPrepareStepResponse.Data?) {
		with(holder) {
			val isStepEnabled = position == 0 || (0 until position).all { index ->
				mList[index]?.status == "completed"
			}

			bind.setSchedule.setHapticClickListener {
				if (isStepEnabled) {
					mClicks.itemClick(position, "schedule")
				}
			}

			bind.step.text = buildString {
				append(position + 1)
			}

			when (item?.status) {
				"unlocked" -> {
					bind.icon.isVisible = true
					bind.step.isVisible = false
					bind.setSchedule.visibility = View.VISIBLE
					bind.setSchedule.text = mCtx.getString(R.string._continue)
					bind.setSchedule.isEnabled = true
					bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_lock))
					bind.iconCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
				}

				"completed" -> {
					bind.icon.isVisible = true
					bind.step.isVisible = false
					bind.setSchedule.visibility = View.GONE
					bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_tick))
					bind.iconCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.success))
				}

				else -> {
					bind.icon.isVisible = false
					bind.step.isVisible = true
					bind.setSchedule.visibility = View.VISIBLE
					bind.setSchedule.text = mCtx.getString(R.string._continue)
					bind.setSchedule.isEnabled = true
				}
			}

			if (!isStepEnabled && item?.status != "completed") {
				bind.icon.isVisible = true
				bind.step.isVisible = false
				bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, R.drawable.ic_lock))
				bind.iconCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
				bind.setSchedule.visibility = View.GONE
				bind.setSchedule.isEnabled = false
			}

			bind.subTitle.text = ((item?.description ?: "").trim()).asHtml()
			bind.title.text = item?.title
		}
	}
}