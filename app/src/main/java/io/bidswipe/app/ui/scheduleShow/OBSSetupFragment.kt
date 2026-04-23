package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOBSSetupBinding

class OBSSetupFragment : BaseFragment<ScheduleShowViewModel , FragmentOBSSetupBinding>() {
	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentOBSSetupBinding.inflate(inflater , view , false)

}