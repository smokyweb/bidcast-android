package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HowToSellPagerAdapter
import io.bidswipe.app.controller.ProductTipsPagerAdapter
import io.bidswipe.app.databinding.FragmentHowToSellBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.string
import io.bidswipe.app.utils.toScheduleShow

class HowToSellFragment : BaseFragment<DashViewModel,FragmentHowToSellBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentHowToSellBinding.inflate(inflater,view,false)

    private var tipList = mutableListOf<GetHowToSellResponse.Data?>()
    private lateinit var pagerAdapter: HowToSellPagerAdapter

    private var type =""

    private var tipPos = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        type = activity?.intent?.getStringExtra("slug") ?:""


        bind.header.onBackClick{
            if (type.isEmpty()){
                findNavController().popBackStack()
            }else{
                finish()
            }

        }

//        bind.next.setOnClickListener {
//            findNavController().navigate(ids.prepareYourShowFragment)
//        }
//
//        bind.back.setOnClickListener {
//            findNavController().popBackStack()
//        }



        pagerAdapter = HowToSellPagerAdapter(tipList)
        bind.pager.adapter = pagerAdapter

        bind.pager.isUserInputEnabled = false

        bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                bind.track.progress = position + 1

                bind.step.text = buildString {
                    append("Step ")
                    append(position + 1)
                    append(" of ${tipList.size}")
                }


            }
        })

        bind.nextBtn.setOnClickListener {

            log("ITEM : ${bind.pager.currentItem}")
            if (bind.pager.currentItem == tipList.size -1 ) {
                if (type.isEmpty()){
                    findNavController().navigate(ids.prepareYourShowFragment)
                }else{
                    finish()
                }
            } else {
                bind.pager.currentItem += 1
            }

        }

        bind.backBtn.setOnClickListener {
            if (bind.pager.currentItem == 0 ) {
                findNavController().popBackStack()
            } else {
                bind.pager.currentItem -= 1
            }

        }


        bind.loader.isVisible = true

        viewModel.getHowToSellStep()
        viewModel.getHowToSellStepRepo.observe (viewLifecycleOwner){
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    tipList.clear()

                    mData?.forEach {

                        tipList.add(it)

                    }

                    bind.track.max = tipList.size

                    bind.track.progress = 1

                    bind.step.text = "Step 1 of ${tipList.size} "

                    pagerAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }





    }

}