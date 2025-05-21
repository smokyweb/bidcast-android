package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe

class HomeFragment : BaseFragment<DashViewModel, FragmentHomeBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentHomeBinding.inflate(inflater, view, false)
	
	private lateinit var homeAdapter: HomeAdapter
	private var itemList = mutableListOf<String>()
	private var categoriesList = mutableListOf<String>()
	
	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			
			when (status) {
				
				"user" -> {
					
					startActivity(Intent(mCtx, SellerProfileActivity::class.java))
					
				}
			}
			
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		homeAdapter = HomeAdapter(itemList, mClick)
		
		bind.recycler.adapter = homeAdapter
		
		repeat(6) {
			itemList.add("  ")
		}
		
		homeAdapter.notifyDataSetChanged()
		
		categoriesList = mutableListOf("For You", "Collectibles", "Trading Cards")
		categoriesList.forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx,
					text = it,
					selected = false
				)
			)
		}
		
		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}
		
		
	}
	
}