package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.AnalyticsGridAdapter
import io.bidswipe.app.databinding.FragmentOverAllBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.SalesAnalyticsResponse
import io.bidswipe.app.network.response.VisitorsAnalyticsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.parse

@SuppressLint("NotifyDataSetChanged")
class OverAllFragment : BaseFragment<SellerHubViewModel, FragmentOverAllBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentOverAllBinding.inflate(inflater, view, false)

	private var gridList = mutableListOf<SellModel>()
	private lateinit var gridAdapter: AnalyticsGridAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		gridAdapter = AnalyticsGridAdapter(gridList, mClick)
		bind.gridRecycler.adapter = gridAdapter


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
					bind.revenue.text = (mData?.stats?.revenue ?: "0").asMoney()
					bind.rating.text = (mData?.stats?.rating ?: 0).toDouble().toString()

					gridList.clear()
					gridList.add(SellModel(R.drawable.ic_people, 0, "Total Followers", ((mData?.stats?.followers ?: 0).toString())))
					gridList.add(SellModel(R.drawable.ic_star, 0, "Avg Rating", ("${(mData?.stats?.rating ?: 0)}/5")))
					gridList.add(SellModel(R.drawable.ic_video, 0, "Live Sessions", (mData?.stats?.liveSessions ?: 0).toString()))
					gridList.add(SellModel(R.drawable.ic_cart, 0, "Total Sales", (mData?.stats?.totalSales ?: 0).toString()))

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

					val mData = it.value.data?.chart?.toMutableList()

					if (mData?.isNotEmpty() == true) {
						if (mData.size == 1) {
							mData.add(0, SalesAnalyticsResponse.Data.Chart("", "0", 0))
						}
						val entries = ArrayList<Entry>()
						mData.forEachIndexed { index, chartData ->
							entries.add(Entry(index.toFloat(), chartData?.totalSales?.toFloat() ?: 0f, chartData?.totalSales))
						}

						setUpChart(mCtx, bind.salesChart, entries, mData.map { (it?.label ?: "").removePrefix("day ") }.toMutableList())
						bind.salesChart.isVisible = true
						bind.noData.isVisible = false
					} else {
						bind.salesChart.isVisible = false
						bind.noData.isVisible = true
					}
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

					val mData = it.value.data?.chart?.toMutableList()

					if (mData?.isNotEmpty() == true) {
						if (mData.size == 1) {
							mData.add(0, VisitorsAnalyticsResponse.Data.Chart("", "0"))
						}
						val entries = ArrayList<Entry>()
						mData.forEachIndexed { index, chartData ->
							entries.add(Entry(index.toFloat(), chartData?.totalVisitors?.toFloat() ?: 0f, chartData?.totalVisitors?.toInt()))
						}
						setUpChart(mCtx, bind.visitorChart, entries, mData?.map { (it?.label ?: "").removePrefix("day ") }?.toMutableList())
						bind.visitorChart.isVisible = true
						bind.noDataVisitors.isVisible = false
					} else {
						bind.visitorChart.isVisible = false
						bind.noDataVisitors.isVisible = true
					}
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

	fun setUpChart(
		mCtx: Context,
		chart: LineChart,
		values: MutableList<Entry>,
		labels: MutableList<String>? = mutableListOf(),
	) {
		val labelCount = labels?.size ?: 0
		val defFont = ResourcesCompat.getFont(mCtx, R.font.poppins_regular)!!
		chart.also {
			it.clear()
			it.invalidate()

			it.setBackgroundColor(Color.WHITE)
			it.animateX(2000)
			it.description.isEnabled = false
			it.setDrawGridBackground(false)
			it.axisRight.isEnabled = false
			it.setScaleEnabled(false)
			it.setTouchEnabled(false)
			it.isDragEnabled = false
			it.setPinchZoom(false)

			it.xAxis.setLabelCount(labelCount, true)

			if (!labels.isNullOrEmpty()) {
				it.xAxis.valueFormatter = object : ValueFormatter() {
					override fun getAxisLabel(value: Float, axis: AxisBase?): String {
						return if (value.toInt() < (labels.size)) {
							try {
								labels[value.toInt()]
							} catch (e: Exception) {
								""
							}
						} else ""
					}
				}
			}

			it.xAxis.also { xAxis ->
				xAxis.axisLineColor = ContextCompat.getColor(mCtx, R.color.inversePrimary)
				xAxis.gridColor = ContextCompat.getColor(mCtx, R.color.inversePrimary)
				xAxis.textColor = ContextCompat.getColor(mCtx, R.color.primary)
				xAxis.position = XAxis.XAxisPosition.BOTTOM
				xAxis.setDrawLimitLinesBehindData(false)
				xAxis.setDrawAxisLine(false)
				xAxis.gridLineWidth = 0f
				xAxis.typeface = defFont
				xAxis.textSize = 10f
			}

			it.axisLeft.also { yAxis ->
				yAxis.axisLineColor = ContextCompat.getColor(mCtx, R.color.inversePrimary)
				yAxis.gridColor = ContextCompat.getColor(mCtx, R.color.inversePrimary)
				yAxis.textColor = ContextCompat.getColor(mCtx, R.color.primary)
				yAxis.setDrawLimitLinesBehindData(false)
				yAxis.setDrawGridLines(false)
				yAxis.typeface = defFont
				yAxis.textSize = 10f
			}
		}

		val legend = chart.legend
		legend.isEnabled = false

		val mDataSet = LineDataSet(values, "").also { set ->
			set.lineWidth = 1.5f
			set.circleRadius = 1f
			set.valueTextSize = 0f
			set.setDrawFilled(true)
			set.setDrawCircles(false)
			set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
			set.setColors(ContextCompat.getColor(mCtx, R.color.primary))
			set.enableDashedHighlightLine(15f, 2f, 10f)
			set.highLightColor = ContextCompat.getColor(mCtx, R.color.primary)
			set.fillColor = ContextCompat.getColor(mCtx, R.color.primaryContainer)
			set.fillAlpha = 80
		}

		chart.data = LineData(arrayListOf<ILineDataSet>(mDataSet))
		chart.animateX(1500)

		chart.isNestedScrollingEnabled = true
		chart.setVisibleXRangeMaximum(10F)

		chart.moveViewToX(values.size - 6f)

	}

}