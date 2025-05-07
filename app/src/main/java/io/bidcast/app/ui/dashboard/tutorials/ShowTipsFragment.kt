package io.bidcast.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.SellAdapter
import io.bidcast.app.databinding.FragmentShowTipsBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.SellModel
import io.bidcast.app.ui.dashboard.DashViewModel
import io.bidcast.app.utils.finish
import io.bidcast.app.utils.ids
import io.bidcast.app.utils.toScheduleShow

class ShowTipsFragment : BaseFragment<DashViewModel,FragmentShowTipsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentShowTipsBinding.inflate(inflater,view,false)

    private var tipsList = mutableListOf<SellModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        tipsList.clear()
        tipsList.addAll(
            listOf(
                SellModel(R.drawable.ic_pencil,R.color.secondaryContainer,"Write an Engaging Title","Create a clear, descriptive title that captures attention. Keep it concise and relevant to your content."),
                SellModel(R.drawable.ic_calender,R.color.tertiaryContainer,"Schedule in Advance","Only sell authentic and legitimate productsPlan your shows ahead of time to maintain consistency and give your audience time to prepare."),
                SellModel(R.drawable.ic_image,R.color.successContainer,"Choose a Quality Thumbnail","Select an eye-catching thumbnail that represents your content well. Use high-resolution images."),
            )
        )

        val adapter = SellAdapter(mList = tipsList, "getStarted",object: RecyclerClicks {
            override fun viewClick(pos: Int) {

            }

            override fun itemClick(pos: Int, status: String) {

            }

        })

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            startActivity(mCtx.toScheduleShow("tips"))
        }


    }

}