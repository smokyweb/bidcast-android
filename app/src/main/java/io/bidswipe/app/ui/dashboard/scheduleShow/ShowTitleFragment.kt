package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExampleAdapter
import io.bidswipe.app.controller.TitleAdapter
import io.bidswipe.app.databinding.FragmentShowTitleBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class ShowTitleFragment : BaseFragment<ScheduleShowViewModel , FragmentShowTitleBinding>() {

	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentShowTitleBinding.inflate(inflater , view , false)

	private lateinit var titleAdapter : TitleAdapter
	private lateinit var exampleAdapter : ExampleAdapter

	private var titleList = mutableListOf<GetAllTipsResponse.Data.Tip?>()
	private var exampleList = mutableListOf<String?>()

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val from = activity?.intent?.getStringExtra("from")

		bind.header.onBackClick {
			finish()
		}

		bind.layout.setOnClickListener {
			hideKeyboard(it)
		}

		titleAdapter = TitleAdapter(titleList)
		bind.recycler.adapter = titleAdapter

		exampleAdapter = ExampleAdapter(exampleList)
		bind.exampleRecycler.adapter = exampleAdapter


		bind.continueBtn.setOnClickListener {

			when {

				bind.showTitle.value().isEmpty() -> {
					Alerts.error(mCtx , "Please enter the show title")
					bind.showTitle.requestFocus()
					showKeyboard(bind.showTitle)
				}

				else -> {
					viewModel.showTitle = bind.showTitle.value()
					hideKeyboard(it)

					if (from == "tips" || from == "showTutorial") {
						findNavController().navigate(ids.goToSelectCategoryFragment)

					} else {
						findNavController().navigate(ids.goToSelectShowTimeFragment)
					}
				}
			}

		}

		bind.loader.isVisible = true

		viewModel.getAllTips("title".request())

		viewModel.getAllTipsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					titleList.clear()
					exampleList.clear()

					mData?.tips?.forEach {
						titleList.add(it)
					}

					mData?.example?.forEach {
						exampleList.add(it)
					}

					titleAdapter.notifyDataSetChanged()

					exampleAdapter.notifyDataSetChanged()

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