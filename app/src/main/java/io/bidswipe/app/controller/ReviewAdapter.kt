package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ReviewItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetRatingResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.setHapticClickListener

class ReviewAdapter(
	mList : MutableList<GetRatingResponse.Data.Rating?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetRatingResponse.Data.Rating? , ReviewItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ReviewItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ReviewItemBinding> ,
		position : Int ,
		item : GetRatingResponse.Data.Rating? ,
	) {
		with(holder) {

			bind.title.text = item?.user?.name
			bind.description.text = item?.comment?.asCapital()

			bind.rating.rating = item?.overallRating?.toFloat() ?: 0f

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}


		}
	}
}