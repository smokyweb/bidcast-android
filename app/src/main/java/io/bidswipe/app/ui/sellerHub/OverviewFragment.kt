package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PromoteFeatureAdapter
import io.bidswipe.app.controller.PromoteMetricAdapter
import io.bidswipe.app.databinding.FragmentOverviewBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PromoteMetricModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPromoteToolsDetailsResponse
import io.bidswipe.app.network.response.GetPromoteToolsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.parse

class OverviewFragment : BaseFragment<SellerHubViewModel, FragmentOverviewBinding>() {


	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java


	override fun getBind(inflater: LayoutInflater, view: ViewGroup?): FragmentOverviewBinding = FragmentOverviewBinding.inflate(inflater, view, false)

	private var gridList = mutableListOf<GetPromoteToolsResponse.Data.Feature?>()
	private lateinit var gridAdapter: PromoteFeatureAdapter

	// Metric lists and adapters
	private var metricsList = mutableListOf<PromoteMetricModel>()
	private lateinit var metricsAdapter: PromoteMetricAdapter

	private var discoveryMetricsList = mutableListOf<PromoteMetricModel>()
	private lateinit var discoveryMetricsAdapter: PromoteMetricAdapter

	private var buyersConvertedList = mutableListOf<PromoteMetricModel>()
	private lateinit var buyersConvertedAdapter: PromoteMetricAdapter

