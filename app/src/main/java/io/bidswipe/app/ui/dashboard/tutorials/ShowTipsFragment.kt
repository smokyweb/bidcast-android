package io.bidswipe.app.ui.dashboard.tutorials

import android.R.color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductTipsPagerAdapter
import io.bidswipe.app.databinding.FragmentShowTipsBinding
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.toScheduleShow

class ShowTipsFragment : BaseFragment<DashViewModel, FragmentShowTipsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentShowTipsBinding.inflate(inflater, view, false)

    private var productTipList = mutableListOf("", "", "")
    private lateinit var pagerAdapter: ProductTipsPagerAdapter

    private var type = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        type = arguments?.getString("type", "").toString()

        if (type == "liveTips") {
            bind.header.setHeaderText("Going Live Tips")
        }else if (type == "bringInBuyers"){
            bind.header.setHeaderText("Bring In Buyers")
            bind.continueBtn.setBackgroundColor(ContextCompat.getColor(mCtx,R.color.secondary))
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }


        bind.stepProgress.max = productTipList.size

        pagerAdapter = ProductTipsPagerAdapter(productTipList, type)
        bind.pager.adapter = pagerAdapter

        bind.pager.isUserInputEnabled = false

        bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                bind.stepProgress.progress = position + 1

                bind.step.text = buildString {
                    append("Step ")
                    append(position + 1)
                    append(" of ${productTipList.size}")
                }

                if (position == 2) {
                    bind.continueBtn.text = "Continue"
                } else {
                    bind.continueBtn.text = "Continue to Next Step"
                }

            }
        })

        bind.continueBtn.setOnClickListener {
            if (bind.pager.currentItem == productTipList.size - 1) {
                when (type) {
                    "showTips" -> {
                        startActivity(mCtx.toScheduleShow("tips"))
                    }
                    "liveTips" -> {
                        findNavController().navigate(ids.goToLiveRehearsalFragment)
                    }
                    else -> {

                    }
                }

            } else {
                bind.pager.currentItem += 1
            }

        }


        /* val adapter = SellAdapter(mList = tipsList, "getStarted",object: RecyclerClicks {
             override fun viewClick(pos: Int) {

             }

             override fun itemClick(pos: Int, status: String) {

             }

         })

         bind.recycler.adapter = adapter*/

        /* bind.continueBtn.setOnClickListener {
             startActivity(mCtx.toScheduleShow("tips"))
         }*/


    }

}