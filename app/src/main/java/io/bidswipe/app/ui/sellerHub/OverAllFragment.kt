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
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.TopBuyerAdapter
import io.bidswipe.app.databinding.FragmentOverAllBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.TopBuyerModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.SalesAnalyticsResponse
import io.bidswipe.app.network.response.SellerAnalyticsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class OverAllFragment : BaseFragment<SellerHubViewModel, FragmentOverAllBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentOverAllBinding.inflate(inflater, view, false)

    private val filterList = listOf("All", "Monthly", "Yearly", "Last 30 Days")

    private var topBuyersBySalesList = mutableListOf<TopBuyerModel>()
    private lateinit var topBuyersBySalesAdapter: TopBuyerAdapter

    private var topBuyersByOrdersList = mutableListOf<TopBuyerModel>()
    private lateinit var topBuyersByOrdersAdapter: TopBuyerAdapter

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    private var startDate: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -14) // Default: last 14 days
    }
    private var endDate: Calendar = Calendar.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupDateRange()
        setupClickListeners()

        bind.btnExportSales.setHapticClickListener {

            bind.loader.isVisible = true
            viewModel.exportAnalyticsData("sale" , "custom" , startDate.timeInMillis.toString() , endDate.timeInMillis.toString() )

        }

        bind.btnExportOrders.setHapticClickListener {
            viewModel.exportAnalyticsData("order" , "custom" , startDate.timeInMillis.toString() , endDate.timeInMillis.toString() )
        }

       /* val adapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            mailClassNames
        )

        bind.mailClass.setAdapter(adapter)

        val drawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.mailClass.setDropDownBackgroundDrawable(drawable)

        bind.mailClass.setOnItemClickListener { _, _, position, _ ->
            selectedMailClass = mailClassesList[position]
            log("Selected mail class: ${selectedMailClass?.label}")
            // Save state to ViewModel
            saveStateToViewModel()
        }

        bind.mailClass.setHapticClickListener {
            if (mailClassesList.isNotEmpty()) {
                bind.mailClass.showDropDown()
            } else {
                viewModel.getMailClasses()
            }
        }*/

        bind.loader.isVisible = true

        viewModel.getSellerAnalytics()
        viewModel.getSalesPerformance()
        viewModel.getVisitorsAnalytics()

        viewModel.getSellerAnalyticsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = it.value.data

                    // Update metric cards
                    bind.estimatedSales.text = (mData?.stats?.revenue ?: "0")

                    // TODO: Update top buyers lists from API
                    updateTopBuyers(mData?.topBuyersBySales , mData?.topBuyersByOrders)
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

        viewModel.getSalesPerformanceRepo.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = resource.value.data?.chart?.toMutableList()

                    if (mData?.isNotEmpty() == true) {
                        setUpBarChart(mCtx, bind.salesChart, mData)
                        bind.salesChart.isVisible = true
                        bind.noData.isVisible = false
                    } else {
                        bind.salesChart.isVisible = false
                        bind.noData.isVisible = true
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    resource.parse(mCtx, TAG, object : AlertClicks {
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
            // Visitor analytics can be removed or kept for future use
        }

        viewModel.exportAnalyticsDataRepo.observe(viewLifecycleOwner) {

        }


    }

    private fun setupRecyclerViews() {
        // Remove gridRecycler setup as it's no longer in the layout
        // gridAdapter = AnalyticsGridAdapter(gridList, mClick)
        // bind.gridRecycler.adapter = gridAdapter

        topBuyersBySalesAdapter = TopBuyerAdapter(topBuyersBySalesList, mClick)
        bind.topBuyersBySalesRecycler.layoutManager = LinearLayoutManager(mCtx)
        bind.topBuyersBySalesRecycler.adapter = topBuyersBySalesAdapter
        bind.topBuyersBySalesRecycler.isNestedScrollingEnabled = false

        topBuyersByOrdersAdapter = TopBuyerAdapter(topBuyersByOrdersList, mClick)
        bind.topBuyersByOrdersRecycler.layoutManager = LinearLayoutManager(mCtx)
        bind.topBuyersByOrdersRecycler.adapter = topBuyersByOrdersAdapter
        bind.topBuyersByOrdersRecycler.isNestedScrollingEnabled = false
    }

    private fun setupDateRange() {
        updateDateRangeDisplay()
    }

    private fun setupClickListeners() {
        bind.btnEditDates.setHapticClickListener {
            // TODO: Open date picker dialog
        }

        bind.btnPrevDate.setHapticClickListener {
            // Move date range back
            val daysDiff = ((endDate.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            endDate.add(Calendar.DAY_OF_MONTH, -daysDiff)
            startDate.add(Calendar.DAY_OF_MONTH, -daysDiff)
            updateDateRangeDisplay()
            loadDataForDateRange()
        }

        bind.btnNextDate.setHapticClickListener {
            // Move date range forward
            val daysDiff = ((endDate.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            startDate.add(Calendar.DAY_OF_MONTH, daysDiff)
            endDate.add(Calendar.DAY_OF_MONTH, daysDiff)
            // Don't allow future dates
            if (endDate.after(Calendar.getInstance())) {
                endDate = Calendar.getInstance()
                startDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -daysDiff)
                }
            }
            updateDateRangeDisplay()
            loadDataForDateRange()
        }

        bind.linkMetricsInfo.setHapticClickListener {
            // TODO: Show metrics info dialog
        }

        bind.btnExportSales.setHapticClickListener {
            // TODO: Export sales data
        }

        bind.btnExportOrders.setHapticClickListener {
            // TODO: Export orders data
        }
    }

    private fun updateDateRangeDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateRangeText = "${dateFormat.format(startDate.time)} - ${dateFormat.format(endDate.time)}"
        bind.dateRange.text = dateRangeText
    }

    private fun loadDataForDateRange() {
        bind.loader.isVisible = true
        viewModel.getSalesPerformance()
        // TODO: Call API with date range parameters
    }

    private fun updateTopBuyers(topBuyersBySalesListData: List<SellerAnalyticsResponse.Data.TopBuyersBySale?>?, topBuyersByOrdersListData: List<SellerAnalyticsResponse.Data.TopBuyersByOrder?>?,) {
        // TODO: Replace with actual API data
        topBuyersBySalesList.clear()

        var rank  = 0

        topBuyersBySalesListData?.forEach {
            rank = rank+1
            topBuyersBySalesList.add(TopBuyerModel( rank ,it?.user?.name.toString() , it?.user?.profileImage , it?.total.toString()))
        }

        topBuyersBySalesAdapter.notifyDataSetChanged()

        topBuyersByOrdersList.clear()

        rank = 0
        topBuyersByOrdersListData?.forEach {
            rank = rank+1
            topBuyersByOrdersList.add(TopBuyerModel(rank ,  it?.user?.name.toString() , it?.user?.profileImage , it?.totalOrders.toString()))
        }
        topBuyersByOrdersAdapter.notifyDataSetChanged()
    }

    fun setUpBarChart(
        mCtx: Context,
        chart: BarChart,
        chartData: MutableList<SalesAnalyticsResponse.Data.Chart?>,
    ) {
        val defFont = ResourcesCompat.getFont(mCtx, R.font.poppins_regular)!!
        chart.also {
            it.clear()
            it.invalidate()

            it.setBackgroundColor(Color.WHITE)
            it.animateY(2000)
            it.description.isEnabled = false
            it.setDrawGridBackground(false)
            it.axisRight.isEnabled = false
            it.setScaleEnabled(false)
            it.setTouchEnabled(false)
            it.isDragEnabled = false
            it.setPinchZoom(false)

            val entries = ArrayList<BarEntry>()
            val labels = mutableListOf<String>()

            chartData.forEachIndexed { index, chartItem ->
                val revenue = chartItem?.totalRevenue?.toFloatOrNull() ?: 0f
                entries.add(BarEntry(index.toFloat(), revenue))
                labels.add(chartItem?.label?.removePrefix("day ") ?: "")
            }

            it.xAxis.setLabelCount(labels.size, true)
            it.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return if (value.toInt() < labels.size) {
                        try {
                            labels[value.toInt()]
                        } catch (_: Exception) {
                            ""
                        }
                    } else ""
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
                yAxis.setDrawGridLines(true)
                yAxis.gridLineWidth = 1f
                yAxis.typeface = defFont
                yAxis.textSize = 10f
            }

            val legend = chart.legend
            legend.isEnabled = false

            val dataSet = BarDataSet(entries, "").also { set ->
                set.color = ContextCompat.getColor(mCtx, R.color.primary)
                set.valueTextSize = 0f
                set.setDrawValues(false)
            }

            chart.data = BarData(dataSet)
            chart.animateY(1500)
        }
    }
}