package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ClipsAdapter
import io.bidswipe.app.databinding.FragmentClipsBinding

class ClipsFragment : BaseFragment<SellerViewModel, FragmentClipsBinding>() {
	override fun getModel() = SellerViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	) = FragmentClipsBinding.inflate(inflater, view, false)
	private lateinit var clipsAdapter: ClipsAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		clipsAdapter = ClipsAdapter()
		bind.recycler.adapter = clipsAdapter

		showEmptyState()
	}
	private fun showEmptyState() {
		bind.recycler.isVisible = false
		bind.noData.isVisible = true
		bind.loader.isVisible = false
		bind.noInternet.isVisible = false
	}
}