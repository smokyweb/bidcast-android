package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductTipsPagerAdapter
import io.bidswipe.app.databinding.FragmentProductTipsBinding
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.string

class ProductTipsFragment : BaseFragment<ScheduleShowViewModel,FragmentProductTipsBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentProductTipsBinding.inflate(inflater,view,false)

    private var productTipList = mutableListOf("","","")
    private lateinit var pagerAdapter: ProductTipsPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            if (bind.pager.currentItem == 0){
                findNavController().popBackStack()
            }else{
                bind.pager.currentItem -= 1
            }

        }

        bind.stepProgress.max = productTipList.size

        pagerAdapter = ProductTipsPagerAdapter(productTipList,"productTips")
        bind.pager.adapter = pagerAdapter

        bind.pager.isUserInputEnabled = false

        bind.continueBtn.setOnClickListener {
            if  (bind.pager.currentItem == productTipList.size-1){

                findNavController().navigate(ids.goToCreateProductFragment)

            }else{
                bind.pager.currentItem += 1
            }

        }

        bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                bind.stepProgress.progress = position+1

                bind.step.text = buildString {
                    append("Step ")
                    append(position+1)
                    append( " of ${productTipList.size}")
                }

                if (position == 2){
                    bind.continueBtn.text = resources.getString(string._continue)
                }else{
                    bind.continueBtn.text = resources.getString(string.continue_to_next_step)
                }

            }
        })

    }

}