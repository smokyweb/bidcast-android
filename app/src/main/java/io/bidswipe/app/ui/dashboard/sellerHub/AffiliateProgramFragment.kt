package io.bidswipe.app.ui.dashboard.sellerHub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentAffiliateProgramBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class AffiliateProgramFragment : BaseFragment<SellerHubViewModel , FragmentAffiliateProgramBinding>() {

	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentAffiliateProgramBinding.inflate(inflater , view , false)

	private val mList = mutableListOf<SellModel>()

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		val adapter = SellAdapter(mList = mList , "affiliate" , object : RecyclerClicks {

			override fun itemClick(pos : Int , status : String?) {

			}

		})

		bind.recycler.adapter = adapter

        bind.copyBtn.setHapticClickListener {

			val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			val clip = ClipData.newPlainText("label" , bind.referralCode.text)
			clipboard.setPrimaryClip(clip)

		}

        bind.share.setHapticClickListener {
			val shareIntent = Intent(Intent.ACTION_SEND).apply {
				type = "Text/*"
				putExtra(Intent.EXTRA_TEXT , "https://play.google.com/store/apps/details?id=io.bidswipe.app&referrer=${bind.referralCode.text}")
			}

			context?.startActivity(Intent.createChooser(shareIntent , "Share invite link"))
		}

		bind.loader.isVisible = true

		viewModel.fetchReferral()

		viewModel.fetchReferralRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data

					bind.referralCode.text = mData?.referralCode
					bind.referralCount.text = mData?.totalReferred.toString()
					bind.totalEarning.text = mData?.totalEarnings.toString().asMoney()

					mList.clear()
					mList.addAll(
						listOf(
							SellModel(R.drawable.ic_dollar , R.color.secondaryContainer , "Earn $100 Reward" , "When your referral makes their first sale") ,
							SellModel(
								R.drawable.ic_gift ,
								R.color.tertiaryContainer ,
								"They Get Bonus Too!" ,
								"Your referrals get $100 matched earnings in their first week"
							)
						)
					)

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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