package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentExploreTypeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.dashboard.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

class ExploreTypeFragment : BaseFragment<DashViewModel,FragmentExploreTypeBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentExploreTypeBinding.inflate(inflater,view,false)

    private lateinit var homeAdapter: HomeAdapter
    private var categoriesList = mutableListOf<String>()
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private var romIdsList = mutableListOf<StreamModel>()
    private var category = ""

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when (status) {
                "user" -> {
                    startActivity(Intent(mCtx, SellerProfileActivity::class.java).putExtra("userId",
                        showList[pos]?.userId.toString()
                    ))
                }

                "viewShow" ->{

                    if (showList[pos]?.isLive == true){
                        startActivity(Intent(mCtx, ViewLiveShowActivity::class.java).putExtra("position", pos).putParcelableArrayListExtra("roomIdsList", romIdsList as ArrayList))
                    }
                }
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = arguments?.getString("category") ?:""

        bind.header.onBackClick {

            findNavController().popBackStack()

        }

        bind.header.onMorePrimaryClick {
            startActivity(Intent(mCtx , NotificationActivity::class.java).putExtra("slug","notification"))
        }

        homeAdapter = HomeAdapter(showList, mClick)

        bind.recycler.adapter = homeAdapter

        selectTab(bind.live)

        bind.live.setOnClickListener { selectTab(it as TextView) }
        bind.popular.setOnClickListener { selectTab(it as TextView) }
        bind.comingSoon.setOnClickListener { selectTab(it as TextView) }

        categoriesList = mutableListOf(category)
        categoriesList.forEach {
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = it,
                    selected = true
                )
            )
        }

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }


       /* repeat(1){
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = "Gaming",
                    selected = true
                )
            )
        }*/

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }

        viewModel.getLiveShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    mData?.forEach {
                        romIdsList.add(StreamModel(it?.roomId.toString(),""))
                    }

                    showList.clear()

                    mData?.forEach {
                        showList.add(it)
                    }

                    if (showList.isEmpty()){
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    }else{
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }


                    homeAdapter.notifyDataSetChanged()

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

    fun selectTab(selectedTab: TextView) {
        val tabs = listOf(bind.live, bind.popular, bind.comingSoon)
        tabs.forEach {
            it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            it.setTypeface(null, Typeface.NORMAL)
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTypeface(null, Typeface.BOLD)

        bind.loader.isVisible = true

        when(selectedTab){
            bind.live ->{
                viewModel.getLiveShow("live".request(),category.request())
            }

            bind.popular ->{
                viewModel.getLiveShow("popular".request(), category.request())
            }

            bind.comingSoon ->{
                viewModel.getLiveShow("upcoming".request(), category.request())
            }

        }
    }

}