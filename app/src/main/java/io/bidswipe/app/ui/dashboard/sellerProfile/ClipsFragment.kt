package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ClipsAdapter
import io.bidswipe.app.databinding.FragmentClipsBinding
import io.bidswipe.app.utils.Utils

class ClipsFragment : BaseFragment<SellerViewModel , FragmentClipsBinding>() {
	override fun getModel() = SellerViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentClipsBinding.inflate(inflater , view , false)

	private lateinit var clipsAdapter : ClipsAdapter

	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = false
			bind.recycler.isVisible = true
			bind.noData.isVisible = false
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val staticClips = listOf(
			"Clip 1" ,
			"Clip 2" ,
			"Clip 3" ,
			"Clip 4" ,
			"Clip 5" ,
			"Clip 6"
		)
		clipsAdapter = ClipsAdapter(mList = staticClips)
		bind.recycler.adapter = clipsAdapter
		bind.recycler.isVisible = true
		bind.noData.isVisible = false
		bind.loader.isVisible = false
		bind.noInternet.isVisible = false
	}
}