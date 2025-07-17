package io.bidswipe.app.ui.dashboard.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowListingAdapter
import io.bidswipe.app.databinding.FragmentShowsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.scheduleShow.LiveShowActivity
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toScheduleShow

class ShowsFragment :  BaseFragment<SellerHubViewModel, FragmentShowsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentShowsBinding.inflate(inflater,view,false)

    private lateinit var showAdapter: ShowListingAdapter

    private var showList = mutableListOf<GetMyShowResponse.Data?>()

    private val mClicks = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {

            startActivity(Intent(mCtx, LiveShowActivity::class.java).putExtra("showId",
                showList.get(pos)?.id.toString()))

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        showAdapter = ShowListingAdapter(showList, mClicks)

        bind.recycler.adapter = showAdapter

        bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {

                log("tab : ${tab?.text}   :  ${tab?.tag}")

               when(tab?.position){

                   0 ->{
                       showList.clear()
                       showAdapter.notifyDataSetChanged()
                       bind.loader.isVisible = true
                       viewModel.getMyScheduledShow("upcoming".request())
                   }

                   1->{
                       showList.clear()
                       bind.loader.isVisible = true
                       viewModel.getMyScheduledShow("past".request())

                   }



               }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        bind.addNewProduct.setOnClickListener {

            startActivity(mCtx.toScheduleShow(from = "dash"))

        }
        bind.swipeRefreshLayout.setOnRefreshListener {
            when(bind.tabs.isSelected){
                true ->{
                    viewModel.getMyScheduledShow("upcoming".request())
                }
                false ->{
                    viewModel.getMyScheduledShow("past".request())
                }
            }
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false

            when(bind.tabs.isSelected){
                true ->{
                    viewModel.getMyScheduledShow("upcoming".request())
                }
                false ->{
                    viewModel.getMyScheduledShow("past".request())
                }
            }
        }

        bind.loader.isVisible = true
        viewModel.getMyScheduledShow("upcoming".request())

        viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    mData?.forEach {

                        showList.add(it)

                        showAdapter.notifyDataSetChanged()

                    }

                    if (showList.isEmpty()){
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    }else{
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing =false
                    bind.noInternet.isVisible = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
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