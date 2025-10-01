package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PayoutAdapter
import io.bidswipe.app.databinding.FragmentWalletViewBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.PayoutHistoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class WalletViewFragment : BaseFragment<SellerHubViewModel, FragmentWalletViewBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentWalletViewBinding.inflate(inflater, view, false)

	private var itemList = mutableListOf<PayoutHistoryResponse.Data?>()

	private lateinit var adapter: PayoutAdapter

	private var kycStatus = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		adapter = PayoutAdapter(itemList, mClick)
		bind.recycler.adapter = adapter
		bind.payoutCard.setHapticClickListener {
//			if (!kycStatus) {
//				Alerts.error(mCtx, "Please Complete Your KYC")
//			} else {
			findNavController().navigate(ids.goToPayoutFragment)
//			}
		}

		App.profileResponse.observe(viewLifecycleOwner) {
			val walletAmount = it?.walletAmount.toString().toDouble()
			bind.walletAmount.text = walletAmount.toString().asMoney()
			bind.available.text = walletAmount.toString().asMoney()
		}

		bind.loader.isVisible = false
		viewModel.getPayoutHistory()
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
					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
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