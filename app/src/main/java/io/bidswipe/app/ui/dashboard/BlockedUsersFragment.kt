package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BlockedUsersAdapter
import io.bidswipe.app.databinding.FragmentBlockedUsersBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import okhttp3.RequestBody.Companion.toRequestBody

@SuppressLint("NotifyDataSetChanged")
class BlockedUsersFragment : BaseFragment<DashViewModel, FragmentBlockedUsersBinding>() {

    override fun getModel() = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentBlockedUsersBinding.inflate(inflater, view, false)

    private var mList = mutableListOf<GetBlockedUsersResponse.Data.BlockedByMe?>()

    private lateinit var adapter: BlockedUsersAdapter
    private var unblockPos = -1

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            bind.loader.isVisible = true
            unblockPos = pos
            val userId = mList[pos]?.id?.toString() ?: return
            val requestBody = userId.toRequestBody()
            viewModel.blockUnblockUser(requestBody)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }
        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.getBlockedUsers()
        }
        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            viewModel.getBlockedUsers()
        }

        adapter = BlockedUsersAdapter(mList, mClick)
        bind.recycler.adapter = adapter
        bind.loader.isVisible = true

        viewModel.blockUnblockUserRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    if (unblockPos != -1 && unblockPos < mList.size) {
                        mList.removeAt(unblockPos)
                        adapter.notifyItemRemoved(unblockPos)

                        if (mList.isEmpty()) {
                            bind.noData.isVisible = true
                            bind.recycler.isVisible = false
                        }
                        successToast("User unblocked successfully")
                        unblockPos = -1
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    unblockPos = -1
                    errorToast("Something went wrong")
                }

                else -> {}
            }
        }

        viewModel.getBlockedUsers()
        viewModel.getBlockedUsersRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    val mData = it.value.data
                    if (mData != null) {
                        mList.clear()
                        mList.addAll(mData.blockedByMe ?: mutableListOf())
                        adapter.notifyDataSetChanged()
                    }
                    if (mList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }
                    adapter.notifyDataSetChanged()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
                    } else {
                        errorToast("Something went wrong")
                    }
                }
                else -> {}
            }
        }

    }

}
