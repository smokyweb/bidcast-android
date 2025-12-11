package io.bidswipe.app.ui.scheduleShow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSelectShowTimeBinding
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.setHapticClickListener
import java.util.Calendar
import java.util.Locale

class SelectShowTimeFragment :
    BaseFragment<ScheduleShowViewModel, FragmentSelectShowTimeBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSelectShowTimeBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from").toString()

        bind.calenderView.setForwardButtonImage(ContextCompat.getDrawable(mCtx, draw.ic_forward)!!)
        bind.calenderView.setPreviousButtonImage(
            ContextCompat.getDrawable(
                mCtx,
                draw.ic_previous
            )!!
        )

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        bind.calenderView.setMinimumDate(today)

        if (viewModel.date.isEmpty()) {
            val calendar = Calendar.getInstance()
            val date = Utils.getFormattedDateTime(
                "dd-MM-yyyy",
                "yyyy-MM-dd",
                Utils.getDateFromTimestamp(calendar.timeInMillis)
            )
            viewModel.date = date.toString()
        }

        bind.calenderView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                val date = Utils.getFormattedDateTime(
                    "dd-MM-yyyy",
                    "yyyy-MM-dd",
                    Utils.getDateFromTimestamp(calendarDay.calendar.timeInMillis)
                ).toString()

                viewModel.date = date

                if (viewModel.time.isNotEmpty()) {
                    val selectedDate = Utils.getSimpleDate("yyyy-MM-dd").parse(viewModel.date)
                    val savedTimeDate = Utils.getSimpleDate("HH:mm").parse(viewModel.time)

                    if (savedTimeDate != null && selectedDate != null) {
                         val savedTimeCalendar = Calendar.getInstance().apply {
                            time = savedTimeDate
                        }

                        val savedDateTime = Calendar.getInstance().apply {
                            time = selectedDate
                            set(Calendar.HOUR_OF_DAY, savedTimeCalendar.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, savedTimeCalendar.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val now = Calendar.getInstance()
                        if (savedDateTime.before(now)) {
                            viewModel.time = ""
                            bind.time.setText("")
                        }
                    }
                }
            }
        })

        bind.header.onBackClick {
            if (from == "tutorial") finish() else findNavController().popBackStack()
        }

        // Restore time from ViewModel if available
        if (viewModel.time.isNotEmpty()) {
            try {
                val formattedTime = Utils.getFormattedDateTime("HH:mm", "hh:mm a", viewModel.time)
                if (formattedTime != null && formattedTime != viewModel.time) {
                    bind.time.setText(formattedTime)
                } else {
                    // If conversion fails, try direct parsing
                    val timeParts = viewModel.time.split(":")
                    if (timeParts.size == 2) {
                        val hour = timeParts[0].toIntOrNull()
                        val minute = timeParts[1].toIntOrNull()
                        if (hour != null && minute != null) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            bind.time.setText(
                                Utils.getSimpleDate("hh:mm a").format(cal.time)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bind.time.setOnClickListener {
            showTimePicker()
        }
        bind.timeBox.setOnClickListener {
            showTimePicker()
        }

        bind.continueBtn.setHapticClickListener {
            when {
                viewModel.date.isEmpty() -> {
                    Alerts.error(mCtx, "Please select a date for show")
                }

                viewModel.time.isEmpty() -> {
                    Alerts.error(mCtx, "Please select a time for show")
                }

                else -> {
                    if (from == "dash") {
                        findNavController().navigate(R.id.ShowTimeFragment_to_selectCategoryFragment)
                    } else {
                        val data = Intent()
                        data.putExtra("date", viewModel.date)
                        data.putExtra("time", viewModel.time)
                        activity?.setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
        }
    }

    private fun showTimePicker() {
        // Parse the selected date
        val selectedDate = Utils.getSimpleDate("yyyy-MM-dd").parse(viewModel.date)
            ?: Calendar.getInstance().time
        
        val selectedDateCalendar = Calendar.getInstance().apply {
            time = selectedDate
        }
        
        val now = Calendar.getInstance()
        val isToday = selectedDateCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                selectedDateCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        
        // Calculate minimum time (current time + 1 minute if today, or start of day if future)
        val minTime = if (isToday) {
            Calendar.getInstance().apply {
                add(Calendar.MINUTE, 1)
            }
        } else {
            Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        
        // Initialize time picker with saved time or minimum time
        var initialHour = minTime.get(Calendar.HOUR_OF_DAY)
        var initialMinute = minTime.get(Calendar.MINUTE)
        
        if (viewModel.time.isNotEmpty()) {
            try {
                val savedTime = Utils.getSimpleDate("HH:mm").parse(viewModel.time)
                if (savedTime != null) {
                    val savedCalendar = Calendar.getInstance().apply {
                        time = savedTime
                    }
                    val savedDateTime = Calendar.getInstance().apply {
                        time = selectedDate
                        set(Calendar.HOUR_OF_DAY, savedCalendar.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, savedCalendar.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    // Use saved time if it's in the future, otherwise use minimum time
                    if (savedDateTime.after(minTime) || savedDateTime == minTime) {
                        initialHour = savedCalendar.get(Calendar.HOUR_OF_DAY)
                        initialMinute = savedCalendar.get(Calendar.MINUTE)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val materialTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val newHour: Int = materialTimePicker.hour
            val newMinute: Int = materialTimePicker.minute

            // Create selected date-time with the chosen time
            val selectedDateTime = Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, newHour)
                set(Calendar.MINUTE, newMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Validate that selected time is in the future
            if (selectedDateTime.before(now)) {
                Alerts.error(mCtx, "Please select a future time")
            } else {
                // Save time in HH:mm format to ViewModel
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", newHour, newMinute)
                viewModel.time = timeString
                
                // Display time in hh:mm a format
                val displayTime = Utils.getSimpleDate("hh:mm a").format(selectedDateTime.time)
                bind.time.setText(displayTime)
            }
        }

        materialTimePicker.show(childFragmentManager, "tag")
    }
}