package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BlockedItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl

class BlockedUsersAdapter (
    mList: MutableList<GetBlockedUsersResponse.Data.BlockedByMe?>, val mClicks: RecyclerClicks,

    ): BaseAdapter<GetBlockedUsersResponse.Data.BlockedByMe?, BlockedItemBinding>(mList) {
    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup,
    )= BlockedItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BlockedItemBinding>,
        position: Int,
        item: GetBlockedUsersResponse.Data.BlockedByMe?,
    ) {
        with(holder){
            bind.name.text = item?.name?.asCapital()
            bind.icon.loadUrl(mCtx,item?.image.toString())

            bind.click.setOnClickListener {   }
            bind.unblockUser.setOnClickListener {
                mClicks.itemClick(position,"unblock")
            }

        }
    }

}