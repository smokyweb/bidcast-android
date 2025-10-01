package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentTermsConditionBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class TermsConditionFragment : BaseFragment<MoreViewModel, FragmentTermsConditionBinding>() {
	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentTermsConditionBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.loader.isVisible = true

		viewModel.getTermsConditions()

		viewModel.getTermsConditionsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.getTermsConditionsRepo.value = null
					bind.content.setHtmlFromString(it.value.data?.pageContent ?: "", false)
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getTermsConditionsRepo.value = null
					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}
					})

				}

				else -> {}
			}
		}
	}
}