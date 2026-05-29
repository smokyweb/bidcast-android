package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BenefitsAdapter
import io.bidswipe.app.controller.RequirementAdapter
import io.bidswipe.app.databinding.FragmentPremierShopBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

@SuppressLint("NotifyDataSetChanged")
class PremierShopFragment : BaseFragment<SellerHubViewModel, FragmentPremierShopBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPremierShopBinding.inflate(inflater, view, false)

	private var gridList = mutableListOf<GetPremierShopResponse.Data.Feature?>()
	private var reqList = mutableListOf<GetPremierShopResponse.Data.Requirement?>()
	private lateinit var gridAdapter: BenefitsAdapter
	private lateinit var reqAdapter: RequirementAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		/* gridList.add(SellModel(R.drawable.ic_percent,0,"Reduced Commission","Pay only 5% commission on sales"))
		 gridList.add(SellModel(R.drawable.ic_finger_print,0,"Unique Profile ID", "Custom URL for your shop"))
		 gridList.add(SellModel(R.drawable.ic_speaker,0,"Marketing Boost", "Priority in search result"))
		 gridList.add(SellModel(R.drawable.ic_support,0,"Priority Support", "24/7 dedicated assistance"))*/

		bind.welcomeText.text = buildSpannedString {
			bold {
				scale(1.2f) {
					append("Welcome!\n")
				}
			}
			color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
				append("This is where you can monitor your key performance metrics, or any other issues on your account")
			}
		}

		bind.policyStandards.text = buildString {
			append("If you have Community Guidelines violations, they’ll display here. Violations typically remain on your account for 180 days. If you think a violation was issued in error, you can appeal by responding to the email from Trust & Safety with details about the violation.")
		}

		bind.becomeText.text = buildString {
			append("These are your key seller performance rates based on the past three months. These rates reflect the % of orders where buyers had positive experiences, with no seller-fault issues based on ship time or refunds and seller-driven cancellations.")
			append("\n\n")
			append("Premier Shop status helps buyers identify top-performing sellers. Keep in mind performance rates are rarely 100% because buyer issues covered under our Buyer Protection Policy are excluded.")
		}

		gridAdapter = BenefitsAdapter(gridList, mClick)
		bind.gridRecycler.adapter = gridAdapter

		reqAdapter = RequirementAdapter(reqList, mClick)
		bind.requirementRecycler.adapter = reqAdapter

		bind.loader.isVisible = true
		viewModel.getPremierShop()
		viewModel.getPremierShopRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					bind.loader.isVisible = false

					val mData = it.value.data

					gridList.clear()

					reqList.clear()

					if (mData?.features != null) {
						gridList.addAll(mData.features)
					}

					if (mData?.requirements != null) {
						reqList.addAll(mData.requirements)
					}
					gridAdapter.notifyDataSetChanged()
					reqAdapter.notifyDataSetChanged()

					// #14 [cmpbh03n500co34hgtycdwcbe]: rating sourced from shopOptions.rating (API) — NOT hardcoded. Confirmed.
					val ratingProg = (mData?.shopOptions?.rating ?: 0.0)
					val responseProg = (mData?.shopOptions?.response?.replace("%", "") ?: "0").toInt()
					val deliveryProg = (mData?.shopOptions?.delivery?.replace("%", "") ?: "0").toInt()

					bind.response.text = "$responseProg%"
					bind.delivery.text = "$deliveryProg%"
					bind.rating.text = ratingProg.toString()

					bind.ratingProgress.progress = (ratingProg * 10.0).toInt()
					bind.responseProgress.progress = responseProg
					// QA-FIX: delivery progress was using responseProg by mistake
					bind.deliveryProgress.progress = deliveryProg

					bind.reviewLogo.loadUrl(mCtx, mData?.reviewLogo ?: "")

					bind.reviewTitle.text = mData?.reviewTitle
//					bind.policyStanding.text = mData?.

					bind.reviewDetails.text = mData?.reviewDetails

					val progress = mData?.currentProgress?.replace("%", "")?.toInt() ?: 0

					bind.stepProgress.progress = progress

					// QA-FIX: previously the click listener was only attached when progress == 100,
					// which meant tapping the button did literally nothing for any seller below 100%.
					// Always attach a click handler and branch on eligibility inside it.
					if (progress < 100) {
						bind.applyBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mCtx, R.color.outlineVariant))
					} else {
						bind.applyBtn.backgroundTintList = null
					}

					bind.applyBtn.setHapticClickListener {
						if (progress >= 100) {
							bind.loader.isVisible = true
							viewModel.applyPremierShop()
						} else {
							AppBottomSheet(
								mCtx,
								R.drawable.ic_error,
								"Not Eligible Yet",
								"You are at ${progress}% of the Premier Shop requirements. Reach 100% to apply.",
								primaryBtnText = "Okay",
								secondaryBtnText = "Cancel",
								canCancel = true,
								showSecondary = false,
								iconPadding = 16,
								alertType = AlertType.ERROR,
								clicks = object : AlertClicks {
									override fun primaryClick(dialog: AppBottomSheet) {
										dialog.dismiss()
									}

									override fun secondaryClick(dialog: AppBottomSheet) {
										dialog.dismiss()
									}
								}
							).show()
						}
					}

					bind.progress.text = mData?.currentProgress

					bind.nextReview.text = buildString {
						append("Next Review in ")
						append(mData?.nextReview ?: 0)
						append(" days")
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

		viewModel.applyPremierShopRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.applyPremierShopRepo.value = null

					AppBottomSheet(
						mCtx,
						R.drawable.ic_success,
						"Premier Shop Applied",
						// MC (2026-05-28): blank-popup fix - never show an empty
						// success dialog; fall back when message is null/blank.
						it.value.message?.takeIf { m -> m.isNotBlank() } ?: "Your Premier Shop application has been submitted.",
						primaryBtnText = "Okay",
						secondaryBtnText = "Cancel",
						canCancel = true,
						showSecondary = false,
						iconPadding = 16,
						alertType = AlertType.SUCCESS,
						clicks = object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						}
					).show()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.applyPremierShopRepo.value = null
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