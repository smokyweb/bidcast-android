package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSelectCategoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class SelectCategoryFragment : BaseFragment<ScheduleShowViewModel , FragmentSelectCategoryBinding>() {
	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentSelectCategoryBinding.inflate(inflater , view , false)

	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var auctionTypeList = mutableListOf<GetAuctionTypeResponse.Data?>()

	private var categoryId = ""
	private var auctionId = ""

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.continueBtn.setOnClickListener {
			when {
				categoryId.isEmpty() -> {
					Alerts.error(mCtx , "Please Select a Category")
				}

				auctionId.isEmpty() -> {
					Alerts.error(mCtx , "Please select an Auction Type")
				}

				else -> {

					viewModel.auctionId = auctionId
					viewModel.categoryId = categoryId
					findNavController().navigate(ids.goToSelectThumbnailFragment)
				}

			}
		}

		bind.category.setOnItemClickListener { _ , _ , position , _ ->
			categoryId = categoryList[position]?.id.toString()
		}

		bind.category.setOnClickListener {
			bind.category.showDropDown()
		}

		bind.auctionType.setOnItemClickListener { _ , _ , position , _ ->
			auctionId = auctionTypeList[position]?.id.toString()
		}

		bind.auctionType.setOnClickListener {
			bind.auctionType.showDropDown()
		}

		bind.loader.isVisible = true
		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					if (it.value.data?.isNotEmpty() == true) {
						bind.loader.isVisible = false
						viewModel.getCategoryRepo.value = null
						categoryList.clear()
						categoryList.addAll(it.value.data)

						val adapter = ArrayAdapter(mCtx , android.R.layout.simple_list_item_1 , categoryList.map { it?.name })
						bind.category.setAdapter(adapter)
						val draw = ContextCompat.getDrawable(mCtx , R.drawable.card_8)
						bind.category.setDropDownBackgroundDrawable(draw)

					}
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

		viewModel.getAuctionType()

		viewModel.getAuctionTypeRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					if (it.value.data?.isNotEmpty() == true) {
						auctionTypeList.clear()
						auctionTypeList.addAll(it.value.data)

						val adapter = ArrayAdapter(mCtx , android.R.layout.simple_list_item_1 , auctionTypeList.map { it?.name })
						bind.auctionType.setAdapter(adapter)
						val draw = ContextCompat.getDrawable(mCtx , R.drawable.card_8)
						bind.auctionType.setDropDownBackgroundDrawable(draw)

					}
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