	private var selectedDateRange = "30" // Default to 30 days

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)


		setupFilterChips()
		setupRecyclerViews()
		setupMetrics(null)

		bind.loader.isVisible = true
		viewModel.getPromoteToolsDetails()
		viewModel.getPromoteToolsDetailsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data
					setupMetrics(mData)
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
			}
		}


	}


	private fun setupRecyclerViews() {
		// Setup metrics recycler
		metricsAdapter = PromoteMetricAdapter(metricsList, mClick)
		bind.metricsRecycler.layoutManager = LinearLayoutManager(mCtx)
		bind.metricsRecycler.adapter = metricsAdapter
		bind.metricsRecycler.isNestedScrollingEnabled = false

		// Setup discovery metrics recycler
		discoveryMetricsAdapter = PromoteMetricAdapter(discoveryMetricsList, mClick)
		bind.discoveryMetricsRecycler.layoutManager = LinearLayoutManager(mCtx)
		bind.discoveryMetricsRecycler.adapter = discoveryMetricsAdapter
		bind.discoveryMetricsRecycler.isNestedScrollingEnabled = false

		// Setup buyers converted recycler
		buyersConvertedAdapter = PromoteMetricAdapter(buyersConvertedList, mClick)
		bind.buyersConvertedRecycler.layoutManager = LinearLayoutManager(mCtx)
		bind.buyersConvertedRecycler.adapter = buyersConvertedAdapter
		bind.buyersConvertedRecycler.isNestedScrollingEnabled = false
	}

	private fun updateDateRangeButtonStates(buttons: List<Pair<MaterialButton, String>>) {
		buttons.forEach { (button, days) ->
			val isSelected = selectedDateRange == days
			if (isSelected) {
				button.backgroundTintList = ContextCompat.getColorStateList(mCtx, R.color.scrim)
				button.setTextColor(ContextCompat.getColor(mCtx, R.color.onPrimary))
				button.strokeWidth = 0
			} else {
				button.backgroundTintList =
					ContextCompat.getColorStateList(mCtx, R.color.surfaceVariant)
				button.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
				button.strokeWidth = mCtx.resources.dpToPx(1)
				button.strokeColor = ContextCompat.getColorStateList(mCtx, R.color.outlineVariant)
			}
		}
	}

	private fun setupMetrics(mData: GetPromoteToolsDetailsResponse.Data?) {
		// Add initial metrics for Audience Reached
		metricsList.clear()
		metricsList.add(
			PromoteMetricModel(
				"Number of Show Boosts",
				(mData?.numberOfBoost)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		metricsList.add(
			PromoteMetricModel(
				"Number of Show Promotions",
				(mData?.numberOfShowPromote)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		metricsList.add(
			PromoteMetricModel(
				"Community Boosts",
				(mData?.communityBoot)?.toString() ?: "N/A",
				"The total number of Community Boosts buyers unlocked during your shows."
			)
		)
		metricsList.add(
			PromoteMetricModel(
				"Impressions",
				(mData?.impressions)?.toString() ?: "N/A",
				"The total number of times a Whatnot user saw your livestreams in their feeds due to a promotion."
			)
		)
		metricsList.add(
			PromoteMetricModel(
				"Number of promoted hours",
				(mData?.promoteHours)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		metricsList.add(
			PromoteMetricModel(
				"Promoted impressions per hour",
				(mData?.impessionPerHours)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		metricsAdapter.notifyDataSetChanged()

		// Discovery Impact Section
		bind.discoveryImpactHeader.isVisible = true
		bind.discoveryMetricsRecycler.isVisible = true

		discoveryMetricsList.clear()
		discoveryMetricsList.add(
			PromoteMetricModel(
				"Total Taps and Clicks",
				(mData?.totalTapsaAndClicks)?.toString() ?: "N/A",
				"Number of users that tapped into your livestream to view your show as a result of your promotions."
			)
		)
		discoveryMetricsList.add(
			PromoteMetricModel(
				"CTR (Click Through Rate)",
				(mData?.ctr)?.toString() ?: "N/A",
				"Percentage of time your promotions in feeds resulted in a buyer entering your show (taps and clicks)."
			)
		)
		discoveryMetricsList.add(
			PromoteMetricModel(
				"Sustained Watches",
				(mData?.sustainedWatches)?.toString() ?: "N/A",
				"Number of users that clicked into your stream and stayed to watch your show for longer than 30 seconds"
			)
		)
		discoveryMetricsList.add(
			PromoteMetricModel(
				"Sustained Watch Rate",
				(mData?.sustainedWatchesRate)?.toString() ?: "N/A",
				"The percentage of visitors from promotions that converted into sustained viewers"
			)
		)
		discoveryMetricsList.add(
			PromoteMetricModel(
				"Follows from Promotion",
				(mData?.followsFromPromotion)?.toString() ?: "N/A",
				"Number of buyers that followed your account by finding you via promotions"
			)
		)
		discoveryMetricsAdapter.notifyDataSetChanged()

		// Buyers Converted Section
		bind.buyersConvertedHeader.isVisible = true
		bind.buyersConvertedRecycler.isVisible = true

		buyersConvertedList.clear()
		buyersConvertedList.add(
			PromoteMetricModel(
				"First Time Buyers from Promotion",
				(mData?.firstTimeBuyersFromPromotion)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		buyersConvertedList.add(
			PromoteMetricModel(
				"Direct Sales from Promotion",
				(mData?.directSalesFormPromotion)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		buyersConvertedList.add(
			PromoteMetricModel(
				"Spend",
				(mData?.spend)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		buyersConvertedList.add(
			PromoteMetricModel(
				"Immediate Return on Spend",
				(mData?.immediateReturnOnSpend)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		buyersConvertedList.add(
			PromoteMetricModel(
				"7-Day Return on Spend",
				(mData?.dayReturnOnSpend)?.toString() ?: "N/A",
				"Run a few more promotions to start seeing results for this metric!"
			)
		)
		buyersConvertedList.add(
			PromoteMetricModel(
				"Bids from Promotion",
				(mData?.bidsFromPromotion)?.toString() ?: "N/A",
				"The number of bids from buyers who found your show via promotion"
			)
		)
		buyersConvertedAdapter.notifyDataSetChanged()

		// Add Pro Tips
		addProTip(
			bind.proTipAudience.tipText,
			"Pro Tip: Running longer promotions through Show Promote is more cost-efficient and offers the most sustained increase in discoverability."
		)

		addProTip(

			bind.proTipDiscovery1.tipText,
			"Pro Tip: Improve your promotion CTR by experimenting with different titles and thumbnails to make your livestream tile more compelling to browsers."
		)

		addProTip(
			bind.proTipDiscovery2.tipText,
			"Pro Tip: Increase Sustained Watches: Keep the potential buyers in the room once they enter. Consider always having an item or auction pinned, or engaging more with your audience."
		)

		addProTip(
			bind.proTipDiscovery3.tipText,
			"Pro Tip: Follow Rate: Remember to remind your viewers to follow you while you are selling. Use the opportunity to tell them what to expect from future shows."
		)

		addProTip(
			bind.proTipBuyer1.tipText,
			"Pro Tip: A buyer who makes a purchase in your show is more likely to be recommended your show in the future by our discovery algorithm. Consider using tools like Rewards Club to keep them engaged."
		)

		addProTip(
			bind.proTipBuyer2.tipText,
			"Pro Tip: Extra bidders in the room are valuable (even if they don't directly generate sales), as they provide engagement, boost the order value, and also benefit your discoverability."
		)
	}

	private fun addProTip(textview: TextView, tipText: String) {
		textview.text = tipText
	}

	private fun setupFilterChips() {
		if (bind.filterChipGroup.childCount > 0) {
			return // Already set up
		}

		val filters = listOf("Last 30 days", "Last 3 months", "Last 6 months", "Last 1 year")
		filters.forEachIndexed { index, filter ->
			val chip = Utils.makeAChip(
				mCtx = mCtx,
				text = filter,
				selected = index == 0,
				closeIconVisible = false,
				chipPadding = 12,
			)
			chip.setOnClickListener {
				bind.filterChipGroup.check(chip.id)
			}
			bind.filterChipGroup.addView(chip)
		}

		// Select first chip (All)
		if (bind.filterChipGroup.isNotEmpty()) {
			bind.filterChipGroup.check(bind.filterChipGroup.getChildAt(0).id)
		}

	}

}