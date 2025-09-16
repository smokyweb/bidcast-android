package io.bidswipe.app.ui.dashboard.tutorials

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowAdapter
import io.bidswipe.app.databinding.FragmentPrepareYourShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toScheduleShow
import okhttp3.MultipartBody
import java.io.File

class PrepareYourShowFragment : BaseFragment<DashViewModel , FragmentPrepareYourShowBinding>() {
	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentPrepareYourShowBinding.inflate(inflater , view , false)

	var imagePartList = mutableListOf<MultipartBody.Part?>()

	private var showId = ""

	private var scheduleShowLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {

				if (result.data != null) {
					log(
						"DATA : ${result.data?.getStringExtra("date").toString()}"
					)

					viewModel.showDate = result.data?.getStringExtra("date").toString()
					viewModel.showTime = result.data?.getStringExtra("time").toString()
					viewModel.currentStep = 1
					viewModel.showList[0]?.status = "completed"
					viewModel.showList[1]?.status = "locked"
					bind.recycler.adapter?.notifyDataSetChanged()
				}

			}

		}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		val adapter = ShowAdapter(mList = viewModel.showList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {
				if (status == null) {

					bind.stepProgress.setProgress(pos + 1)

					viewModel.showList.forEachIndexed { index , showModel ->
						showModel?.selected = index == pos
					}

					bind.recycler.adapter?.notifyDataSetChanged()

				} else {
					when (pos) {
						0 -> {
							scheduleShowLauncher.launch(mCtx.toScheduleShow(from = "tutorial"))
						}

						1 -> {
							findNavController().navigate(ids.goToShowTipsFragment , bundleOf("type" to "showTips"))
						}

						2 -> {
							findNavController().navigate(ids.goToShowTipsFragment , bundleOf("type" to "liveTips"))
						}

						3 -> {

							val mData = viewModel.showData.value

							imagePartList.add(
								Utils.imagePart(
									"thumbnail[]" ,
									mData?.thumbnail.toString() ,
									File(mData?.thumbnail)
								)
							)

							if (mData?.productIds?.isEmpty() == true){
								Alerts.error(mCtx,"Please select products")
								return
							}

							val productIds = mData?.productIds?.split(",")?.map {
								it.toInt()
							}

							bind.loader.isVisible = true

							viewModel.storeScheduleShow(
								mData?.showTitle?.request() ,
								viewModel.showDate.request() ,
								viewModel.showTime.request() ,
								mData?.categoryId?.request() ,
								mData?.actionId?.request() ,
								imagePartList ,
								productIds?.joinToString(",")?.request()
							)

						}

						4 -> {

							bind.loader.isVisible = true

							findNavController().navigate(
								ids.goToShowTipsFragment ,
								bundleOf("type" to "goLive" , "showId" to viewModel.showId)
							)
						}

					}
				}
			}

		})

		bind.recycler.adapter = adapter

		bind.loader.isVisible = false

		if (viewModel.getPrepareStepRepo.value == null) {
			viewModel.getPrepareStep()
		}

		viewModel.getPrepareStepRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					viewModel.showList.clear()

					mData?.forEach {

//						it?.status ="locaked"

						viewModel.showList.add(it)

					}

					bind.stepProgress.max = viewModel.showList.size
					bind.stepProgress.setProgress(1)

					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

		viewModel.showData.observe(viewLifecycleOwner) {
			if (viewModel.showList.isNotEmpty()) {
				val mData = it
				log("SHOW DATA :$it")


				when (viewModel.currentStep) {
					1 -> {
						viewModel.showList[0]?.status = "completed"
						viewModel.showList[1]?.status = "locked"

					}

					2 -> {
						viewModel.showList[1]?.status = "completed"
						viewModel.showList[2]?.status = "locked"
					}

					3 -> {
						viewModel.showList[2]?.status = "completed"
						viewModel.showList[3]?.status = "locked"
					}

					4 -> {
						viewModel.showList[3]?.status = "completed"
						viewModel.showList[4]?.status = "locked"
					}

				}

				bind.recycler.adapter?.notifyDataSetChanged()
			}

		}

		viewModel.storeScheduleShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.storeScheduleShowRepo.value = null
					bind.loader.isVisible = false

					val mData = it.value.data

					viewModel.showId = mData?.id.toString()

					findNavController().navigate(
						ids.goToShowTipsFragment ,
						bundleOf("type" to "bringInBuyers" , "showId" to mData?.id.toString())
					)

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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