package io.bidcast.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.databinding.FragmentSelectShowTimeBinding
import io.bidcast.app.utils.Utils
import io.bidcast.app.utils.draw
import io.bidcast.app.utils.finish
import io.bidcast.app.utils.runSafe


class SelectShowTimeFragment : BaseFragment<ScheduleShowViewModel,FragmentSelectShowTimeBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSelectShowTimeBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from").toString()

        bind.calenderView.setForwardButtonImage(ContextCompat.getDrawable(mCtx,draw.ic_forward)!!)
        bind.calenderView.setPreviousButtonImage(ContextCompat.getDrawable(mCtx,draw.ic_previous)!!)

        bind.header.onBackClick{
           if (from =="tutorial") finish() else findNavController().popBackStack()
        }

        repeat(6){
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = "09:00 AM",
                    selected = false
                )
            )
        }

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }


    }

}