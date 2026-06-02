package io.bidswipe.app.ui.sellerProfile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentSellerShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.upcomingshow.UpcomingShowDetailsActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SellerShowFragment : BaseFragment<SellerViewModel, FragmentSellerShowBinding>() {
    override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSellerShowBinding.inflate(inflater, view, false)

    private lateinit var showAdapter: HomeAdapter
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private var page = 1
    private var isLoading = false
    private var sellerId: String? = ""

    private val mClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            val show = showList.getOrNull(pos) ?: return
            when (status) {
                "user" -> {
                    startActivity(
                        Intent(mCtx, SellerProfileActivity::class.java)
                            .putExtra("sellerId", show.userId.toString())
                    )
                }
                // Basecamp #1/#9: the show tile tap was a dead no-op. Live
                // shows open the live screen; upcoming shows open the
                // upcoming-show detail screen (parity with Home/Explore).
                else -> {
                    if (show.isLive == true) {
                        startActivity(
                            Intent(mCtx, ViewLiveShowActivity::class.java)
                                .putExtra("roomId", show.roomId.toString())
                                .putExtra("userId", show.userId.toString())
                        )
                    } else {
                        startActivity(
                            Intent(mCtx, UpcomingShowDetailsActivity::class.java)
                                .putExtra("show_id", show.id?.toString())
                        )
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (Utils.isOnline(mCtx)) {
            bind.noInternet.isVisible = false
            bind.loader.isVisible = true
            page = 1
            isLoading = false
            viewModel.getMyScheduledShow(sellerId?.request(),"upcoming".request(), page.toString().request())
        } else {
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            bind.loader.isVisible = false
            bind.noInternet.isVisible = true
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sellerId = (requireActivity() as SellerProfileActivity).sellerId

        showAdapter = HomeAdapter(showList, mClicks)
        val gridLm = bind.recycler.layoutManager as GridLayoutManager
        gridLm.setSpanCount(if (resources.isTablet()) 3 else 2)
        bind.recycler.adapter = showAdapter

        // Basecamp #9: infinite scroll / load-more. Previously only page 1 was
        // fetched, so sellers with many upcoming shows showed just the first
        // page. Fetch the next page (same seller_id) as the user nears the end.
        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val lastVisible = gridLm.findLastVisibleItemPosition()
                if (!isLoading && lastVisible >= showList.size - 1 && showList.isNotEmpty()) {
                    isLoading = true
                    page++
                    viewModel.getMyScheduledShow(
                        sellerId?.request(),
                        "upcoming".request(),
                        page.toString().request()
                    )
                }
            }
        })
        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            isLoading = false
            viewModel.getMyScheduledShow(sellerId?.request(),"upcoming".request(), page.toString().request())
        }

        bind.loader.isVisible = true
        page = 1
        isLoading = false
        viewModel.getMyScheduledShow(sellerId?.request(),"upcoming".request(), page.toString().request())

        viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.noInternet.isVisible = false

                    val mData = it.value.data
                    if (page == 1) {
                        showList.clear()
                    }
                    if (mData != null) {
                        showList.addAll(mData)
                    }

                    if (showList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }
                    isLoading = page >= (it.value.totalPage ?: 0)
                    showAdapter.notifyDataSetChanged()
                }

                is Resource.Error -> {
                    bind.noData.isVisible = false
                    bind.loader.isVisible = false

                    // Roll back a failed load-more page so the next scroll can
                    // retry instead of permanently skipping a page.
                    if (page > 1) page--
                    isLoading = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
//                        errorToast(getString(R.string.no_internet))
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