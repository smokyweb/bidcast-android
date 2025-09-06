package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.davidmiguel.numberkeyboard.NumberKeyboardListener
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPayoutBinding

class PayoutFragment : BaseFragment<SellerHubViewModel , FragmentPayoutBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentPayoutBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.numberKeyboard.apply {

		}

		bind.numberKeyboard.setListener(object : NumberKeyboardListener {
			override fun onNumberClicked(number : Int) {

			}

			override fun onLeftAuxButtonClicked() {

			}

			override fun onRightAuxButtonClicked() {

			}
		})


	}

}