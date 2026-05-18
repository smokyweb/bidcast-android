package io.bidswipe.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.databinding.ItemSearchUserBinding
import io.bidswipe.app.databinding.ItemSearchProductBinding
import io.bidswipe.app.databinding.ItemSearchShowBinding
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow

class SectionedSearchAdapter(
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    private val headers = mutableListOf<String>()

    interface OnItemClickListener {
        fun onUserClick(user: SearchUser)
        fun onProductClick(product: SearchProduct)
        fun onShowClick(show: SearchShow)
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_USER = 1
        private const val TYPE_PRODUCT = 2
        private const val TYPE_SHOW = 3
    }

    fun setData(users: List<SearchUser>, products: List<SearchProduct>, shows: List<SearchShow>) {
        items.clear()
        headers.clear()

        if (users.isNotEmpty()) {
            headers.add("Users")
            items.add("Users")
            items.addAll(users)
        }
        if (products.isNotEmpty()) {
            headers.add("Products")
            items.add("Products")
            items.addAll(products)
        }
        if (shows.isNotEmpty()) {
            headers.add("Shows")
            items.add("Shows")
            items.addAll(shows)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is String -> TYPE_HEADER
            is SearchUser -> TYPE_USER
            is SearchProduct -> TYPE_PRODUCT
            is SearchShow -> TYPE_SHOW
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemSearchHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_USER -> {
                val binding = ItemSearchUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            TYPE_PRODUCT -> {
                val binding = ItemSearchProductBinding.inflate(inflater, parent, false)
                ProductViewHolder(binding)
            }
            TYPE_SHOW -> {
                val binding = ItemSearchShowBinding.inflate(inflater, parent, false)
                ShowViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as String)
            is UserViewHolder -> holder.bind(items[position] as SearchUser)
            is ProductViewHolder -> holder.bind(items[position] as SearchProduct)
            is ShowViewHolder -> holder.bind(items[position] as SearchShow)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val binding: ItemSearchHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.headerTitle.text = title
        }
    }

    inner class UserViewHolder(private val binding: ItemSearchUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: SearchUser) {
            binding.userName.text = user.name
            binding.root.setOnClickListener { listener.onUserClick(user) }
        }
    }

    inner class ProductViewHolder(private val binding: ItemSearchProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: SearchProduct) {
            binding.productName.text = product.title
            binding.root.setOnClickListener { listener.onProductClick(product) }
        }
    }

    inner class ShowViewHolder(private val binding: ItemSearchShowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(show: SearchShow) {
            binding.showName.text = show.title
            binding.root.setOnClickListener { listener.onShowClick(show) }
        }
    }
}
