package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.bidswipe.app.R
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.databinding.SearchPagerRowBinding
import io.bidswipe.app.databinding.SearchProductRowBinding
import io.bidswipe.app.databinding.SearchSectionHeaderBinding
import io.bidswipe.app.databinding.SearchUserRowBinding
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.utils.dpToPx
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
 * GridLayoutManager; call [makeSpanSizeLookup] and attach it to the layout
 * manager so Show cards get 1 span and everything else gets the full span.
 */
class UnifiedSearchResultAdapter(
    private val onUserClick: (SearchUser) -> Unit,
    private val onProductClick: (SearchProduct) -> Unit,
    private val onShowClick: (SearchShow) -> Unit = {},
    private val onPageClick: (Int) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SearchResultItem> = emptyList()

    companion object {
        const val TYPE_HEADER  = 0
        const val TYPE_SHOW    = 1
        const val TYPE_PRODUCT = 2
        const val TYPE_USER    = 3
        const val TYPE_PAGER   = 4
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

    class PagerVH(val bind: SearchPagerRowBinding) :
        RecyclerView.ViewHolder(bind.root)

    // ─── Adapter overrides ────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SearchResultItem.SectionHeader -> TYPE_HEADER
        is SearchResultItem.ShowItem      -> TYPE_SHOW
        is SearchResultItem.ProductItem   -> TYPE_PRODUCT
        is SearchResultItem.UserItem      -> TYPE_USER
        is SearchResultItem.PagerItem     -> TYPE_PAGER
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER  -> HeaderVH(SearchSectionHeaderBinding.inflate(inf, parent, false))
            TYPE_SHOW    -> ShowVH(HomeItemBinding.inflate(inf, parent, false))
            TYPE_PRODUCT -> ProductVH(SearchProductRowBinding.inflate(inf, parent, false))
            TYPE_USER    -> UserVH(SearchUserRowBinding.inflate(inf, parent, false))
            else         -> PagerVH(SearchPagerRowBinding.inflate(inf, parent, false))
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

            is SearchResultItem.PagerItem -> {
                bindPager(holder as PagerVH, item)
            }
        }
    }

    private fun bindPager(holder: PagerVH, item: SearchResultItem.PagerItem) {
        val ctx = holder.itemView.context
        val container = holder.bind.pagerContainer
        container.removeAllViews()

        addPagerButton(
            ctx = ctx,
            container = container,
            label = "< Prev",
            page = item.currentPage - 1,
            enabled = item.currentPage > 1,
            selected = false
        )

        pageOptions(item.currentPage, item.lastPage).forEach { page ->
            if (page == null) {
                addPagerEllipsis(ctx, container)
            } else {
                addPagerButton(
                    ctx = ctx,
                    container = container,
                    label = page.toString(),
                    page = page,
                    enabled = true,
                    selected = page == item.currentPage
                )
            }
        }

        addPagerButton(
            ctx = ctx,
            container = container,
            label = "Next >",
            page = item.currentPage + 1,
            enabled = item.currentPage < item.lastPage,
            selected = false
        )
    }

    private fun pageOptions(current: Int, last: Int): List<Int?> {
        val pages = mutableListOf<Int?>()
        var previousVisible = 0
        for (page in 1..last) {
            val visible = page == 1 || page == last || page in (current - 2)..(current + 2)
            if (!visible) continue
            if (previousVisible != 0 && page - previousVisible > 1) {
                pages += null
            }
            pages += page
            previousVisible = page
        }
        return pages
    }

    private fun addPagerButton(
        ctx: Context,
        container: LinearLayout,
        label: String,
        page: Int,
        enabled: Boolean,
        selected: Boolean,
    ) {
        val horizontalPadding = ctx.resources.dpToPx(12)
        val button = MaterialButton(ctx).apply {
            text = label
            setAllCaps(false)
            minWidth = ctx.resources.dpToPx(if (label.length > 2) 74 else 40)
            minHeight = ctx.resources.dpToPx(36)
            minimumHeight = ctx.resources.dpToPx(36)
            insetTop = 0
            insetBottom = 0
            cornerRadius = ctx.resources.dpToPx(18)
            strokeWidth = if (selected) 0 else ctx.resources.dpToPx(1)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.outline))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (selected) R.color.scrim else R.color.background)
            )
            setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.onPrimary else R.color.onBackground))
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            isEnabled = enabled
            isClickable = enabled && !selected
            alpha = if (enabled) 1f else 0.45f
            if (enabled && !selected) {
                setHapticClickListener { onPageClick(page) }
            }
        }
        container.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            ctx.resources.dpToPx(36)
        ).apply {
            marginEnd = ctx.resources.dpToPx(6)
        })
    }

    private fun addPagerEllipsis(ctx: Context, container: LinearLayout) {
        val text = TextView(ctx).apply {
            this.text = "..."
            gravity = android.view.Gravity.CENTER
            setTextColor(ContextCompat.getColor(ctx, R.color.outlineVariant))
            setPadding(ctx.resources.dpToPx(4), 0, ctx.resources.dpToPx(10), 0)
        }
        container.addView(text, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            ctx.resources.dpToPx(36)
        ))
    }

    // ─── SpanSizeLookup ───────────────────────────────────────────────────────

    /**
     * Returns a SpanSizeLookup for the host GridLayoutManager. Show cards get
     * 1 span; headers, products, users, and pager rows get the full span.
     */
    fun makeSpanSizeLookup(fullSpanSize: Int = 2): GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (position in items.indices && items[position] is SearchResultItem.ShowItem) 1 else fullSpanSize
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

    /**
     * Basecamp #9960348333 (round 7 rebuild): render a SINGLE tab's section
     * with no section header (the TabLayout already labels the section).
     * Used by the tabbed SearchShowFragment — one of shows / products / users
     * is shown at a time.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitShowsOnly(shows: List<SearchShow>, currentPage: Int = 1, lastPage: Int = 1) {
        items = shows.map { SearchResultItem.ShowItem(it) }.withPager(currentPage, lastPage)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitProductsOnly(products: List<SearchProduct>, currentPage: Int = 1, lastPage: Int = 1) {
        items = products.map { SearchResultItem.ProductItem(it) }.withPager(currentPage, lastPage)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitUsersOnly(users: List<SearchUser>, currentPage: Int = 1, lastPage: Int = 1) {
        items = users.map { SearchResultItem.UserItem(it) }.withPager(currentPage, lastPage)
        notifyDataSetChanged()
    }

    private fun List<SearchResultItem>.withPager(currentPage: Int, lastPage: Int): List<SearchResultItem> {
        val safeCurrent = currentPage.coerceAtLeast(1)
        val safeLast = lastPage.coerceAtLeast(1)
        return if (safeLast > 1) this + SearchResultItem.PagerItem(safeCurrent.coerceAtMost(safeLast), safeLast) else this
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
    data class PagerItem(val currentPage: Int, val lastPage: Int) : SearchResultItem()
}
