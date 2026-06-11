package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SoldProductsAdapter(
    mList: MutableList<GetOrdersResponse.Data?>,
    private val mClicks: RecyclerClicks,
) : BaseAdapter<GetOrdersResponse.Data?, ShopItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShopItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShopItemBinding>,
        position: Int,
        item: GetOrdersResponse.Data?,
    ) {
        with(holder) {
            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            val product = item?.product
            bind.productImage.loadUrl(
                mCtx,
                product?.thumbnail?.firstOrNull().orEmpty()
                    .ifEmpty { product?.images?.firstOrNull().orEmpty() },
            )

            bind.productName.text = when {
                !product?.title.isNullOrBlank() -> product?.title?.asCapital()
                !item?.productSetItemUnit?.name.isNullOrBlank() -> item?.productSetItemUnit?.name?.asCapital()
                !item?.productSetItem?.name.isNullOrBlank() -> item?.productSetItem?.name?.asCapital()
                !item?.productSet?.name.isNullOrBlank() -> item?.productSet?.name?.asCapital()
                else -> "Product"
            }

            val category = product?.category?.name.orEmpty()
            val condition = product?.productCondition
                ?.replace("_", " ")
                ?.asCapital()
                .orEmpty()

            val hasCategory = category.isNotBlank()
            val hasCondition = condition.isNotBlank()
            bind.category.isVisible = hasCategory || hasCondition
            bind.category.text = buildSpannedString {
                if (hasCategory) append(category)
                if (hasCategory && hasCondition) {
                    append(" ")
                    append(Const.BULLET)
                    append(" ")
                }
                if (hasCondition) append(condition)
            }

            bind.price.text = when {
                !product?.pricing.isNullOrBlank() -> product?.pricing.asMoney()
                item?.productSetItemUnit?.price != null -> item.productSetItemUnit.price.toString().asMoney()
                item?.productSetItem?.price != null -> item.productSetItem.price.toString().asMoney()
                item?.productSet?.price != null -> item.productSet.price.toString().asMoney()
                else -> "0".asMoney()
            }
        }
    }
}
