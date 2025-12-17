package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import com.google.android.material.datepicker.MaterialDatePicker
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.TopBuyerAdapter
import io.bidswipe.app.databinding.FragmentOverAllBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.TopBuyerModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.RetrofitService
import io.bidswipe.app.network.response.SalesAnalyticsResponse
import io.bidswipe.app.network.response.SellerAnalyticsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.Utils.timestamp
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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

    // Track when all analytics calls have finished so we can hide the loader only once
    private var isSellerAnalyticsLoaded = false
    private var isSalesPerformanceLoaded = false
    private var isVisitorsAnalyticsLoaded = false

    private fun resetAnalyticsLoadingState() {
        isSellerAnalyticsLoaded = false
        isSalesPerformanceLoaded = false
        isVisitorsAnalyticsLoaded = false
        bind.loader.isVisible = true
    }

    private fun updateAnalyticsLoader() {
        bind.loader.isVisible =
            !(isSellerAnalyticsLoaded && isSalesPerformanceLoaded && isVisitorsAnalyticsLoaded)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupDateRange()
        setupClickListeners()

        bind.btnExportSales.setHapticClickListener {
            bind.loader.isVisible = true
//            viewModel.exportAnalyticsData("sale" , "custom" , Utils.getSimpleDate("yyyy-MM-dd").format(startDate.timeInMillis),Utils.getSimpleDate("yyyy-MM-dd").format(endDate.timeInMillis))
           downloadFile(mCtx,"sale"){
               bind.loader.isVisible = false
               if(it) {
                   Alerts.success(mCtx, "File downloaded successfully!")
               }else{
                   Alerts.error(mCtx, "Something went wrong!")
               }
           }

        }

        bind.btnExportOrders.setHapticClickListener {
            bind.loader.isVisible = true
            downloadFile(mCtx, "order") {
                bind.loader.isVisible = false
                if (it) {
                    Alerts.success(mCtx, "File downloaded successfully!")
                } else {
                    Alerts.error(mCtx, "Something went wrong!")
                }
            }
            //            viewModel.exportAnalyticsData("order" , "custom" , Utils.getSimpleDate("yyyy-MM-dd").format(startDate.timeInMillis),Utils.getSimpleDate("yyyy-MM-dd").format(endDate.timeInMillis))
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

        // Initial load: show loader until all 3 analytics calls complete
        resetAnalyticsLoadingState()
        viewModel.getSellerAnalytics()
        viewModel.getSalesPerformance()
        viewModel.getVisitorsAnalytics()

        viewModel.getSellerAnalyticsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    isSellerAnalyticsLoaded = true
                    updateAnalyticsLoader()
                    val mData = it.value.data

                    bind.estimatedSales.text = (mData?.stats?.revenue ?: "0")

                    updateTopBuyers(mData?.topBuyersBySales , mData?.topBuyersByOrders)
                }

                is Resource.Error -> {
                    isSellerAnalyticsLoaded = true
                    updateAnalyticsLoader()
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
                    isSalesPerformanceLoaded = true
                    updateAnalyticsLoader()
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
                    isSalesPerformanceLoaded = true
                    updateAnalyticsLoader()
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
            when (it) {
                is Resource.Success,
                is Resource.Error -> {
                    // Mark visitors analytics as done (success or error)
                    isVisitorsAnalyticsLoaded = true
                    updateAnalyticsLoader()
                }

                else -> {}
            }
        }

    }

    private fun setupRecyclerViews() {

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
            val builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(
                    androidx.core.util.Pair<Long, Long>(
                        MaterialDatePicker.todayInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds() + (7 * 24 * 60 * 60 * 1000)
                    )
                )

            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                startDate = Calendar.getInstance().apply {
                    timeInMillis = selection.first
                }
                endDate = Calendar.getInstance().apply {
                    timeInMillis = selection.second
                }

                updateDateRangeDisplay()

            }
            // Show the picker
            picker.show(parentFragmentManager, picker.toString())
        }

        bind.btnPrevDate.setHapticClickListener {
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
        // For date range changes we only refetch sales performance.
        // Mark just that one as loading; the others stay as-is.
        bind.loader.isVisible = true
        isSalesPerformanceLoaded = false
        viewModel.getSalesPerformance()
    }

    private fun updateTopBuyers(topBuyersBySalesListData: List<SellerAnalyticsResponse.Data.TopBuyersBySale?>?, topBuyersByOrdersListData: List<SellerAnalyticsResponse.Data.TopBuyersByOrder?>?,) {
        topBuyersBySalesList.clear()
        var rank  = 0

        topBuyersBySalesListData?.forEach {
            rank = rank+1
            topBuyersBySalesList.add(TopBuyerModel( rank ,it?.user?.name.toString() , it?.user?.profileImage , it?.total?.asMoney().toString()))
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

    fun downloadFile(mCtx: Context,type:String, callback: (status: Boolean)->Unit) {
        val apiService = RetrofitService(mCtx).build()
        val call = apiService.exportAnalyticsDataD(type , "custom" , Utils.getSimpleDate("yyyy-MM-dd").format(startDate.timeInMillis),Utils.getSimpleDate("yyyy-MM-dd").format(endDate.timeInMillis))

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Start saving the file to local storage
                    response.body()?.let { body ->
                        SaveFileTask(mCtx){
                            callback(it)
                        }.execute(body)
                    }
                } else {
                    Log.e("FileDownload", "Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FileDownload", "Failure: ${t.message}")
            }
        })
    }

    class SaveFileTask(val context: Context,val callback: (status: Boolean)->Unit) : AsyncTask<ResponseBody, Void, Boolean>() {
        override fun doInBackground(vararg params: ResponseBody?): Boolean {
            val inputStream: InputStream?
            val outputStream: OutputStream?
            try {
                // Get the input stream from the response body
                inputStream = params[0]?.byteStream()

                // Create ContentValues to specify the file's metadata
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "sale_top_buyers_report.csv") // The name of the file
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv") // The MIME type of the file
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Bidcast") // Save in app folder in Downloads
                }

                // Get the URI to insert the file into Downloads
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                outputStream = context.contentResolver.openOutputStream(uri!!) // Open output stream

                // Read data from the input stream and write it to the output stream
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                }

                outputStream?.close()
                inputStream?.close()

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                callback(true)
                Log.d("FileDownload", "File downloaded successfully!")
                Alerts.success(context, "File downloaded successfully!")
            } else {
                callback(false)
                Log.e("FileDownload", "Failed to download file.")
            }
        }
    }

}

