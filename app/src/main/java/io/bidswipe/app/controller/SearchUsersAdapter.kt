package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UserSelectorItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SearchUsersAdapter(
    mList: MutableList<UserSearchingResponse.Data?>,
    val mClicks: RecyclerClicks,
) : BaseAdapter<UserSearchingResponse.Data, UserSelectorItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        UserSelectorItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<UserSelectorItemBinding>,
        position: Int,
        item: UserSearchingResponse.Data?,
    ) {
        with(holder) {
            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.userImage.loadUrl(mCtx, item?.profileImage ?: "", userName = item?.name)
            bind.text.text = item?.name
            bind.divider.isVisible = false

            if (item?.selected == true) {
                bind.iconCard.strokeWidth = mCtx.resources.dpToPx(3)
                bind.iconCard.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
            } else {
                bind.iconCard.strokeWidth = 0
                bind.iconCard.strokeColor = ContextCompat.getColor(mCtx, R.color.background)
            }
        }
    }
}