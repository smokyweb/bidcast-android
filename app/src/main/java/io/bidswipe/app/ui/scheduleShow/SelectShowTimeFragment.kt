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
import com.google.android.material.chip.Chip
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

class SelectShowTimeFragment : BaseFragment<ScheduleShowViewModel , FragmentSelectShowTimeBinding>() {

	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentSelectShowTimeBinding.inflate(inflater , view , false)

	private var timeList = mutableListOf<String>()

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val from = activity?.intent?.getStringExtra("from").toString()

		bind.calenderView.setForwardButtonImage(ContextCompat.getDrawable(mCtx , draw.ic_forward) !!)
		bind.calenderView.setPreviousButtonImage(ContextCompat.getDrawable(mCtx , draw.ic_previous) !!)

		val today = Calendar.getInstance().apply {
			set(Calendar.HOUR_OF_DAY, 0)
			set(Calendar.MINUTE, 0)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}
		bind.calenderView.setMinimumDate(today)

		if (viewModel.date.isEmpty()) {
			val calendar = Calendar.getInstance()
			val date = Utils.getFormattedDateTime("dd-MM-yyyy" , "yyyy-MM-dd" , Utils.getDateFromTimestamp(calendar.timeInMillis))
			viewModel.date = date.toString()
		}

		updateTimeSlots()

		bind.calenderView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
			override fun onClick(calendarDay : CalendarDay) {

				val date = Utils.getFormattedDateTime("dd-MM-yyyy" , "yyyy-MM-dd" , Utils.getDateFromTimestamp(calendarDay.calendar.timeInMillis))
					.toString()

				viewModel.date = date

				updateTimeSlots()

			}
		})

		bind.header.onBackClick {
			if (from == "tutorial") finish() else findNavController().popBackStack()
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			val chipId = chipGroup.checkedChipId
			if (chipId == View.NO_ID) {
				viewModel.time = ""
				return@setOnCheckedStateChangeListener
			}

			val chip : Chip? = chipGroup.findViewById(chipId)
			chip?.let {
				viewModel.time = it.text.toString()
			}

		}

        bind.continueBtn.setHapticClickListener {

			when {
				viewModel.date.isEmpty() -> {
					Alerts.error(mCtx , "Please select a date for show")
				}

				viewModel.time.isEmpty() -> {
					Alerts.error(mCtx , "Please select a time for show")
				}

				else -> {
					if (from == "dash") {
						findNavController().navigate(R.id.ShowTimeFragment_to_selectCategoryFragment)
					} else {
						val data = Intent()
						data.putExtra("date" , viewModel.date)
						data.putExtra("time" , viewModel.time)
						activity?.setResult(Activity.RESULT_OK , data)
						finish()
					}
				}

			}

		}


	}

	private fun updateTimeSlots() {
		val slots = generateTimeSlots(viewModel.date)
		timeList.clear()
		timeList.addAll(slots)

		if (!timeList.contains(viewModel.time)) {
			viewModel.time = ""
		}

		bind.chipGroup.removeAllViews()

		timeList.forEach { time ->
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx ,
					text = time ,
					selected = time == viewModel.time,
					closeIconVisible = false
				)
			)
		}

		if (viewModel.time.isNotEmpty()) {
			bind.chipGroup.check(viewModel.time.hashCode())
		} else {
			bind.chipGroup.clearCheck()
		}
	}

	private fun generateTimeSlots(selectedDate : String?) : List<String> {
		if (selectedDate.isNullOrEmpty()) return emptyList()

		return try {
			val date = Utils.getSimpleDate("yyyy-MM-dd").parse(selectedDate) ?: return emptyList()

			val baseCalendar = Calendar.getInstance().apply {
				time = date
				set(Calendar.HOUR_OF_DAY , 0)
				set(Calendar.MINUTE , 0)
				set(Calendar.SECOND , 0)
				set(Calendar.MILLISECOND , 0)
			}

			val current = Calendar.getInstance()
			val slots = mutableListOf<String>()

			for (hour in 0 until 24) {
				val slot = baseCalendar.clone() as Calendar
				slot.set(Calendar.HOUR_OF_DAY , hour)
				slot.set(Calendar.MINUTE , 0)
				slot.set(Calendar.SECOND , 0)
				slot.set(Calendar.MILLISECOND , 0)

				val isSameDay = isSameDay(slot , current)
				if (isSameDay && !slot.after(current)) {
					continue
				}

				val displayTime = Utils.getSimpleDate("HH:mm").apply {
					timeZone = slot.timeZone
				}.format(slot.time)
				slots.add(displayTime.uppercase(Locale.getDefault()))
			}

			slots
		} catch (e : Exception) {
			Alerts.log("SelectShowTimeFragment" , "Failed to generate time slots: ${e.localizedMessage}")
			emptyList()
		}
	}

	private fun isSameDay(first : Calendar , second : Calendar) : Boolean {
		return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
			first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
	}

}