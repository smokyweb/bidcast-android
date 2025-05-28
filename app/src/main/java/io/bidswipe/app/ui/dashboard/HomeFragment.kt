package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

class HomeFragment : BaseFragment<DashViewModel, FragmentHomeBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentHomeBinding.inflate(inflater, view, false)
	
	private lateinit var homeAdapter: HomeAdapter
	private var showList = mutableListOf<GetMyShowResponse.Data?>()

	private var categoriesList = mutableListOf<String>()
	
	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			
			when (status) {
				
				"user" -> {
					
					startActivity(Intent(mCtx, SellerProfileActivity::class.java).putExtra("userId",
						showList[pos]?.userId.toString()
					))
					
				}
			}
			
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		homeAdapter = HomeAdapter(showList, mClick)
		
		bind.recycler.adapter = homeAdapter

		selectTab(bind.recommended)

		bind.recommended.setOnClickListener { selectTab(it as TextView) }
		bind.popular.setOnClickListener { selectTab(it as TextView) }
		bind.all.setOnClickListener { selectTab(it as TextView) }

		
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

		bind.loader.isVisible = true

		viewModel.getLiveShow()

		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					showList.clear()

					mData?.forEach {
						showList.add(it)
					}

					if (showList.isEmpty()){
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					}else{
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}


					homeAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}
		
		
	}

	fun selectTab(selectedTab: TextView) {
		val tabs = listOf(bind.recommended, bind.popular, bind.all)
		tabs.forEach {
			it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
			it.setTypeface(null, Typeface.NORMAL)
		}
		selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
		selectedTab.setTypeface(null, Typeface.BOLD)
	}
	
}