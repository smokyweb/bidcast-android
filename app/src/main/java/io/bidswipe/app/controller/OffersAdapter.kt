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
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class OffersAdapter(
	mList: MutableList<GetOffersResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetOffersResponse.Data?, BidsItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		BidsItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<BidsItemBinding>,
		position: Int,
		item: GetOffersResponse.Data?,
	) {
		with(holder) {
			
			bind.buttonLayout.isVisible = true
			bind.accept.setHapticClickListener {
				mClicks.itemClick(position, "accept")
			}
			
			bind.decline.setHapticClickListener {
				mClicks.itemClick(position, "reject")
			}
			
			when (item?.status) {
				"accepted" -> {
					bind.buttonLayout.isVisible = false
					bind.status.isVisible = true
					bind.status.setTextColor(ContextCompat.getColor(mCtx, R.color.success))
					bind.status.backgroundTintList = ColorStateList.valueOf(
						ContextCompat.getColor(
							mCtx,
							R.color.successContainer
						)
					)
					bind.status.text = ContextCompat.getString(mCtx, R.string.accepted)
				}
				
				"rejected" -> {
					bind.buttonLayout.isVisible = false
					bind.status.isVisible = true
					bind.status.setTextColor(ContextCompat.getColor(mCtx, R.color.error))
					bind.status.backgroundTintList = ColorStateList.valueOf(
						ContextCompat.getColor(
							mCtx,
							R.color.errorContainer
						)
					)
					bind.status.text = ContextCompat.getString(mCtx, R.string.declined)
				}
				
				"pending" -> {
					bind.status.isVisible = false
					bind.buttonLayout.isVisible = true
				}
				
			}
			
			bind.userImage.loadUrl(mCtx, item?.user?.profileImage ?: "")
			bind.userName.text = item?.user?.name?.asCapital()
			bind.offerPrice.text = (item?.amount ?: 0).toString().asMoney()
			
			bind.subTitle.text = buildSpannedString {
				append("Placed an Offer ")
				bold { append("• ") }
				append(Utils.getTimeAgo(item?.createdAt ?: "", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
			}
			
			bind.productImage.loadUrl(mCtx, item?.product?.images?.first() ?: "")
			bind.productName.text = item?.product?.title?.asCapital()
			bind.productName.text = buildString {
				append(item?.product?.title?.asCapital())
				append(" #")
				append(item?.product?.id.toString())
			}
			
			bind.prodSubTitle.text = buildString {
				append("Asking Price: ")
				append((item?.product?.pricing ?: 0).toString().asMoney())
			}
			
			
		}
	}
}