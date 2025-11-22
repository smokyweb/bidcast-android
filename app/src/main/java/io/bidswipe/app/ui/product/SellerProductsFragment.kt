package io.bidswipe.app.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSellerProductsBinding

class SellerProductsFragment : BaseFragment< ProductViewModel, FragmentSellerProductsBinding>() {
	override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java
	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	): FragmentSellerProductsBinding  = FragmentSellerProductsBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

	}


}