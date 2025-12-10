package io.bidswipe.app.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentRaiseTicketBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class RaiseTicketFragment : BaseFragment<ProductViewModel, FragmentRaiseTicketBinding>() {
	override fun getModel(): Class<ProductViewModel>  = ProductViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	): FragmentRaiseTicketBinding = FragmentRaiseTicketBinding.inflate(inflater,view,false)

	private var orderId : String ? = ""

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		orderId = arguments?.getString("orderId") ?: ""

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.submit.setHapticClickListener {

			when{

				bind.subject.value().isEmpty() -> {
					Alerts.error(mCtx,"Subject can't be empty")
					bind.subject.requestFocus()
					return@setHapticClickListener
				}

				bind.description.value().isEmpty() -> {

					Alerts.error(mCtx,"Enter Message")
					bind.subject.requestFocus()
					return@setHapticClickListener

				}

				else->{
					hideKeyboard(it)

					bind.loader.isVisible = true

					viewModel.raiseTicket(
						orderId = orderId?.request(),
						subject = bind.subject.value().request(),
						message = bind.description.value().request()
					)

				}

			}

		}


		viewModel.raiseTicketRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data
					Alerts.success(mCtx, it.value.message.toString())

					findNavController().popBackStack()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
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