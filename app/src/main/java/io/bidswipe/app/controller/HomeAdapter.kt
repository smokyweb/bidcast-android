package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl

class HomeAdapter(
    val mList: MutableList<GetMyShowResponse.Data?>, val mClick: RecyclerClicks,
) : BaseAdapter<GetMyShowResponse.Data?, HomeItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        HomeItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<HomeItemBinding>,
        position: Int,
        item: GetMyShowResponse.Data?,
    ) {
        with(holder) {

            bind.userInfo.setOnClickListener {
                mClick.itemClick(position, "user")
            }

            bind.thumbnail.setOnClickListener {
                mClick.itemClick(position, "viewShow")
            }

            bind.userName.text = buildSpannedString {
                bold {
                    append(item?.user?.username?.ifEmpty { item.user.name.toString() })
                }
            }
            bind.userImage.loadUrl(mCtx, item?.user?.profileImage.toString(), draw.user_image)

            bind.thumbnail.loadUrl(mCtx, item?.thumbnail?.get(0).toString())

            bind.title.text = item?.title.toString().asCapital()

        }
    }
}