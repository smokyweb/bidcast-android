package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl

class HomeAdapter (val mList: MutableList<GetMyShowResponse.Data?>, val mClick: RecyclerClicks
) : BaseAdapter<GetMyShowResponse.Data?, HomeItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        HomeItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<HomeItemBinding>,
        position: Int,
        item: GetMyShowResponse.Data?
    ) {
        with(holder) {

            bind.userInfo.setOnClickListener{

                mClick.itemClick(position,"user")

            }

            bind.userName.text = item?.user?.name.toString()
            bind.userImage.loadUrl(mCtx,item?.user?.profileImage.toString(), draw.user_image)

            bind.thumnail.loadUrl(mCtx,item?.thumbnail?.get(0).toString())

            bind.title.text = item?.title.toString()
            bind.category.text = item?.category?.name

        }
    }
}