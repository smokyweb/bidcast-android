package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SavedItemAdapter
import io.bidswipe.app.databinding.FragmentSavedItemsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SavedItemsFragment : BaseFragment<DashViewModel,FragmentSavedItemsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSavedItemsBinding.inflate(inflater, view , false)

    private lateinit var savedAdapter : SavedItemAdapter
    private var mList = mutableListOf<GetProductsByStatusResponse.Data?>()
    private var page = 1
    private var isLoading = false

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedAdapter = SavedItemAdapter(mList,mClick)

        bind.recycler.adapter = savedAdapter

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (mList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        viewModel.getSavedProductsByStatus("saved".request(),page.toString().request())
                    }
                }
            }
        })
        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            viewModel.getSavedProductsByStatus("saved".request(),page.toString().request())
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            viewModel.getSavedProductsByStatus("saved".request(),page.toString().request())
        }

        bind.loader.isVisible = true

        viewModel.getSavedProductsByStatus("saved".request(),"1".request())
        viewModel.getSavedProductsByStatusRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible =false
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    if (page==1){
                        mList.clear()
                    }

                    if (mData != null){
                        mList.addAll(mData)
                    }

                    if (mList.isEmpty()){
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    }else{
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    savedAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
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