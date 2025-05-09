package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FormatAdapter
import io.bidswipe.app.databinding.FragmentChooseSalesFormatBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.FormatModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids

class ChooseSalesFormatFragment : BaseFragment<ScheduleShowViewModel,FragmentChooseSalesFormatBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentChooseSalesFormatBinding.inflate(inflater,view,false)

    private var formatList = mutableListOf<FormatModel>()
    private lateinit var adapter : FormatAdapter

    private val mClick = object : RecyclerClicks{
        override fun viewClick(pos: Int) {

            formatList.forEachIndexed { index, formatModel ->
                formatModel.selected = index == pos
            }

            bind.offerLayout.isVisible = pos == 1

            adapter.notifyDataSetChanged()

        }

        override fun itemClick(pos: Int, status: String) {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        formatList.add(FormatModel(draw.ic_hammer,"Auction"))
        formatList.add(FormatModel(draw.ic_tag,"Buy It Now"))

        adapter = FormatAdapter(formatList,mClick)

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            findNavController().navigate(ids.goToProductWeightFragment)
        }

    }

}