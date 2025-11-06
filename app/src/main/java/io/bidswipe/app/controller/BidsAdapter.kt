package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class BidsAdapter(
	mList: MutableList<FetchBidResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<FetchBidResponse.Data?, BidsItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		BidsItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<BidsItemBinding>,
		position: Int,
		item: FetchBidResponse.Data?,
	) {
		with(holder) {
			
			bind.userName.text = buildString {
				append(item?.user?.name?.asCapital())
			}
			
			val profileImage = item?.user?.profileImage
			if (profileImage.isNullOrEmpty()) {
				bind.userImage.setImageResource(R.drawable.placeholder_user)
			} else {
				bind.userImage.loadUrl(mCtx, profileImage)
			}
			
			bind.offerPrice.text = (item?.product?.pricing ?: 0).toString().asMoney()
			bind.offerPrice.alpha = 1.0f
			bind.offerPrice.setTextColor(mCtx.getColor(R.color.success))
			
			bind.subTitle.text = buildSpannedString {
				append("Placed a Bid ")
				bold { append(" • ") }
				append(Utils.getTimeAgo(item?.createdAt ?: "", Const.DD_MM_YYYY_HH_MM_SS))
			}
			
			if (item?.product != null) {
				bind.prodSubTitle.text = buildString {
					append("Current Bid: ")
					append((item.bidPrice ?: 0).toString().asMoney())
				}
				
				bind.productName.text = item.product.title?.asCapital()
				
				val productImage = if (item.product.images.isNullOrEmpty()) {
					null
				} else {
					item.product.images[0]
				}
				
				bind.productName.alpha = 1f
				bind.prodSubTitle.alpha = 1f
				
				if (productImage.isNullOrEmpty()) {
					bind.productImage.setImageResource(R.drawable.placeholder_square)
				} else {
					bind.productImage.loadUrl(mCtx, productImage, R.drawable.placeholder_square)
				}
				
			} else {
				
				bind.productName.text = mCtx.getString(R.string.product_deleted)
				bind.productName.setTextColor(mCtx.getColor(R.color.onSurfaceVariant))
				bind.productName.alpha = 0.6f
				bind.prodSubTitle.text = mCtx.getString(R.string.product_no_longer_available)
				bind.prodSubTitle.setTextColor(mCtx.getColor(R.color.onSurfaceVariant))
				bind.prodSubTitle.alpha = 0.7f
				bind.productImage.setImageResource(R.drawable.placeholder_square)
				
			}
			
			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
			
		}
	}
}