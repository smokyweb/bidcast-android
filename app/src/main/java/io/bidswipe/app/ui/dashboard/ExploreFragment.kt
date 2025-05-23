package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class ExploreFragment : BaseFragment<DashViewModel,FragmentExploreBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentExploreBinding.inflate(inflater,view,false)

    private lateinit var exploreAdapter : ExploreAdapter
    private var exploreList = mutableListOf<GetCategoryResponse.Data?>()

    private val mClick = object : RecyclerClicks{
               override fun itemClick(pos: Int, status: String?) {
            findNavController().navigate(ids.goTopExploreType)
            
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       exploreAdapter = ExploreAdapter(exploreList,mClick)
        bind.recycler.adapter = exploreAdapter

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

}