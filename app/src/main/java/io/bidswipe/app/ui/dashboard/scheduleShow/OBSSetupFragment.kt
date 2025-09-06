package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOBSSetupBinding

class OBSSetupFragment : BaseFragment<ScheduleShowViewModel , FragmentOBSSetupBinding>() {
	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentOBSSetupBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)


	}

}