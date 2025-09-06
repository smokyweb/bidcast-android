package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCompleteYourProfileBinding
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids

class CompleteYourProfileFragment : BaseFragment<DashViewModel , FragmentCompleteYourProfileBinding>() {
	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentCompleteYourProfileBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			findNavController().navigate(ids.action_completeYourProfileFragment_to_prepareYourShowFragment)
		}

		bind.continueBtn.setOnClickListener {
			viewModel.currentStep = 4
			findNavController().navigate(ids.action_completeYourProfileFragment_to_prepareYourShowFragment)
		}

	}

}