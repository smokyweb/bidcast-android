package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductAdapter
import io.bidswipe.app.databinding.FragmentAddProductBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class AddProductFragment : BaseFragment<ScheduleShowViewModel,FragmentAddProductBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentAddProductBinding.inflate(inflater,view,false)

    private lateinit var productAdapter :ProductAdapter
    private var mList = mutableListOf("","")

    private var mClick = object : RecyclerClicks{
                override fun itemClick(pos: Int, status: String?) {
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        productAdapter = ProductAdapter(mList,mClick)

        bind.recycler.adapter = productAdapter

        bind.finishBtn.setOnClickListener {
            finish()
        }

    }

}