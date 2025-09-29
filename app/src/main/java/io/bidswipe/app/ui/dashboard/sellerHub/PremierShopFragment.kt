package io.bidswipe.app.ui.dashboard.sellerHub

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

@SuppressLint("NotifyDataSetChanged")
class PremierShopFragment : BaseFragment<SellerHubViewModel , FragmentPremierShopBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentPremierShopBinding.inflate(inflater , view , false)

	private var gridList = mutableListOf<GetPremierShopResponse.Data.Feature?>()
	private var reqList = mutableListOf<GetPremierShopResponse.Data.Requirement?>()
	private lateinit var gridAdapter : BenefitsAdapter
	private lateinit var reqAdapter : RequirementAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
		}

	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		/* gridList.add(SellModel(R.drawable.ic_percent,0,"Reduced Commission","Pay only 5% commission on sales"))
		 gridList.add(SellModel(R.drawable.ic_finger_print,0,"Unique Profile ID", "Custom URL for your shop"))
		 gridList.add(SellModel(R.drawable.ic_speaker,0,"Marketing Boost", "Priority in search result"))
		 gridList.add(SellModel(R.drawable.ic_support,0,"Priority Support", "24/7 dedicated assistance"))*/

		gridAdapter = BenefitsAdapter(gridList , mClick)
		bind.gridRecycler.adapter = gridAdapter

		reqAdapter = RequirementAdapter(reqList , mClick)
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

					bind.rating.text = mData?.shopOptions?.rating.toString()

					bind.response.text = mData?.shopOptions?.response ?: "N/A"

					bind.delivery.text = mData?.shopOptions?.delivery ?: "N/A"

					bind.reviewLogo.loadUrl(mCtx , mData?.reviewLogo ?: "")

					bind.reviewTitle.text = mData?.reviewTitle

					bind.reviewDetails.text = mData?.reviewDetails

					val progress = mData?.currentProgress?.replace("%" , "")?.toInt() ?: 0

					bind.stepProgress.progress = progress ?: 0

					if (progress < 100) {
						bind.applyBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mCtx , R.color.outlineVariant))
					}else if(progress==100){
						bind.applyBtn.setHapticClickListener {
							bind.loader.isVisible=true
							viewModel.applyPremierShop()
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
	
		viewModel.applyPremierShopRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.applyPremierShopRepo.value = null
					
					AppBottomSheet(
						mCtx,
						R.drawable.ic_success,
						"Premier Shop Applied",
						it.value.message?:"",
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
		
	}

}