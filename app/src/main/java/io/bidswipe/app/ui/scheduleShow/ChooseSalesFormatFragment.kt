package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FormatAdapter
import io.bidswipe.app.databinding.FragmentChooseSalesFormatBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.FormatModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged")
class ChooseSalesFormatFragment : BaseFragment<ScheduleShowViewModel, FragmentChooseSalesFormatBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentChooseSalesFormatBinding.inflate(inflater, view, false)

	private var formatList = mutableListOf<FormatModel>()
	private lateinit var adapter: FormatAdapter

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos: Int, status: String?) {

			formatList.forEachIndexed { index, formatModel ->
				formatModel.selected = index == pos
			}

			bind.offerLayout.isVisible = pos == 1
			viewModel.productSalesFormat = formatList[pos].title ?: ""

			adapter.notifyDataSetChanged()

		}
	}

	override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		formatList.clear()
		formatList.add(FormatModel(draw.ic_hammer, "Auction"))
		formatList.add(FormatModel(draw.ic_tag, "Buy It Now"))

		adapter = FormatAdapter(formatList, mClick)

		preselectFormat()
		bind.recycler.adapter = adapter
		bind.bidPrice.setText(viewModel.productPrice)

		bind.continueBtn.setHapticClickListener {
			val selectedFormat = formatList.firstOrNull { it.selected == true }?.title ?: ""

			if (selectedFormat.isEmpty()) {
				Alerts.error(mCtx, "Please select a format")
				return@setHapticClickListener
			}

			if (bind.bidPrice.value().isEmpty()) {
				Alerts.error(mCtx, "Please enter a bid price")
				return@setHapticClickListener
			}

			viewModel.productSalesFormat = selectedFormat
			viewModel.productPrice = bind.bidPrice.text.toString().trim()

			findNavController().navigate(ids.goToProductWeightFragment)
		}
	}

	private fun preselectFormat() {
		if (viewModel.productSalesFormat.isEmpty()) return
		formatList.forEachIndexed { index, formatModel ->
			if (formatModel.title.equals(viewModel.productSalesFormat, ignoreCase = true)) {
				formatModel.selected = true
				bind.offerLayout.isVisible = index == 1
			}
		}
	}

}