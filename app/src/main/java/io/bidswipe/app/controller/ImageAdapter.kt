package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ImageAdapter(
	mList: MutableList<String?>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, UploadImageItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        UploadImageItemBinding.inflate(inflater, parent, false)

    override fun onBind(
		holder: BaseViewHolder<UploadImageItemBinding>,
		position: Int,
		item: String?,
	) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            if (item != null) {
                bind.image.setImageURI(item.toUri())
            }

        }
    }
}