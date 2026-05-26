package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.databinding.SearchResultItemBinding
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

/**
 * Basecamp #9929090875 (Trey 2026-05-26): unified search result adapter.
 *
 * Displays a flat list of users and products returned by the
 * POST /api/v1/search endpoint. Phase 1 showed only a count summary;
 * this adapter replaces that with actual tappable rows.
 *
 * Items are backed by [SearchResultItem] which wraps either a [SearchUser]
 * or a [SearchProduct]. Clicks are delivered via [onUserClick] / [onProductClick].
 */
class UnifiedSearchResultAdapter(
    private var items: List<SearchResultItem> = emptyList(),
    private val onUserClick: (SearchUser) -> Unit,
    private val onProductClick: (SearchProduct) -> Unit,
) : RecyclerView.Adapter<UnifiedSearchResultAdapter.SearchViewHolder>() {

    class SearchViewHolder(val bind: SearchResultItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            SearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = items[position]
        val ctx: Context = holder.bind.root.context

        when (item) {
            is SearchResultItem.UserItem -> {
                val user = item.user
                holder.bind.title.text = user.name ?: "Unknown"
                holder.bind.subtitle.text = user.username?.let { "@$it" } ?: ""
                holder.bind.typeChip.text = "User"
                // Round avatar
                holder.bind.imageCard.radius = holder.bind.imageCard.width / 2f.coerceAtLeast(52f)
                holder.bind.thumbnail.loadUrl(ctx, user.profileImage ?: "", userName = user.name)
                holder.bind.rootClick.setHapticClickListener {
                    onUserClick(user)
                }
            }

            is SearchResultItem.ProductItem -> {
                val product = item.product
                holder.bind.title.text = product.title ?: "Unknown product"
                holder.bind.subtitle.text = product.pricing?.let { "$$it" } ?: ""
                holder.bind.typeChip.text = "Product"
                // Square thumbnail
                val thumb = product.thumbnail?.firstOrNull() ?: product.images?.firstOrNull() ?: ""
                holder.bind.thumbnail.loadUrl(ctx, thumb)
                holder.bind.rootClick.setHapticClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

/** Sealed wrapper so the adapter can hold mixed user + product rows. */
sealed class SearchResultItem {
    data class UserItem(val user: SearchUser) : SearchResultItem()
    data class ProductItem(val product: SearchProduct) : SearchResultItem()
}
