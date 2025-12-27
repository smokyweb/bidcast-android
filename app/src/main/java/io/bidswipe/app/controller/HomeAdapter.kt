package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class HomeAdapter(
	var mList: MutableList<GetMyShowResponse.Data?>, val mClick: RecyclerClicks,
) : BaseAdapter<GetMyShowResponse.Data?, HomeItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) = HomeItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<HomeItemBinding>,
		position: Int,
		item: GetMyShowResponse.Data?,
	) {
		with(holder) {

			bind.userInfo.setHapticClickListener {
				mClick.itemClick(position, "user")
			}

			bind.thumbnail.setHapticClickListener {
				mClick.itemClick(position, "viewShow")
			}

			bind.userImage.loadUrl(mCtx, item?.user?.profileImage ?: "", draw.placeholder_user)

			if (item?.thumbnail != null) {
				bind.thumbnail.loadUrl(mCtx, item.thumbnail[0] ?: "")
			}

			bind.title.text = item?.title.toString().asCapital()

			// Category with bullet separator style
			bind.category.text = buildString {
				append(item?.category?.name ?: "")
			}

			Log.d(TAG, "onBind: ${item?.user?.name}")

			bind.userName.text = buildSpannedString {
				bold {
					append((item?.user?.username?.asCapital() ?: "").ifEmpty { item?.user?.name?.asCapital() ?: "user@${item?.user?.id}" })
				}
			}

			// Live badge with viewer count
			val isLive = item?.isLive == true
			bind.liveCard.isVisible = isLive
			if (isLive) {
				val viewerCount = item?.viewerCount ?: 0
				bind.viewerCount.text = "Live • ${viewerCount}"
			}

		}
	}

	/*fun updateList(newItems: MutableList<GetMyShowResponse.Data?>) {
		val diffCallback = object : DiffUtil.Callback() {
			override fun getOldListSize(): Int = mList.size
			override fun getNewListSize(): Int = newItems.size

			override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
				return mList[oldItemPosition]?.id == newItems[newItemPosition]?.id
			}

			override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
				return mList[oldItemPosition] == newItems[newItemPosition]
			}
		}

		val diffResult = DiffUtil.calculateDiff(diffCallback)

		// Update the internal list
		mList = newItems

		// Notify only changes
		diffResult.dispatchUpdatesTo(this)
	}*/
}
