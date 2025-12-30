package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSellerOrderDetailBinding

class SellerOrderDetailFragment : BaseFragment<SellerHubViewModel, FragmentSellerOrderDetailBinding>() {

	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?): FragmentSellerOrderDetailBinding  = FragmentSellerOrderDetailBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}



	}

}