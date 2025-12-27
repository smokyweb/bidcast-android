package io.bidswipe.app.ui.sellerProfile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShopAdapter
import io.bidswipe.app.controller.SortingOptionAdapter
import io.bidswipe.app.databinding.FragmentShopBinding
import io.bidswipe.app.databinding.SortingOptionSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import kotlin.text.ifEmpty

class ShopFragment : BaseFragment<SellerViewModel, FragmentShopBinding>() {
    override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentShopBinding.inflate(inflater, view, false)

    private var productList = mutableListOf<Product?>()
    private lateinit var shopAdapter: ShopAdapter
    private val optionList = mutableListOf<LiveMoreOption?>()
    private var sellerId = ""
    private var sortBy = ""
    private var saleType = ""
    private var type = ""
    private var productStatus = ""
    private var page = 1
    private var isLoading = false

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            startActivity(
                Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
                    "productId",
                    productList[pos]?.id.toString()
                )
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sellerId = activity?.intent?.getStringExtra("sellerId") ?: ""

        setUpChips()

        optionList.add(LiveMoreOption("Title (A–Z)", isSelected = false))
        optionList.add(LiveMoreOption("Title (Z–A)", isSelected = false))
        optionList.add(LiveMoreOption("Price (Low to High)", isSelected = false))
        optionList.add(LiveMoreOption("Price (High to Low)", isSelected = false))
        optionList.add(LiveMoreOption("Newest", isSelected = false))
        optionList.add(LiveMoreOption("Oldest", isSelected = false))

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (productList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        bind.bottomLoader.isVisible = true
                        loadData()
                    }
                }
            }
        })

        bind.search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                bind.searchLayout.isEndIconVisible = query.isNotEmpty()

                if (query.isNotEmpty()) {
                    bind.loader.isVisible = true
                    page = 1
                    loadData()
                }

            }
        })

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            viewModel.getUserProducts(userId = sellerId.request(), page = page.toString().request())
        }

        shopAdapter = ShopAdapter(productList, mClick)
        bind.recycler.adapter = shopAdapter
        viewModel.getUserProducts(userId = sellerId.request(), page = page.toString().request())
        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.noInternet.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noData.isVisible = false

                    val mData = it.value.products
                    if (page == 1) {
                        productList.clear()
                    }

                    if (mData != null) {
                        productList.addAll(mData)
                    }

                    if (productList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)
                    shopAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noData.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (Utils.isOnline(mCtx)) {
            bind.noInternet.isVisible = false
            bind.loader.isVisible = true
            page = 1
            viewModel.getUserProducts(userId = sellerId.request(), page = page.toString().request())
        } else {
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            bind.loader.isVisible = false
            bind.noInternet.isVisible = true
        }
    }

    private fun loadData() {
        viewModel.getUserProducts(
            userId = sellerId.request(),
            saleType = saleType.ifEmpty { null }?.request(),
            type = type.ifEmpty { null }?.request(),
	        status = productStatus.ifEmpty { null }?.request(),
            sortBy = sortBy.ifEmpty { null }?.request(),
            page = page.toString().request(),
            search = bind.search.value().ifEmpty { null }?.request()
        )
    }

    private fun setUpChips() {
        bind.search.setText("")
        bind.chipGroup.removeAllViews()

        bind.chipGroup.addView(
            Utils.makeAChip(
                mCtx = mCtx,
                text = "Sort",
                selected = false,
                closeIconVisible = false,
                chipPadding = 12,
                iconRes = io.bidswipe.app.R.drawable.ic_down_arrow
            )
        )

        listOf("Auction", "Buy Now", "Sold").forEach {
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = it,
                    selected = false,
                    closeIconVisible = false,
                    chipPadding = 12,
                )
            )
        }

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {

                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))

                log("Index: $index")

                if (index == -1) return@runSafe

                bind.search.setText("")
                saleType = ""
                sortBy = ""
                page = 1

                when (index) {
                    0 -> {
                        sortOptionSheet()
                    }

                    1 -> {
                        saleType = "auction"
	                    type = ""
	                    productStatus = ""
                    }

                    2 -> {
                        type  = "buy_now"
	                    saleType = ""
	                    productStatus = ""
                    }

                    3 -> {
                        productStatus = "inactive"
	                    saleType = ""
	                    type = ""
                    }

                }

                bind.loader.isVisible = true

                loadData()
            }
        }

    }

    private fun sortOptionSheet() {

        val sortingOptionSheetBinding = SortingOptionSheetBinding.bind(
            layoutInflater.inflate(
                io.bidswipe.app.R.layout.sorting_option_sheet,
                null,
                false
            )
        )

        val sortingOptionSheet = Alerts.appBottomSheet(mCtx, true, sortingOptionSheetBinding)

        sortingOptionSheetBinding.optionRecycler.adapter =
            SortingOptionAdapter(optionList, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {

                    optionList.forEachIndexed { index, item ->
                        item?.isSelected = index == pos
                    }

                    sortingOptionSheetBinding.optionRecycler.adapter?.notifyDataSetChanged()

                    bind.search.setText("")
                    saleType = ""
                    sortBy = ""
	                productStatus = ""

                    page = 1

                    when (pos) {
                        0 -> {
                            sortBy = "title_asc"
                        }

                        1 -> {
                            sortBy = "title_desc"
                        }

                        2 -> {
                            sortBy = "price_low_high"
                        }

                        3 -> {
                            sortBy = "price_high_low"
                        }

                        4 -> {
                            sortBy = "newest"
                        }

                        5 -> {
                            sortBy = "oldest"
                        }

                    }

                    bind.loader.isVisible = true

                    loadData()

                    sortingOptionSheet.dismiss()

                }
            })

        sortingOptionSheetBinding.close.setHapticClickListener {
            sortingOptionSheet.dismiss()
        }

        sortingOptionSheet.show()

    }

}