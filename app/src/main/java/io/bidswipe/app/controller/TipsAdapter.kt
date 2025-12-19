package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TipsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetTipAmountResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class TipsAdapter(
    mList: MutableList<GetTipAmountResponse.Data.Tip?>, val mClicks: RecyclerClicks
) : BaseAdapter<GetTipAmountResponse.Data.Tip, TipsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        TipsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<TipsItemBinding>,
        position: Int,
        item: GetTipAmountResponse.Data.Tip?
    ) {
        with(holder) {
            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.chat.setHapticClickListener {
                mClicks.itemClick(position, "chat")
            }

            bind.userName.text = buildSpannedString {
               bold { append( item?.user?.name?.asCapital()) }
                append(" tipped ")
               bold {   append(item?.total.toString().asMoney())}
                if(item?.show!=null){
                    append(" during ")
                    append(item.show.title.toString())
                }
            }

            bind.date.text = Utils.getFormattedDateTime(
                Const.SERVER_TIME_FORMAT,
                "MMM dd, yyyy",
                item?.createdAt.toString()
            )

            bind.icon.loadUrl(mCtx, item?.user?.profileImage ?: "", userName = item?.user?.name)

            }

        }
    }