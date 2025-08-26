package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BlockedUsersAdapter
import io.bidswipe.app.databinding.FragmentBlockedUsersBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
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
            unblockPos = pos
            userId = mList[pos]?.id?.toString() ?: return

            showUnblockConfirmation(mList[pos]?.name ?: "this user")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
          finish()
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
                        adapter.notifyItemRangeChanged(0, mList.size)

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
    private fun showUnblockConfirmation(userName: String) {
        AppBottomSheet(
            requireContext(),
            R.drawable.ic_block,
            "Unblock User",
            "Are you sure you want to unblock $userName?",
            primaryBtnText = "Unblock",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            iconPadding = 16,
            alertType = AlertType.INFO,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    bind.loader.isVisible = true
                    viewModel.blockUnblockUser(userId.toRequestBody())
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()

                    unblockPos = -1
                    userId = ""
                }
            }).show()
    }

}
