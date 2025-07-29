package io.bidswipe.app.ui.dashboard.sellerProfile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentSellerShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.scheduleShow.LiveShowActivity
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SellerShowFragment : BaseFragment<SellerViewModel, FragmentSellerShowBinding>() {
    override fun getModel(): Class<SellerViewModel>  = SellerViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSellerShowBinding.inflate(inflater,view,false)

    private lateinit var showAdapter: HomeAdapter
    private var showList = mutableListOf<GetMyShowResponse.Data?>()

    private val mClicks = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {

            startActivity(Intent(mCtx, LiveShowActivity::class.java).putExtra("showId",
                showList.get(pos)?.id.toString()))

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showAdapter = HomeAdapter(showList, mClicks)

        bind.recycler.adapter = showAdapter

        bind.loader.isVisible = true
        viewModel.getMyScheduledShow("upcoming".request())

        viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
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