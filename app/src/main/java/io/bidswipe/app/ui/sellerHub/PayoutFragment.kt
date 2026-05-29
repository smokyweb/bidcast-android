package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.davidmiguel.numberkeyboard.NumberKeyboardListener
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPayoutBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class PayoutFragment : BaseFragment<SellerHubViewModel, FragmentPayoutBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java
	
	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPayoutBinding.inflate(inflater, view, false)

	var walletAmount =0.0
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if(arguments?.getString("amount")!=null) {
			walletAmount=(arguments?.getString("amount")?:"0.0").toDouble()
		}

		bind.walletAmount.text= buildSpannedString {
			append("Available for payout : ")
			append("$")
			append(walletAmount.toString())
		}

		bind.header.onBackClick {
			findNavController().popBackStack()
		}
		
		val screenWidth = (resources.displayMetrics.widthPixels)-resources.dpToPx(52)
		val keyWidth = (screenWidth * 0.3).toInt()
		
		bind.numberKeyboard.apply {
		setKeyWidth(keyWidth)
		}
		
		bind.numberKeyboard.setListener(object : NumberKeyboardListener {
			override fun onNumberClicked(number: Int) {
				var current = bind.amount.text.toString()
				
				if (current.contains(".")) {
					val decimals = current.substringAfter(".")
					if (decimals.length >= 2) {
						return
					}
				}
				
				current += number
				bind.amount.setText(current)
			}
			
			override fun onLeftAuxButtonClicked() {
				var current = bind.amount.text.toString()
				if (!current.contains(".")) {
					current += "."
					bind.amount.setText(current)
				}
			}
			
			override fun onRightAuxButtonClicked() {
				var current = bind.amount.text.toString()
				if (current.isNotEmpty()) {
					current = current.dropLast(1)
					bind.amount.setText((current.ifEmpty { "" }))
				}
			}
		})
		
		bind.payout.setHapticClickListener {
			if (bind.amount.text.isEmpty()) {
				errorToast("Enter Amount")
			} else {
				val amount = bind.amount.text.toString().toDouble()
				
				if (amount < 10) {
					errorToast("Minimum payout is $10")
					return@setHapticClickListener
				} else if (amount > 500) {
					errorToast("Maximum payout is $500")
					return@setHapticClickListener
				}
				
				if (walletAmount < amount) {
					errorToast("Insufficient Balance")
				} else {
					bind.loader.isVisible = true
					viewModel.payout(amount.toString().request())
				}
			}
		}
		
		viewModel.payoutRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.payoutRepo.value = null
					
					AppBottomSheet(
						mCtx,
						R.drawable.ic_success,
						"Success",
						// MC (2026-05-28): blank-popup fix - never show an empty
						// success dialog; fall back when message is null/blank.
						it.value.message?.takeIf { m -> m.isNotBlank() } ?: "Request submitted successfully.",
						primaryBtnText = "Okay",
						secondaryBtnText = "Cancel",
						canCancel = true,
						showSecondary = false,
						iconPadding = 16,
						alertType = AlertType.SUCCESS,
						clicks = object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								App.getProfile()
								findNavController().popBackStack()
							}
							
							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						}
					).show()
				}
				
				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.payoutRepo.value = null
					
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