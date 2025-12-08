package io.bidswipe.app.ui.sellerHub

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
import io.bidswipe.app.controller.BenefitItem
import io.bidswipe.app.controller.ReferralBenefitsAdapter
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentAffiliateProgramBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class AffiliateProgramFragment : BaseFragment<SellerHubViewModel, FragmentAffiliateProgramBinding>() {

	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentAffiliateProgramBinding.inflate(inflater, view, false)

	private val mList = mutableListOf<SellModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		val buyerBenefits = mutableListOf(
			BenefitItem(
				R.drawable.ic_people,
				"Make Referrals",
				"Each referred buyer gets $15 to shop your shows, you get $5"
			),
			BenefitItem(R.drawable.ic_people, "Build your audience", "Referred buyers auto follow you and bookmark your next show"),
			BenefitItem(R.drawable.ic_people, "Prioritize your show", "When they join, your shows will be highlighted in their feed")
		)

		val buyerAdapter = ReferralBenefitsAdapter( buyerBenefits )
		bind.buyerBenefits.adapter = buyerAdapter


		val sellerBenefits = mutableListOf(
			BenefitItem(
				R.drawable.ic_people,
				"Share your invite link",
				"Send your referral link to friends and followers and invite them to sell"
			),
			BenefitItem(R.drawable.ic_people, "You earn \$100", "Receive \$100 after your referrals make their"),
			BenefitItem(R.drawable.ic_people, "They earn too", "Your referrals will earn a bonus of up to \$150 in matched earnings during their first week")
		)

		val sellerAdapter = ReferralBenefitsAdapter( sellerBenefits )
		bind.sellerBenefits.adapter = sellerAdapter


		val adapter = SellAdapter(mList = mList, "affiliate", object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

			}
		})

		bind.recycler.adapter = adapter

		bind.copyBtn.setHapticClickListener {

			val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			val clip = ClipData.newPlainText("label", bind.referralCode.text)
			clipboard.setPrimaryClip(clip)

		}

		bind.share.setHapticClickListener {

			val shareText = buildString {
				append(Const.BASE_URL)
				append("/referral-code?referralCode=${bind.referralCode.text}")
			}

			val shareIntent = Intent(Intent.ACTION_SEND).apply {
				type = "Text/*"
				putExtra(Intent.EXTRA_TEXT, shareText )
			}

			context?.startActivity(Intent.createChooser(shareIntent, "Share invite link"))
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
							SellModel(
								R.drawable.ic_dollar,
								R.color.secondaryContainer,
								"Earn $100 Reward",
								"When your referral makes their first sale"
							),
							SellModel(
								R.drawable.ic_gift,
								R.color.tertiaryContainer,
								"They Get Bonus Too!",
								"Your referrals get $100 matched earnings in their first week"
							)
						)
					)

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

	}

}