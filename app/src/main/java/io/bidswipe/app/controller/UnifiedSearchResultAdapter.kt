package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

/**
 * Basecamp #9929090875 (Trey 2026-05-26, round 2/3 2026-05-27):
 * unified search result adapter.
 *
 * ROUND 3 (2026-05-27): Trey reported product+user rows should match the
 * Live/Upcoming show-card layout instead of the previous tiny compact rows.
 * Adapter now uses the SAME home_item.xml binding as HomeAdapter so all 3
 * result types render with consistent card sizing, image area, and feel.
 *
 * Variants:
 *   - ShowItem    → full card (thumbnail, seller info, title, optional live badge)
 *   - UserItem    → seller info row only (large avatar + name), no thumbnail
 *   - ProductItem → product thumbnail + price as subtitle
 */
class UnifiedSearchResultAdapter(
    private var items: List<SearchResultItem> = emptyList(),
    private val onUserClick: (SearchUser) -> Unit,
    private val onProductClick: (SearchProduct) -> Unit,
    private val onShowClick: (SearchShow) -> Unit = {},
) : RecyclerView.Adapter<UnifiedSearchResultAdapter.SearchViewHolder>() {

    class SearchViewHolder(val bind: HomeItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            HomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = items[position]
        val ctx: Context = holder.bind.root.context

        when (item) {
            is SearchResultItem.UserItem -> {
                val user = item.user
                holder.bind.userImage.loadUrl(ctx, user.profileImage ?: "", userName = user.name)
                holder.bind.userName.text = user.name ?: "Unknown"
                holder.bind.title.text = user.username?.let { "@$it" } ?: ""
                holder.bind.category.text = "User"
                holder.bind.cardView.isVisible = false
                holder.bind.liveCard.isVisible = false
                holder.bind.userInfo.setHapticClickListener { onUserClick(user) }
                holder.bind.root.setHapticClickListener { onUserClick(user) }
            }

            is SearchResultItem.ProductItem -> {
                val product = item.product
                val seller = product.user
                holder.bind.userImage.loadUrl(ctx, seller?.profileImage ?: "", userName = seller?.name)
                holder.bind.userName.text = seller?.name ?: "Seller"
                holder.bind.title.text = product.title ?: "Untitled product"
                holder.bind.category.text = product.pricing?.let { "$$it" } ?: "Product"
                val thumb = product.thumbnail?.firstOrNull() ?: product.images?.firstOrNull() ?: ""
                holder.bind.cardView.isVisible = true
                holder.bind.thumbnail.loadUrl(ctx, thumb)
                holder.bind.liveCard.isVisible = false
                holder.bind.root.setHapticClickListener { onProductClick(product) }
            }

            is SearchResultItem.ShowItem -> {
                val show = item.show
                val seller = show.user
                holder.bind.userImage.loadUrl(ctx, seller?.profileImage ?: "", userName = seller?.name)
                holder.bind.userName.text = seller?.name ?: "Seller"
                holder.bind.title.text = show.title ?: "Untitled show"
                holder.bind.category.text = if (show.isLive == true) "Live" else "Upcoming"
                holder.bind.cardView.isVisible = true
                val thumb = show.thumbnail?.firstOrNull() ?: ""
                holder.bind.thumbnail.loadUrl(ctx, thumb)
                holder.bind.liveCard.isVisible = (show.isLive == true)
                holder.bind.root.setHapticClickListener { onShowClick(show) }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

/** Sealed wrapper so the adapter can hold mixed user + product + show rows. */
sealed class SearchResultItem {
    data class UserItem(val user: SearchUser) : SearchResultItem()
    data class ProductItem(val product: SearchProduct) : SearchResultItem()
    data class ShowItem(val show: SearchShow) : SearchResultItem()
}
