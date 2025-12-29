package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TitleItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ThumbnailTipsAdapter(
	mList : MutableList<GetAllTipsResponse.Data.Tip?> , val type : String , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetAllTipsResponse.Data.Tip? , TitleItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TitleItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<TitleItemBinding> ,
		position : Int ,
		item : GetAllTipsResponse.Data.Tip? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			/*if (type == "getStarted" || type == "tips") {
				bind.next.isVisible = false
			}*/

			if (type == "shipping") {
				bind.root.background.setTint(ContextCompat.getColor(mCtx , R.color.background))
			}

			bind.icon.loadUrl(mCtx , item?.icon.toString())

			/*if(type=="getStarted"){
				bind.icon.backgroundTintList= ColorStateList.valueOf(ContextCompat.getColor(mCtx,clr.onSecondary))
				bind.root.background.setTint(ContextCompat.getColor(mCtx , R.color.background))
			}else{
				bind.iconCard.setCardBackgroundColor(Color.parseColor(item?.color))
			}*/

			bind.subTitle.text = item?.description
			bind.title.text = item?.title

		}
	}
}