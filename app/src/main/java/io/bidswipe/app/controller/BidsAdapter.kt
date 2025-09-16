package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class BidsAdapter(
	mList : MutableList<FetchBidResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<FetchBidResponse.Data? , BidsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		BidsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<BidsItemBinding> ,
		position : Int ,
		item : FetchBidResponse.Data? ,
	) {
		with(holder) {

			bind.userName.text = buildString {
				append(item?.user?.name?.asCapital())
			}

			val profileImage = item?.user?.profileImage
			if (profileImage.isNullOrEmpty()) {
				bind.userImage.setImageResource(R.drawable.avatar)
			} else {
				bind.userImage.loadUrl(mCtx , profileImage)
			}

			bind.offerPrice.text = (item?.product?.pricing ?: 0).toString().asMoney()
			bind.productName.text = item?.product?.title?.asCapital()

			val productImage = if (item?.product?.images.isNullOrEmpty()) {
				null
			} else {
				item.product.images[0]
			}

			if (productImage.isNullOrEmpty()) {
				bind.productImage.setImageResource(R.drawable.avatar)
			} else {
				bind.productImage.loadUrl(mCtx , productImage)
			}
			bind.prodSubTitle.text = buildString {
				append("Current Bid: ")
				append((item?.bidPrice ?: 0).toString().asMoney())
			}

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}
			bind.subTitle.text = buildSpannedString {
				append("Placed a Bid ")
				bold { append(" • ") }
				append(Utils.getTimeAgo(item?.createdAt ?: "" , Const.DD_MM_YYYY_HH_MM_SS))
			}
		}
	}
}