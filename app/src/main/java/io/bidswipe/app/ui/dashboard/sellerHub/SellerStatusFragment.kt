package io.bidswipe.app.ui.dashboard.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.StatusAdapter
import io.bidswipe.app.databinding.FragmentSellerStatusBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.SellerStatusResponse
import io.bidswipe.app.ui.dashboard.more.MoreActivity
import io.bidswipe.app.utils.finish

class SellerStatusFragment : BaseFragment<SellerHubViewModel, FragmentSellerStatusBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSellerStatusBinding.inflate(inflater, view, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        bind.contactButton.setOnClickListener {
            startActivity(Intent(mCtx, MoreActivity::class.java).putExtra("slug", "contactUs"))

        }

        viewModel.getSellerStatus()
        viewModel.getSellerStatusRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    val marketplaceVendor = mData?.marketplaceVendor
                    val liveSellVendor = mData?.liveSellVendor

                    if (marketplaceVendor != null) {
                        bind.vendor.status.text = marketplaceVendor.status
                        bind.vendor.title.text = marketplaceVendor.title
                        bind.vendor.subTitle.text = "Seller Rating: ${marketplaceVendor.sellerRating}/5"
                    }
                    if (liveSellVendor != null) {
                        bind.sender.status.text = liveSellVendor.status
                        bind.sender.title.text = liveSellVendor.title
                        bind.sender.subTitle.text = "Submitted: ${liveSellVendor.submitted}"


                    }
                }

                is Resource.Error -> {
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                    }
                }

                else -> {}

            }
        }

    }

}