package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.databinding.BuyNowSheetBinding
import io.bidswipe.app.databinding.FragmentProductDetailsBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

class ProductDetailsFragment : BaseFragment<DashViewModel, FragmentProductDetailsBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentProductDetailsBinding.inflate(inflater,view,false)

	private var productId = ""
	private var price = ""
	private var selectedPrice = ""

	private var offerList = mutableListOf<OfferModel>()

//	private lateinit var makeOfferSheetBind : MakeOfferSheetBinding
	private lateinit var offerAdapter : MakeOfferAdapter
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		productId = activity?.intent?.getStringExtra("productId") ?:""
		
		bind.header.onBackClick {
			finish()
		}
		
		bind.buyNow.setOnClickListener {
			var buyNowSheetBind = BuyNowSheetBinding.bind(layoutInflater.inflate(R.layout.buy_now_sheet, null, false))
			var buyNowSheet = Alerts.appBottomSheet(mCtx, true, buyNowSheetBind)

			buyNowSheetBind.cardNumber.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(mCtx,draw.ic_visa), null, null, null)

			buyNowSheetBind.confirmButton.setOnClickListener {

				buyNowSheet.dismiss()

				findNavController().navigate(ids.goToSendGiftFragment)

			}
			
			buyNowSheet.show()
		}
		
		bind.makeOffer.setOnClickListener {
			showOfferSheet()
		}


		bind.loader.isVisible = true
		viewModel.getProductDetails(productId.request())
		viewModel.getProductDetailsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					bind.userName.text = mData?.user?.name

					if (mData?.user?.sellerVerification == true) {
						bind.sellerStatus.text = "Verified Seller"
					} else {
						bind.sellerStatus.text = "Unverified Seller"
					}

					bind.userImage.loadUrl(mCtx, mData?.user?.profileImage.toString())

					bind.productImage.loadUrl(mCtx, mData?.images?.get(0).toString())

					bind.productName.text = mData?.title

					price = mData?.pricing.toString()

					bind.price.text = buildString {
						append("$")
						append(price)
					}

					offerList.clear()

					offerList.add(
						OfferModel(
							getDiscountAmount(mData?.pricing?:0, 20),
							"20% off"
						)
					)

					offerList.add(
						OfferModel(
							getDiscountAmount(mData?.pricing ?: 0, 15),
							"15% off"
						)
					)
					offerList.add(
						OfferModel(
							getDiscountAmount(mData?.pricing ?: 0, 10),
							"10% off"
						)
					)
					offerList.add(
						OfferModel(
							getDiscountAmount(mData?.pricing ?: 0, 5),
							"5% off"
						)
					)

					bind.address.text = mData?.shippingAdress?.streetAddress

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

		viewModel.makeOfferRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					Alerts.success(mCtx,"Offer Sent")

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

	fun getDiscountAmount(originalAmount: Int, percentOff: Int): String {

		val discountedAmount = originalAmount - (originalAmount*percentOff)/100

		return discountedAmount.toString()
	}

	fun showOfferSheet(){
		var makeOfferSheetBind = MakeOfferSheetBinding.bind(layoutInflater.inflate(R.layout.make_offer_sheet, null, false))
		var makeOfferSheet = Alerts.appBottomSheet(mCtx, true, makeOfferSheetBind)

		makeOfferSheetBind.listedPrice.text = price.asMoney()

		makeOfferSheetBind.offerRecycler.adapter = MakeOfferAdapter(offerList,object : RecyclerClicks{

			override fun itemClick(pos: Int, status: String?) {

				makeOfferSheetBind.customOffer.setText(offerList[pos].amount)

				offerList.forEachIndexed { index , item ->
					item.selected = index == pos
				}

				makeOfferSheetBind.offerRecycler.adapter?.notifyDataSetChanged()
			}
		})

		makeOfferSheetBind.close.setOnClickListener {
			makeOfferSheet.dismiss()
		}

		makeOfferSheetBind.select.setOnClickListener {

			if (makeOfferSheetBind.customOffer.value().isEmpty()){
				Alerts.error(mCtx,"Please Enter Offer Amount")
			}else{
				hideKeyboard(it)
				makeOfferSheet.dismiss()
				bind.loader.isVisible = true
				viewModel.makeOffer(makeOfferSheetBind.customOffer.value().request(),productId.request())
			}

		}
		makeOfferSheet.show()
	}

}