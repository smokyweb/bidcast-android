package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class PayoutFragment : BaseFragment<SellerHubViewModel, FragmentPayoutBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java
	
	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPayoutBinding.inflate(inflater, view, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		bind.header.onBackClick {
			findNavController().popBackStack()
		}
		
		bind.numberKeyboard.apply {
		
		}
		
		bind.numberKeyboard.setListener(object : NumberKeyboardListener {
			override fun onNumberClicked(number: Int) {
			
			}
			
			override fun onLeftAuxButtonClicked() {
			
			}
			
			override fun onRightAuxButtonClicked() {
			
			}
		})
		
		
		bind.payout.setHapticClickListener {
			val amount = bind.amount.text.toString().toDouble()
			if (App.profileResponse.value?.walletAmount.toString().toDouble() < amount) {
				errorToast("Insufficient Balance")
			} else {
				bind.loader.isVisible = true
				viewModel.payout(amount.toString().request())
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
						it.value.message ?: "",
						primaryBtnText = "Okay",
						secondaryBtnText = "Cancel",
						canCancel = true,
						showSecondary = false,
						iconPadding = 16,
						alertType = AlertType.SUCCESS,
						clicks = object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
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