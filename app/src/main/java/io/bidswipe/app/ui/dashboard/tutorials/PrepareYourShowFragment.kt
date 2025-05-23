package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowAdapter
import io.bidswipe.app.databinding.FragmentPrepareYourShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.toScheduleShow

class PrepareYourShowFragment : BaseFragment<DashViewModel, FragmentPrepareYourShowBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	) = FragmentPrepareYourShowBinding.inflate(inflater, view, false)
	
	private val showList = mutableListOf<GetPrepareStepResponse.Data?>()
	private val currentStep = 0
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		bind.header.onBackClick {
			findNavController().popBackStack()
		}
		
		/*showList.clear()
		showList.addAll(
			listOf(
				ShowModel("Schedule your first show", true, false, "Pick a date and time for your live show"),
				ShowModel("Add Products to your show", false, false, "Select products you'll be featuring"),
				ShowModel("Rehearse going live", false, false, "Practice with our simulator"),
				ShowModel("Bring in buyers", false, false, "Share your show with potential buyer"),
				ShowModel("Preview show and go live", false, true, "Final check and start streaming")
			)
		
		)*/
		

		
		val adapter = ShowAdapter(mList = showList,  object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				if (status == null) {
					
					bind.stepProgress.setProgress(pos + 1)
					
					showList.forEachIndexed { index, showModel ->
						showModel?.selected = index == pos
					}
					
					bind.recycler.adapter?.notifyDataSetChanged()
					
				} else {
					when (pos) {
						0 -> {
							startActivity(mCtx.toScheduleShow(from = "tutorial"))
						}
						
						1 -> {
							findNavController().navigate(ids.goToShowTipsFragment, bundleOf("type" to "showTips"))
						}
						
						2 -> {
							findNavController().navigate(ids.goToShowTipsFragment, bundleOf("type" to "liveTips"))
						}
						
						3 -> {
							findNavController().navigate(
								ids.goToShowTipsFragment,
								bundleOf("type" to "bringInBuyers")
							)
						}
						
					}
				}
			}
			
		})
		
		bind.recycler.adapter = adapter

		bind.loader.isVisible = false

		viewModel.getPrepareStep()
		viewModel.getPrepareStepRepo.observe (viewLifecycleOwner){
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					showList.clear()

					mData?.forEach {

						showList.add(it)

					}

					bind.stepProgress.max = showList.size
					bind.stepProgress.setProgress(1)

					adapter.notifyDataSetChanged()

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
	
}