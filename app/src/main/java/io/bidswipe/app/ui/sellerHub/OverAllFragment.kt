package io.bidswipe.app.ui.sellerHub

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.AnalyticsGridAdapter
import io.bidswipe.app.databinding.FragmentOverAllBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse

class OverAllFragment : BaseFragment<SellerHubViewModel, FragmentOverAllBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentOverAllBinding.inflate(inflater, view, false)

	private var gridList = mutableListOf<SellModel>()
	private var reqList = mutableListOf("", "", "", "")
	private lateinit var gridAdapter: AnalyticsGridAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)


		gridAdapter = AnalyticsGridAdapter(gridList, mClick)
		bind.gridRecycler.adapter = gridAdapter


		createVisitorChart()

		bind.loader.isVisible = true

		viewModel.getSellerAnalytics()

		viewModel.getSalesPerformance()

		viewModel.getVisitorsAnalytics()

		viewModel.getSellerAnalyticsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					bind.items.text = (mData?.stats?.totalItems ?: 0).toString()
					bind.revenue.text = mData?.stats?.revenue ?: ""
					bind.rating.text = (mData?.stats?.rating ?: 0).toString()

					gridList.clear()
					gridList.add(SellModel(R.drawable.ic_people, 0, "Total Followers", ((mData?.stats?.followers ?:0).toString()) ))
					gridList.add(SellModel(R.drawable.ic_star, 0, "Avg Rating", ("${(mData?.stats?.rating ?:0)}/5")))
					gridList.add(SellModel(R.drawable.ic_video, 0, "Live Sessions", (mData?.stats?.liveSessions ?:0).toString()))
					gridList.add(SellModel(R.drawable.ic_cart, 0, "Total Sales", (mData?.stats?.totalSales ?:0).toString()))

					gridAdapter.notifyDataSetChanged()

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

		viewModel.getSalesPerformanceRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					log("PERFORMANCE DATA : ${mData}")

					val entries = ArrayList<Entry>()

					mData?.chart?.forEach {
						entries.add(Entry(1f, 10f,it))
					}

					createSalesChart()
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

		viewModel.getVisitorsAnalyticsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data





					log("VISITORS DATA : ${mData}")
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

	private fun createSalesChart() {

		val entries = ArrayList<Entry>()
		entries.add(Entry(1f, 10f)) // x-value, y-value
		entries.add(Entry(2f, 5f))
		entries.add(Entry(2f, 15f))
		entries.add(Entry(3f, 8f))
		entries.add(Entry(4f, 10f))
		entries.add(Entry(4f, 12f))
		entries.add(Entry(4f, 2f))

		val lineDataSet = LineDataSet(entries, "My Data")
		lineDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER

		lineDataSet.cubicIntensity = 0.1f // Adjust for desired curve intensity
		lineDataSet.setDrawFilled(true) // To fill the area below the line
		lineDataSet.fillAlpha = 80 // Transparency of the fill color
		lineDataSet.fillColor =  ContextCompat.getColor(mCtx,R.color.primaryContainer)// Color of the fill area
		lineDataSet.color = ContextCompat.getColor(mCtx,R.color.primary) // Color of the line
		lineDataSet.lineWidth = 2f
		lineDataSet.setDrawCircles(false)

		val lineData = LineData(lineDataSet)

		bind.salesChart.data = lineData

		bind.salesChart.description.isEnabled = false // Hide description label
		bind.salesChart.setTouchEnabled(true)
		bind.salesChart.isDragEnabled = true
		bind.salesChart.setScaleEnabled(true)
		bind.salesChart.setPinchZoom(true)

		// Customize X-axis
		val xAxis = bind.salesChart.xAxis
		xAxis.position = XAxis.XAxisPosition.BOTTOM
		xAxis.setDrawGridLines(false)

		val xLabels = arrayOf("Jan","Jan", "Feb", "Mar", "Apr")
		bind.salesChart.xAxis.apply {
			granularity = 1f

			valueFormatter = object : ValueFormatter() {
				override fun getFormattedValue(value: Float): String {
					return if (value >= 0 && value < xLabels.size) xLabels[value.toInt()] else ""
				}
			}
		}

		val yLabels = arrayOf("10", "20", "30", "40")
		bind.salesChart.axisLeft.apply {
			granularity = 1f
			valueFormatter = object : ValueFormatter() {
				override fun getFormattedValue(value: Float): String {
					val index = value.toInt()
					return if (index >= 0 && index < yLabels.size) yLabels[value.toInt()] else ""
				}
			}
		}

		// Customize Y-axis
		val leftAxis = bind.salesChart.axisLeft
		leftAxis.setDrawGridLines(false)
		bind.salesChart.axisRight.isEnabled = true // Disable right Y-axis

		bind.salesChart.animateX(1500) // Animate the chart
		bind.salesChart.invalidate() // Refresh the chart

	}

	private fun createVisitorChart() {

		val entries = ArrayList<Entry>()
		entries.add(Entry(1f, 10f)) // x-value, y-value
		entries.add(Entry(2f, 5f))
		entries.add(Entry(2f, 15f))
		entries.add(Entry(3f, 8f))
		entries.add(Entry(4f, 10f))
		entries.add(Entry(4f, 12f))
		entries.add(Entry(4f, 2f))

		val lineDataSet = LineDataSet(entries, "My Data")
		lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
		lineDataSet.color = Color.BLUE
		lineDataSet.setCircleColor(Color.RED)
		lineDataSet.setDrawCircles(false)
		lineDataSet.lineWidth = 2f

		lineDataSet.circleRadius = 4f
		lineDataSet.setDrawValues(true)

		val lineData = LineData(lineDataSet)

		bind.visitorChart.data = lineData

		bind.visitorChart.description.isEnabled = false // Hide description label
		bind.visitorChart.setTouchEnabled(true)
		bind.visitorChart.isDragEnabled = true
		bind.visitorChart.setScaleEnabled(true)
		bind.visitorChart.setPinchZoom(true)

		// Customize X-axis
		val xAxis = bind.visitorChart.xAxis
		xAxis.position = XAxis.XAxisPosition.BOTTOM
		xAxis.setDrawGridLines(false)

		// Customize Y-axis
		val leftAxis = bind.visitorChart.axisLeft
		leftAxis.setDrawGridLines(false)
		bind.visitorChart.axisRight.isEnabled = false // Disable right Y-axis

		bind.visitorChart.animateX(1500) // Animate the chart
		bind.visitorChart.invalidate() // Refresh the chart

	}

}