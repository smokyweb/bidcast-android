package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.controller.OffersAdapter
import io.bidswipe.app.databinding.BuyNowSheetBinding
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.databinding.FragmentProductDetailsBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.databinding.SellBottomSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids

class ProductDetailsFragment : BaseFragment<DashViewModel, FragmentProductDetailsBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentProductDetailsBinding.inflate(inflater,view,false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
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
			var makeOfferSheetBind = MakeOfferSheetBinding.bind(layoutInflater.inflate(R.layout.make_offer_sheet, null, false))
			var makeOfferSheet = Alerts.appBottomSheet(mCtx, true, makeOfferSheetBind)

			makeOfferSheetBind.offerRecycler.adapter = MakeOfferAdapter(mutableListOf("1,039","1,104","1,169","1,234"),object : RecyclerClicks{
			
				override fun itemClick(pos: Int, status: String?) {
				
				}
				
			})
			
			makeOfferSheetBind.close.setOnClickListener {
			makeOfferSheet.dismiss()
			}
			makeOfferSheet.show()
			
		}
		
		
	}
}