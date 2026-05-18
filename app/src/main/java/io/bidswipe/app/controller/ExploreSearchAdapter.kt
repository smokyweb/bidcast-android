package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.bidswipe.app.R
import io.bidswipe.app.databinding.ItemSearchProductCardBinding
import io.bidswipe.app.databinding.ItemSearchSectionHeaderBinding
import io.bidswipe.app.databinding.ItemSearchShowCardBinding
import io.bidswipe.app.databinding.ItemSearchUserCardBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.SearchData
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.utils.formatPrice

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_SHOWS = 1
private const val VIEW_TYPE_PRODUCTS = 2
private const val VIEW_TYPE_USERS = 3

class ExploreSearchAdapter(
    private val listener: RecyclerClicks
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(searchData: SearchData?) {
        items.clear()
        if (searchData != null) {
            if (!searchData.shows.isNullOrEmpty()) {
                items.add("Live Shows")
                items.add(searchData.shows)
            }
            if (!searchData.products.isNullOrEmpty()) {
                items.add("Products")
                items.add(searchData.products)
            }
            if (!searchData.users.isNullOrEmpty()) {
                items.add("Users")
                items.add(searchData.users)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is String -> VIEW_TYPE_HEADER
            is List<*> -> {
                when {
                    item.all { it is SearchShow } -> VIEW_TYPE_SHOWS
                    item.all { it is SearchProduct } -> VIEW_TYPE_PRODUCTS
                    item.all { it is SearchUser } -> VIEW_TYPE_USERS
                    else -> throw IllegalArgumentException("Unsupported list type at position $position")
                }
            }
            else -> throw IllegalArgumentException("Unsupported type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemSearchSectionHeaderBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
            VIEW_TYPE_SHOWS -> ShowsViewHolder(
                RecyclerView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                }, listener
            )
            VIEW_TYPE_PRODUCTS -> ProductsViewHolder(
                RecyclerView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                }, listener
            )
            VIEW_TYPE_USERS -> UsersViewHolder(
                RecyclerView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                }, listener
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as String)
            is ShowsViewHolder -> holder.bind(item as List<SearchShow>)
            is ProductsViewHolder -> holder.bind(item as List<SearchProduct>)
            is UsersViewHolder -> holder.bind(item as List<SearchUser>)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(private val binding: ItemSearchSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.headerTitle.text = title
        }
    }

    class ShowsViewHolder(
        private val recyclerView: RecyclerView,
        private val listener: RecyclerClicks
    ) : RecyclerView.ViewHolder(recyclerView) {
        fun bind(shows: List<SearchShow>) {
            recyclerView.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = ShowsAdapter(shows, listener)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.half_margin), 0,
                    resources.getDimensionPixelSize(R.dimen.half_margin), 0
                )
                clipToPadding = false
            }
        }
    }

    class ProductsViewHolder(
        private val recyclerView: RecyclerView,
        private val listener: RecyclerClicks
    ) : RecyclerView.ViewHolder(recyclerView) {
        fun bind(products: List<SearchProduct>) {
            recyclerView.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = ProductsAdapter(products, listener)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.half_margin), 0,
                    resources.getDimensionPixelSize(R.dimen.half_margin), 0
                )
                clipToPadding = false
            }
        }
    }

    class UsersViewHolder(
        private val recyclerView: RecyclerView,
        private val listener: RecyclerClicks
    ) : RecyclerView.ViewHolder(recyclerView) {
        fun bind(users: List<SearchUser>) {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = UsersAdapter(users, listener)
            }
        }
    }
}

// Inner Adapters
class ShowsAdapter(
    private val shows: List<SearchShow>,
    private val listener: RecyclerClicks
) : RecyclerView.Adapter<ShowsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSearchShowCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(shows[position])
    }

    override fun getItemCount(): Int = shows.size

    inner class ViewHolder(private val binding: ItemSearchShowCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.itemClick(adapterPosition, "show")
                }
            }
        }

        fun bind(show: SearchShow) {
            binding.showTitle.text = show.title
            binding.userName.text = show.user?.name ?: "Unknown"
            binding.liveCard.isVisible = show.isLive == true

            Glide.with(itemView.context)
                .load(show.thumbnail?.firstOrNull())
                .placeholder(R.drawable.placeholder_rect)
                .into(binding.thumbnail)

            Glide.with(itemView.context)
                .load(show.user?.profileImage)
                .placeholder(R.drawable.user_image)
                .into(binding.userImage)
        }
    }
}

class ProductsAdapter(
    private val products: List<SearchProduct>,
    private val listener: RecyclerClicks
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSearchProductCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ViewHolder(private val binding: ItemSearchProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.itemClick(adapterPosition, "product")
                }
            }
        }

        fun bind(product: SearchProduct) {
            binding.productTitle.text = product.title
            binding.productPrice.text = product.pricing.formatPrice()
            Glide.with(itemView.context)
                .load(product.thumbnail?.firstOrNull())
                .placeholder(R.drawable.placeholder_rect)
                .into(binding.thumbnail)
        }
    }
}

class UsersAdapter(
    private val users: List<SearchUser>,
    private val listener: RecyclerClicks
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSearchUserCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class ViewHolder(private val binding: ItemSearchUserCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.itemClick(adapterPosition, "user")
                }
            }
        }

        fun bind(user: SearchUser) {
            binding.userName.text = user.name
            binding.userUsername.text = "@${user.username}"
            Glide.with(itemView.context)
                .load(user.profileImage)
                .placeholder(R.drawable.user_image)
                .into(binding.userImage)
        }
    }
}
