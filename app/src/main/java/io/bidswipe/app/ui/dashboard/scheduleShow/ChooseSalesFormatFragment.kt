package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FormatAdapter
import io.bidswipe.app.databinding.FragmentChooseSalesFormatBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.FormatModel
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.ids

class ChooseSalesFormatFragment : BaseFragment<ScheduleShowViewModel, FragmentChooseSalesFormatBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentChooseSalesFormatBinding.inflate(inflater, view, false)

    private var formatList = mutableListOf<FormatModel>()
    private lateinit var adapter: FormatAdapter
    private var productData: Bundle? = null

    private val mClick = object : RecyclerClicks {

        override fun itemClick(pos: Int, status: String?) {

            formatList.forEachIndexed { index, formatModel ->
                formatModel.selected = index == pos
            }

            bind.offerLayout.isVisible = pos == 1

            adapter.notifyDataSetChanged()

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(arguments!= null){
           productData =  requireArguments()
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        formatList.add(FormatModel(draw.ic_hammer, "Auction"))
        formatList.add(FormatModel(draw.ic_tag, "Buy It Now"))

        adapter = FormatAdapter(formatList, mClick)

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            val selectedFormat = formatList.firstOrNull { it.selected == true }?.title ?: ""
            val bundle = productData
            bundle?.putString("salesFormat",selectedFormat)
            bundle?.putString("price", bind.bidPrice.text.toString().trim())

            findNavController().navigate(ids.goToProductWeightFragment, bundle)
        }
    }

}