package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ShopAdapter(
	mList: MutableList<Product?>, val mClicks: RecyclerClicks,
) : BaseAdapter<Product?, ShopItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ShopItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ShopItemBinding>,
		position: Int,
		item: Product?,
	) {
		with(holder) {
			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.productImage.loadUrl(mCtx, item?.images?.get(0).toString())

			bind.productName.text = item?.title.toString().asCapital()

			when {
				item?.productCondition != null -> {
					bind.category.text = buildSpannedString {
						append(item.category?.name + " ")
						append(Const.BULLET)
						append(" " + item.productCondition.replace("_"," "))
					}
				}
				item?.category != null -> {
					bind.category.text = item.category.name.toString().asCapital()
				}
				else -> {
					bind.category.isVisible = false
				}
			}

			bind.price.text = item?.pricing.toString().asMoney()

		}
	}
}