package io.bidswipe.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSalesTaxExemptionBinding
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl

class SalesTaxExemptionFragment : BaseFragment<MoreViewModel , FragmentSalesTaxExemptionBinding>() {
	override fun getModel() : Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentSalesTaxExemptionBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		val userData = App.profileResponse.value

		bind.userName.text = userData?.name?.asCapital()
		bind.bio.text = userData?.bio
		bind.userImage.loadUrl(mCtx, userData?.profileImage ?:"" , draw.placeholder_user)

	}
}