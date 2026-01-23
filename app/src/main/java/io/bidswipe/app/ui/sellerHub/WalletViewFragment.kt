package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PayoutAdapter
import io.bidswipe.app.databinding.FragmentWalletViewBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.PayoutHistoryResponse
import io.bidswipe.app.network.response.WalletInfoResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class WalletViewFragment : BaseFragment<SellerHubViewModel , FragmentWalletViewBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentWalletViewBinding.inflate(inflater , view , false)

	private var itemList = mutableListOf<PayoutHistoryResponse.Data?>()
	private lateinit var adapter : PayoutAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
		}
	}

	private var walletData: WalletInfoResponse.Data? =null

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		adapter = PayoutAdapter(itemList , mClick)
		bind.recycler.adapter = adapter
		bind.payoutCard.setHapticClickListener {
//			if (!kycStatus) {
//				Alerts.error(mCtx, "Please Complete Your KYC")
//			} else {
			findNavController().navigate(ids.goToPayoutFragment, bundleOf("amount" to walletData?.avaiableForPayout.toString()))
//			}
		}
		val parentSwipe = requireActivity().findViewById<SwipeRefreshLayout>(
			io.bidswipe.app.R.id.swipeRefreshLayout
		)

		bind.scroll.viewTreeObserver.addOnScrollChangedListener {
			val canScrollUp = bind.scroll.canScrollVertically(- 1)
			parentSwipe?.isEnabled = ! canScrollUp
		}

		bind.loader.isVisible = false
		viewModel.getPayoutHistory()
		viewModel.walletInfo()
		viewModel.getPayoutHistoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					if (it.value.currentPage == 1) {
						itemList.clear()
					}
					if (it.value.data?.isNotEmpty() == true) {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
						itemList.addAll(it.value.data)
					} else {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(mCtx , TAG , object : AlertClicks {
						override fun primaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}
					})
				}

				else -> {}

			}
		}

		viewModel.walletInfoRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					walletData = it.value.data
					bind.walletAmount.text = walletData?.avaiableBalance.toString().asMoney()
					bind.available.text = walletData?.avaiableForPayout.toString().asMoney()
					bind.processing.text = walletData?.processing.toString().asMoney()

					bind.payoutCard.isVisible = (walletData?.avaiableForPayout ?: 0.0) > 10

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(mCtx , TAG , object : AlertClicks {
						override fun primaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}
					})
				}

				else -> {}

			}
		}

		/*		viewModel.checkKyc()
				viewModel.checkKycRepo.observe(viewLifecycleOwner) {
					when (it) {
						is Resource.Success -> {
							bind.loader.isVisible = false
							val mData = it.value.data

							kycStatus = mData?.kycStatus == "active"

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
				}*/

	}

}