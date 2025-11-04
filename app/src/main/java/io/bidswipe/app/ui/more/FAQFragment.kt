package io.bidswipe.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FAQAdapter
import io.bidswipe.app.databinding.FragmentFAQBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe

class FAQFragment : BaseFragment<MoreViewModel, FragmentFAQBinding>() {
	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentFAQBinding.inflate(inflater, view, false)

	private var faqList = mutableListOf<FAQResponse.Data?>()
	private var categoriesList = mutableListOf<String>()

	private lateinit var adapter: FAQAdapter

	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			faqList.forEachIndexed { index, data ->
				data?.selected = index == pos
			}

			adapter.notifyDataSetChanged()

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		categoriesList = mutableListOf("All FAQs", "Bidding", "Payments")
		categoriesList.forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx,
					text = it,
					selected = false,
					closeIconVisible = false
				)
			)
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}


		adapter = FAQAdapter(faqList, mClick)

		bind.recyclerFaq.adapter = adapter

		bind.loader.isVisible = true

		viewModel.getFAQ()

		viewModel.getFAQRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					faqList.clear()

					mData?.forEach {

						faqList.add(it)

					}


					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}
					})
				}

				else -> {}

			}
		}

	}

}