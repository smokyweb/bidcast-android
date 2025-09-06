package io.bidswipe.app.ui.dashboard.scheduleShow

import android.app.Activity
import android.content.Intent
import android.icu.util.Calendar
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
import io.bidswipe.app.utils.runSafe

class SelectShowTimeFragment : BaseFragment<ScheduleShowViewModel , FragmentSelectShowTimeBinding>() {

	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentSelectShowTimeBinding.inflate(inflater , view , false)

	private var timeList = mutableListOf("07:00" , "09:00" , "11:00" , "13:00" , "15:00" , "17:00" , "19:00" , "21:00" , "23:00" , "01:00")

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val from = activity?.intent?.getStringExtra("from").toString()

		bind.calenderView.setForwardButtonImage(ContextCompat.getDrawable(mCtx , draw.ic_forward) !!)
		bind.calenderView.setPreviousButtonImage(ContextCompat.getDrawable(mCtx , draw.ic_previous) !!)

		if (viewModel.date.isEmpty()) {
			val calendar = Calendar.getInstance()
			val date = Utils.getFormattedDateTime("dd-MM-yyyy" , "yyyy-MM-dd" , Utils.getDateFromTimestamp(calendar.timeInMillis))
			viewModel.date = date.toString()
		}

		bind.calenderView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
			override fun onClick(calendarDay : CalendarDay) {

				val date = Utils.getFormattedDateTime("dd-MM-yyyy" , "yyyy-MM-dd" , Utils.getDateFromTimestamp(calendarDay.calendar.timeInMillis))
					.toString()

				viewModel.date = date

				log("DATE : $date")

			}
		})

		bind.header.onBackClick {
			if (from == "tutorial") finish() else findNavController().popBackStack()
		}

		timeList.forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx ,
					text = it ,
					selected = false
				)
			)
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))

				val chip : Chip = bind.chipGroup.getChildAt(index) as Chip

				viewModel.time = chip.text.toString()

			}
		}

		bind.continueBtn.setOnClickListener {

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

}