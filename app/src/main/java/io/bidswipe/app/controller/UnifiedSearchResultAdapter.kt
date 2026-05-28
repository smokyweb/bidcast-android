package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.databinding.SearchProductRowBinding
import io.bidswipe.app.databinding.SearchSectionHeaderBinding
import io.bidswipe.app.databinding.SearchUserRowBinding
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

/**
 * Basecamp #9929090875 (Trey 2026-05-28, redesign round 4):
 *
 * Multi-viewtype search-result adapter. Three typed sections, each with a
 * bold section header and item count:
 *
 *   Shows    → 2-column grid using home_item.xml (same card as Live/Upcoming)
 *   Products → full-width compact rows (small square thumbnail, bold title,
 *              price, seller username)
 *   Users    → full-width rows (circular profile avatar + bold large username)
 *
 * Empty sections are omitted entirely. The host RecyclerView must use a
 * GridLayoutManager with spanCount=2; call [makeSpanSizeLookup] and attach
 * it to the layout manager so Show cards get 1 span and everything else
 * (headers, products, users) gets 2 spans (full width).
 */
class UnifiedSearchResultAdapter(
    private val onUserClick: (SearchUser) -> Unit,
    private val onProductClick: (SearchProduct) -> Unit,
    private val onShowClick: (SearchShow) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SearchResultItem> = emptyList()

    companion object {
        const val TYPE_HEADER  = 0
        const val TYPE_SHOW    = 1
        const val TYPE_PRODUCT = 2
        const val TYPE_USER    = 3
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    class HeaderVH(val bind: SearchSectionHeaderBinding) :
        RecyclerView.ViewHolder(bind.root)

    class ShowVH(val bind: HomeItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    class ProductVH(val bind: SearchProductRowBinding) :
        RecyclerView.ViewHolder(bind.root)

    class UserVH(val bind: SearchUserRowBinding) :
        RecyclerView.ViewHolder(bind.root)

    // ─── Adapter overrides ────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SearchResultItem.SectionHeader -> TYPE_HEADER
        is SearchResultItem.ShowItem      -> TYPE_SHOW
        is SearchResultItem.ProductItem   -> TYPE_PRODUCT
        is SearchResultItem.UserItem      -> TYPE_USER
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER  -> HeaderVH(SearchSectionHeaderBinding.inflate(inf, parent, false))
            TYPE_SHOW    -> ShowVH(HomeItemBinding.inflate(inf, parent, false))
            TYPE_PRODUCT -> ProductVH(SearchProductRowBinding.inflate(inf, parent, false))
            else         -> UserVH(SearchUserRowBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ctx: Context = holder.itemView.context
        when (val item = items[position]) {

            is SearchResultItem.SectionHeader -> {
                val h = holder as HeaderVH
                h.bind.sectionTitle.text = item.title
                h.bind.sectionCount.text = "(${item.count})"
            }

            is SearchResultItem.ShowItem -> {
                val h = holder as ShowVH
                val show = item.show
                val seller = show.user
                h.bind.userImage.loadUrl(ctx, seller?.profileImage ?: "", userName = seller?.name)
                // Basecamp #9929090875: seller username should be BOLDED on show rows
                h.bind.userName.text = seller?.name ?: "Seller"
                h.bind.userName.setTypeface(null, android.graphics.Typeface.BOLD)
                h.bind.title.text = show.title ?: "Untitled show"
                h.bind.category.text = if (show.isLive == true) "Live" else "Upcoming"
                h.bind.cardView.visibility = View.VISIBLE
                val thumb = show.thumbnail?.firstOrNull() ?: ""
                h.bind.thumbnail.loadUrl(ctx, thumb)
                h.bind.liveCard.visibility = if (show.isLive == true) View.VISIBLE else View.GONE
                h.bind.root.setHapticClickListener { onShowClick(show) }
            }

            is SearchResultItem.ProductItem -> {
                val h = holder as ProductVH
                val product = item.product
                val seller = product.user
                val thumb = product.thumbnail?.firstOrNull()
                    ?: product.images?.firstOrNull() ?: ""
                h.bind.thumbnail.loadUrl(ctx, thumb)
                h.bind.productTitle.text = product.title ?: "Untitled product"
                h.bind.productPrice.text = product.pricing?.let { "$$it" } ?: ""
                // Basecamp #9929090875: seller username BOLDED on product rows
                h.bind.sellerName.text = seller?.name ?: seller?.username ?: ""
                h.bind.root.setHapticClickListener { onProductClick(product) }
            }

            is SearchResultItem.UserItem -> {
                val h = holder as UserVH
                val user = item.user
                h.bind.userAvatar.loadUrl(ctx, user.profileImage ?: "", userName = user.name)
                // Basecamp #9929090875: user names BOLDED + LARGER
                h.bind.userName.text = user.username?.let { "@$it" } ?: user.name ?: "Unknown"
                h.bind.root.setHapticClickListener { onUserClick(user) }
            }
        }
    }

    // ─── SpanSizeLookup ───────────────────────────────────────────────────────

    /**
     * Returns a SpanSizeLookup for a 2-span GridLayoutManager.
     * Show cards get 1 span (2 per row); headers, products, and users
     * get 2 (full width).
     */
    fun makeSpanSizeLookup(): GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (position in items.indices && items[position] is SearchResultItem.ShowItem) 1 else 2
        }

    // ─── Data ─────────────────────────────────────────────────────────────────

    /**
     * Build the section list from raw result sets and call notifyDataSetChanged.
     * Empty sections are omitted. Call from the [unifiedSearchRepo] observer.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitResults(
        shows: List<SearchShow>,
        products: List<SearchProduct>,
        users: List<SearchUser>,
    ) {
        val list = mutableListOf<SearchResultItem>()
        if (shows.isNotEmpty()) {
            list += SearchResultItem.SectionHeader("Shows", shows.size)
            shows.mapTo(list) { SearchResultItem.ShowItem(it) }
        }
        if (products.isNotEmpty()) {
            list += SearchResultItem.SectionHeader("Products", products.size)
            products.mapTo(list) { SearchResultItem.ProductItem(it) }
        }
        if (users.isNotEmpty()) {
            list += SearchResultItem.SectionHeader("Users", users.size)
            users.mapTo(list) { SearchResultItem.UserItem(it) }
        }
        items = list
        notifyDataSetChanged()
    }

    /** Legacy path kept so existing call-sites don't break. */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun isEmpty() = items.isEmpty()
}

/** Sealed wrapper so the adapter can hold mixed section headers + result rows. */
sealed class SearchResultItem {
    data class SectionHeader(val title: String, val count: Int) : SearchResultItem()
    data class UserItem(val user: SearchUser) : SearchResultItem()
    data class ProductItem(val product: SearchProduct) : SearchResultItem()
    data class ShowItem(val show: SearchShow) : SearchResultItem()
}
