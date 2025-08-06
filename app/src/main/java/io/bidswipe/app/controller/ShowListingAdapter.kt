package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShowListingItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl

class ShowListingAdapter(
    val mList: MutableList<GetMyShowResponse.Data?>, val mClick: RecyclerClicks,
) : BaseAdapter<GetMyShowResponse.Data?, ShowListingItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShowListingItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShowListingItemBinding>,
        position: Int,
        item: GetMyShowResponse.Data?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClick.itemClick(position, "click")
            }

            bind.name.text = item?.title?.asCapital()

            bind.date.text = item?.date

            bind.time.text = item?.time

            bind.image.loadUrl(mCtx, item?.imgThumbnail?.first() ?:"")

        }
    }

}