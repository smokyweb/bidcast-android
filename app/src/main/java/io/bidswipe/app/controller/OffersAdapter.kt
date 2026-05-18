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

// #38: null item in mList is treated as a section-separator header between
//      buyer-placed bids and seller-received bids.
class        OffersAdapter(
	mList: MutableList<GetOffersResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetOffersResponse.Data?, BidsItemBinding>(mList) {
	
	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) = BidsItemBinding.inflate(inflater, parent, false)
	
	override fun onBind(
		holder: BaseViewHolder<BidsItemBinding>,
		position: Int,
		item: GetOffersResponse.Data?,
	) {
		with(holder) {

			// #38: null item = section separator between buyer-placed bids and
			// seller-received bids.  Reuse the card layout to show a label row.
			if (item == null) {
				bind.iconCard.isVisible = false
				bind.userName.text = "— Bids on My Items —"
				bind.subTitle.text = "Offers placed on your listings"
				bind.offerPrice.isVisible = false
				bind.productContainer.isVisible = false
				bind.status.isVisible = false
				return@with
			}

			// Ensure all views are visible for normal items (reset from header state)
			bind.iconCard.isVisible = true
			bind.offerPrice.isVisible = true
			bind.productContainer.isVisible = true

			bind.productContainer.setHapticClickListener {
				mClicks.itemClick(position)
			}

			when (item?.status) {
				"accepted" -> {
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
					bind.status.isVisible = true
					bind.status.setTextColor(ContextCompat.getColor(mCtx, R.color.primary))
					bind.status.backgroundTintList = ColorStateList.valueOf(
						ContextCompat.getColor(
							mCtx,
							R.color.primaryContainer
						)
					)
					bind.status.text = ContextCompat.getString(mCtx, R.string.pending)
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