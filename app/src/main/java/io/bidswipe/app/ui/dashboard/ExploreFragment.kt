package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class ExploreFragment : BaseFragment<DashViewModel,FragmentExploreBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentExploreBinding.inflate(inflater,view,false)

    private lateinit var exploreAdapter : ExploreAdapter
    private var exploreList = mutableListOf<GetCategoryResponse.Data?>()

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            val category = exploreList[pos]?.name

            findNavController().navigate(
                ids.goTopExploreType,
                bundleOf("category" to category )
            )

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       exploreAdapter = ExploreAdapter(exploreList,mClick)
        bind.recycler.adapter = exploreAdapter

        bind.header.onMorePrimaryClick {
            startActivity(Intent(mCtx , NotificationActivity::class.java).putExtra("slug","notification"))
        }

        selectTab(bind.recommended)

        bind.recommended.setOnClickListener { selectTab(it as TextView) }
        bind.popular.setOnClickListener { selectTab(it as TextView) }
        bind.all.setOnClickListener { selectTab(it as TextView) }

        bind.loader.isVisible = true
        viewModel.getCategory()
        viewModel.getCategoryRepo.observe (viewLifecycleOwner){
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    if(it.value.data?.isNotEmpty()==true){
                        exploreList.clear()
                        exploreList.addAll(it.value.data)
                        exploreAdapter.notifyDataSetChanged()
                    }
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
        val tabs = listOf(bind.recommended, bind.popular, bind.all)
        tabs.forEach {
            it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            it.setTypeface(null, Typeface.NORMAL)
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTypeface(null, Typeface.BOLD)
    }


}