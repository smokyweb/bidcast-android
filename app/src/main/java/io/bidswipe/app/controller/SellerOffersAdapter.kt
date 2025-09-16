package io.bidswipe.app.controller

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SellerOffersItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SellerOffersAdapter(
	mList : MutableList<GetOffersResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetOffersResponse.Data? , SellerOffersItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		SellerOffersItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<SellerOffersItemBinding> ,
		position : Int ,
		item : GetOffersResponse.Data? ,
	) {
		with(holder) {

			bind.buttonLayout.isVisible = true

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.accept.setHapticClickListener {
				mClicks.itemClick(position , "accept")
			}

			bind.decline.setHapticClickListener {
				mClicks.itemClick(position , "reject")
			}

			bind.userImage.loadUrl(mCtx , item?.user?.profileImage ?: "")
			bind.userName.text = item?.user?.name
			bind.offerPrice.text = item?.amount.toString().asMoney()

			bind.productImage.loadUrl(mCtx , item?.product?.images?.first() ?: "")
			bind.productName.text = item?.product?.title

			bind.subTitle.text = buildSpannedString {
				append("Placed an Offer ")
				bold { append("•") }
				append(Utils.getTimeAgo(item?.createdAt ?: ""))
			}

			bind.prodSubTitle.text = buildSpannedString {
				append("Asking price : ")
				append(item?.product?.pricing.toString().asMoney())
			}

			if (item?.status == "pending") {
				bind.status.isVisible = false
				bind.buttonLayout.isVisible = true
			} else {
				bind.buttonLayout.isVisible = false
				bind.status.isVisible = true

				when (item?.status) {
					"accepted" -> {
						bind.status.setTextColor(ContextCompat.getColor(mCtx , R.color.success))
						bind.status.backgroundTintList = ColorStateList.valueOf(
							ContextCompat.getColor(
								mCtx ,
								R.color.successContainer
							)
						)
						bind.status.text = ContextCompat.getString(mCtx , R.string.accepted)
					}

					"rejected" -> {
						bind.status.setTextColor(ContextCompat.getColor(mCtx , R.color.error))
						bind.status.backgroundTintList = ColorStateList.valueOf(
							ContextCompat.getColor(
								mCtx ,
								R.color.errorContainer
							)
						)
						bind.status.text = ContextCompat.getString(mCtx , R.string.declined)
					}
				}
			}
		}

	}
}