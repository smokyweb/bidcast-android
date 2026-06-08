package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.R
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.controller.SurpriseProductAdapter
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.databinding.SelectProductTypeSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.value

@AndroidEntryPoint
class ProductsForLiveShowFragment : BottomSheetDialogFragment() {

    // ── Callback for randomizer slot product selection ──────────────────────
    interface OnProductSelectedListener {
        fun onProductSelected(productId: Int, productTitle: String)
    }
    private var productSelectedListener: OnProductSelectedListener? = null
    fun setOnProductSelectedListener(listener: (Int, String) -> Unit) {
        productSelectedListener = object : OnProductSelectedListener {
            override fun onProductSelected(productId: Int, productTitle: String) = listener(productId, productTitle)
        }
    }

    companion object {
        fun newInstance(from: String = "live_show"): ProductsForLiveShowFragment {
            return ProductsForLiveShowFragment().apply {
                arguments = android.os.Bundle().apply { putString("from", from) }
            }
        }
    }

    private lateinit var productAdapter: FirebaseProductAdapter
    private var productList = mutableListOf<Product?>()
    // Basecamp parity: full product list for the CURRENT show (from
    // get-show-details-by-id). The All/Sold/Offers tabs filter this list
    // client-side so every tab stays scoped to this show, never the seller's
    // entire catalog.
    private var showProducts = mutableListOf<Product?>()
    private var surpriseProductList = mutableListOf<GetSurpriseProductsResponse.Data?>()
    private lateinit var surpriseProductAdapter: SurpriseProductAdapter
    private var surprisePage = 1
    private var surpriseIsLoading = false
    private lateinit var mCtx: Context

    private var _binding: FragmentProductsForLiveShowBinding? = null
    private val bind get() = _binding!!

    val viewModel: DashViewModel by activityViewModels()

    private var page = 1
    private var productTotalPage = 1
    private var isLoading = false
    private var selectedPos = -1
    private var saleType = ""
    private var type = ""
    private var status = ""
    private var socketManager: SocketManager? = null

    var from = "live_show"
    var isLive = false
    var auctionTypeId = AuctionType.LIVE.id
    private var readOnly = false

    private fun isRandomizerPicker() = from == "randomizer_slot" || from == "randomizer_prize"

    private var addProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (chipIndex == surpriseChipIndex) {
                    surprisePage = 1
                    loadSurpriseSets()
                } else {
                    loadData()
                }
            }
        }

    // Parity: a show's products all share one pricing format, so the redundant
    // "Auction"/"Buy Now" tabs are gone. Tabs are All / Sold / Offers, plus the
    // Surprise Sets tab (separate seller-surprise-set context).
    private var productTypesList =
        mutableListOf("All", "Sold", "Offers", "Surprise Sets")
    var chipIndex = 0
    // Index of the Surprise Sets chip (last one).
    private val surpriseChipIndex get() = productTypesList.lastIndex

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as FrameLayout?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bottomSheetDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                bottomSheetDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                bottomSheetDialog.window?.navigationBarColor =
                    ContextCompat.getColor(requireContext(), R.color.background)
            }

            bottomSheet?.let {
                val layoutParams = it.layoutParams
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                it.layoutParams = layoutParams
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }

        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mCtx = inflater.context
        _binding = FragmentProductsForLiveShowBinding.inflate(inflater, container, false)
        return bind.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            from = arguments?.getString("from") ?: "live_show"
            auctionTypeId = arguments?.getInt("auction_type_id") ?: AuctionType.LIVE.id
            isLive = arguments?.getBoolean("live_status") ?: false
            readOnly = arguments?.getBoolean("read_only") ?: false
        }

        // (Format-specific saleType/type are no longer used: the panel is scoped
        // to the show and tabs filter client-side.)

        if (isRandomizerPicker()) {
            productTypesList = mutableListOf("All")
            bind.chipGroupScroll.isVisible = false
        }
        if (readOnly) {
            bind.title.text = "Show Products"
        }

        if (from == "freebie") {
            // MC cmph7xsgy00g4ms8pslgxzr1u (2026-05-22): host may select
            // multiple products for one freebie pool.
            bind.title.text = "Select Product(s) for Freebie"
            bind.addBtn.text = "Start Freebie"
        } else if (from == "randomizer_slot") {
            bind.title.text = "Pick Product for Slot"
            bind.addBtn.text = "Select"
        } else if (from == "randomizer_prize") {
            bind.title.text = "Pick Prize Product"
            bind.addBtn.text = "Select"
        }

        socketManager = SocketManager.getInstance(requireContext())

        bind.chipGroup.apply {
            productTypesList.forEach {
                addView(
                    Utils.makeAChip(
                        mCtx,
                        text = it,
                        selected = auctionTypeId == AuctionType.LIVE.id,
                        closeIconVisible = false
                    )
                )
            }

            setOnCheckedStateChangeListener { chipGroup, _ ->
                runSafe {
                    val chipId = chipGroup.checkedChipId
                    chipIndex = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
                    if (isRandomizerPicker()) return@runSafe

                    Log.d("TAG", "onViewCreated: SHOW ${viewModel.showId}--$chipIndex")
                    when (chipIndex) {
                        // All / Sold / Offers are client-side filters over the
                        // current show's product list. Surprise Sets is its own
                        // endpoint.
                        0, 1, 2 -> applyShowFilter()
                        surpriseChipIndex -> {
                            surprisePage = 1
                            loadSurpriseSets()
                        }

                        else -> applyShowFilter()
                    }

                }
            }
        }

        bind.chipGroup.check(bind.chipGroup[0].id)

        // Initial fetch of the current show's product list (All tab). Tab
        // switches then just re-filter the already-loaded list.
        bind.loader.isVisible = true
        loadData()

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isRandomizerPicker()) return
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (productList.size - 1) && !isLoading && page < productTotalPage) {
                    isLoading = true
                    page++
                    loadData(true)
                }
            }
        })

        bind.surpriseRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.surpriseRecycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (surpriseProductList.size - 1)) {
                    if (!surpriseIsLoading) {
                        surpriseIsLoading = true
                        surprisePage++
                        bind.surpriseBottomLoader.isVisible = true
                        loadSurpriseSets(true)
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
                if (isRandomizerPicker()) {
                    page = 1
                    productTotalPage = 1
                    isLoading = false
                    loadData()
                    return
                }
                // Search filters the current show's product list client-side
                // (scoped to this show, with the active All/Sold/Offers tab).
                applyShowFilter()
            }
        })

        productAdapter = FirebaseProductAdapter(
            from = from,
            mList = productList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {

                    val selectedProduct = productList[pos]

                    if (productList[pos]?.status == "sold") {
                        Alerts.error(mCtx, "This product is already sold")
                    } else if (status == "start_auction") {
                        if (readOnly) {
                            Alerts.error(mCtx, "Cohosts can view products but cannot control bidding.")
                            return@itemClick
                        }
                        if (isLive) {
                            if (auctionTypeId == AuctionType.LIVE.id) {
                                auctionSettingsSheet(
                                    selectedProduct?.id.toString(),
                                    selectedProduct?.pricing ?: ""
                                )
                            } else {
                                socketManager?.startAuction(
                                    viewModel.currentRoomId,
                                    listOf(selectedProduct?.id.toString()),
                                    selectedProduct?.pricing ?: "0.0",
                                    null, null, null,
                                    auctionTypeId
                                )
                                dismiss()

                            }
                        } else {
                            Alerts.error(mCtx, "Please start live show to start auction")
                            return@itemClick
                        }

                    } else if (status == "set_next") {
                        if (readOnly) {
                            Alerts.error(mCtx, "Cohosts can view products but cannot control bidding.")
                            return@itemClick
                        }
                        selectedPos = pos
                        socketManager?.pinProduct(
                            roomId = viewModel.currentRoomId,
                            productId = selectedProduct?.id.toString()
                        )
                    } else if (status == "freebie") {
                        if (readOnly) {
                            Alerts.error(mCtx, "Cohosts can view products but cannot control bidding.")
                            return@itemClick
                        }
                        // MC cmph7xsgy00g4ms8pslgxzr1u (2026-05-22): host
                        // can pick MULTIPLE products to give away in one
                        // freebie. Toggle the tapped row instead of
                        // single-selecting (which used to wipe every
                        // other selection on each tap).
                        val cur = productList[pos]
                        cur?.selected = cur?.selected != true
                        selectedPos = pos
                        bind.recycler.adapter?.notifyItemChanged(pos)
                    } else if (status == "select") {
                        // Basecamp #9929871140 (2026-05-27): randomizer-slot
                        // picker. Single-product selection — toggle the
                        // tapped row, clear any prior selection. Trey
                        // reported tapping rows did nothing because this
                        // case was missing; the picker confirm button then
                        // failed the `firstOrNull { it.selected == true }`
                        // check and showed "Please select a product".
                        productList.forEachIndexed { idx, p ->
                            if (idx != pos && p?.selected == true) {
                                p.selected = false
                                bind.recycler.adapter?.notifyItemChanged(idx)
                            }
                        }
                        val cur = productList[pos]
                        cur?.selected = cur?.selected != true
                        selectedPos = pos
                        bind.recycler.adapter?.notifyItemChanged(pos)
                    }

                }

            })

        bind.recycler.adapter = productAdapter

        surpriseProductAdapter = SurpriseProductAdapter(
            from = from,
            mList = surpriseProductList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                    val selectedProduct = surpriseProductList[pos]
                    val totalQuantity = selectedProduct?.items?.sumOf { it?.quantity ?: 0 }
                    val soldQuantity = selectedProduct?.items?.sumOf { it?.soldQuantity ?: 0 }

                    if (totalQuantity == soldQuantity) {
                        Alerts.error(mCtx, "This product is already sold")
                    } else if (status == "manage") {
                        val bottomSheetFragment = ManageSurpriseSetSheet { status ->
                            when (status) {
                                "dismiss" -> dismiss()
                                else->{
                                    surprisePage = 1
                                    loadSurpriseSets()
                                }
                            }

                        }.apply {
                            arguments = bundleOf("setData" to selectedProduct)
                        }
                        bottomSheetFragment.show(parentFragmentManager, "MANAGE_SURPRISE")
                    } else if (status == "start_auction") {
                        if (auctionTypeId == AuctionType.LIVE.id) {
                            auctionSettingsSheet(selectedProduct?.id.toString(), (selectedProduct?.price ?: 0.0).toString())
                        } else {
                            socketManager?.startAuctionBreakSpot(
                                viewModel.currentRoomId,
                                selectedProduct?.id.toString(),
                                selectedProduct?.items?.first { it?.status == "available" }?.id.toString(),
                                selectedProduct?.items?.first { it?.status == "available" }?.units?.first { it?.status == "available" }?.id.toString(),
                                (selectedProduct?.price ?: 0.0).toString(),
                                null, null, null,
                            )
                            dismiss()
                        }
                    } else if (status == "set_next") {
                    } else if (status == "freebie") {
                    }
                }
            })

        bind.surpriseRecycler.adapter = surpriseProductAdapter

        bind.close.setHapticClickListener {
            dismiss()
        }

        bind.addBtn.setHapticClickListener {
            if (from == "freebie") {
                // MC cmph7xsgy00g4ms8pslgxzr1u (2026-05-22): collect ALL
                // selected products. Falls back to single-product emit
                // for back-compat when only one is selected, but uses the
                // new multi-product socket payload when 2+ are picked.
                // The server-side `create-freebie` handler accepts both
                // shapes (see socketEvents.js patched).
                val selectedIds = productList
                    .filterNotNull()
                    .filter { it.selected == true }
                    .mapNotNull { it.id?.toString() }
                if (selectedIds.isEmpty()) {
                    Alerts.error(mCtx, "Please select at least one product")
                    return@setHapticClickListener
                }
                if (selectedIds.size == 1) {
                    socketManager?.createFreebie(
                        roomId = viewModel.currentRoomId,
                        productId = selectedIds.first(),
                        time = "1"
                    )
                } else {
                    socketManager?.createFreebieMulti(
                        roomId = viewModel.currentRoomId,
                        productIds = selectedIds,
                        time = "1"
                    )
                }
                dismiss()

            } else if (isRandomizerPicker()) {
                // Single-product selection for a randomizer slot or prize
                val selected = productList.filterNotNull().firstOrNull { it.selected == true }
                if (selected == null) {
                    Alerts.error(mCtx, "Please select a product")
                    return@setHapticClickListener
                }
                productSelectedListener?.onProductSelected(
                    selected.id ?: 0,
                    selected.title ?: "Product"
                )
                dismiss()

            } else {
                addProductSheet()
            }
        }

        socketManager?.onProductPinned { json ->
            runSafe {
                requireActivity().runOnUiThread {

                    val productId = json.optString("product_id")

                    viewModel.pinnedProducts.add(productId)

                    Log.d("TAG", "pinnedProducts: ${viewModel.pinnedProducts}")

                    productList[selectedPos]?.selected = true
                    productAdapter.notifyItemChanged(selectedPos)

                }
            }

        }

        socketManager?.onProductUnPinned { json ->
            runSafe {
                requireActivity().runOnUiThread {

                    if (viewModel.currentRoomId == json.optString("room_id")) {

                        val productId = json.optString("product_id")

                        Log.d("TAG", "pinnedProducts: ${viewModel.pinnedProducts}")


                        productList[selectedPos]?.selected = false
                        productAdapter.notifyItemChanged(selectedPos)

                        viewModel.pinnedProducts.remove(productId)

                    }
                }
            }
        }

        viewModel.getShowDetailsRepo.value = null

        viewModel.getShowDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.bottomLoader.isVisible = false
                    bind.loader.isVisible = false
                    bind.switcher.displayedChild = 0

                    // Full product list for THIS show (scoped server-side via
                    // product_ids). The All/Sold/Offers tabs + search filter it
                    // client-side.
                    showProducts.clear()
                    it.value.data?.products?.let { p -> showProducts.addAll(p) }

                    applyShowFilter()
                }

                is Resource.Error -> {
                    bind.bottomLoader.isVisible = false
                    bind.loader.isVisible = false

                    it.parse(mCtx, javaClass.simpleName, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }

                else -> {}
            }
        }

        viewModel.getSurpriseProductRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.surpriseBottomLoader.isVisible = false
                    bind.switcher.displayedChild = 1
                    val mData = it.value.data
                    if (surprisePage == 1) {
                        surpriseProductList.clear()
                    }

                    if (mData != null) {
                        surpriseProductList.addAll(mData)
                        surpriseProductAdapter.notifyDataSetChanged()
                    }

                    surpriseProductList.forEach {
                    }

                    if (surpriseProductList.isEmpty()) {
                        bind.surpriseNoDataView.isVisible = true
                        bind.surpriseRecycler.isVisible = false
                    } else {
                        bind.surpriseNoDataView.isVisible = false
                        bind.surpriseRecycler.isVisible = true
                    }

                    surpriseIsLoading = surprisePage >= (it.value.totalPage ?: 0)

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.surpriseBottomLoader.isVisible = false

                    it.parse(mCtx, javaClass.simpleName, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }

                else -> {}
            }
        }

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            if (!isRandomizerPicker()) return@observe
            when (it) {
                is Resource.Success -> {
                    bind.bottomLoader.isVisible = false
                    bind.loader.isVisible = false
                    bind.switcher.displayedChild = 0
                    isLoading = false
                    productTotalPage = it.value.totalPage ?: page

                    if (page == 1) {
                        productList.clear()
                    }
                    it.value.products?.let { products -> productList.addAll(products) }
                    productAdapter.notifyDataSetChanged()

                    bind.noDataView.isVisible = productList.isEmpty()
                    bind.recycler.isVisible = productList.isNotEmpty()
                }

                is Resource.Error -> {
                    bind.bottomLoader.isVisible = false
                    bind.loader.isVisible = false
                    isLoading = false

                    it.parse(mCtx, javaClass.simpleName, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }

                else -> {}
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData(loadMore: Boolean = false) {
        if (isRandomizerPicker()) {
            bind.switcher.displayedChild = 0
            bind.bottomLoader.isVisible = loadMore
            bind.loader.isVisible = !loadMore
            val query = bind.search.value().trim()
            val currentUserId = Prefs(mCtx).getUserData()?.id?.toString()
            viewModel.getUserProducts(
                userId = currentUserId?.request(),
                page = page.toString().request(),
                search = query.ifEmpty { null }?.request()
            )
            return
        }

        // Fetch the CURRENT show's product list (scoped to show_id), not the
        // seller's whole catalog. All/Sold/Offers tabs filter this list.
        bind.loader.isVisible = true
        bind.bottomLoader.isVisible = false
        val showId = viewModel.showId.takeIf { it.isNotBlank() }
            ?: viewModel.currentShowData?.id?.toString()
        viewModel.getShowDetails(showId)
    }

    /**
     * Apply the active tab (All / Sold / Offers) + search query over the
     * current show's product list, then render the filtered result.
     *  - All    = every product in the show.
     *  - Sold   = sold / inactive products.
     *  - Offers = products that received bids / offers (bid_count > 0).
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun applyShowFilter() {
        bind.loader.isVisible = false
        bind.bottomLoader.isVisible = false
        bind.switcher.displayedChild = 0

        val query = bind.search.value().trim().lowercase()

        val filtered = showProducts.filter { p ->
            p ?: return@filter false
            val matchesTab = when (chipIndex) {
                1 -> p.status == "sold" || p.status == "inactive"
                2 -> (p.bidCount ?: 0) > 0
                else -> true // All
            }
            val matchesSearch =
                query.isEmpty() || (p.title?.lowercase()?.contains(query) == true)
            matchesTab && matchesSearch
        }

        productList.clear()
        productList.addAll(filtered)
        productList.forEach {
            if (viewModel.pinnedProducts.contains(it?.id.toString())) {
                it?.selected = true
            }
        }
        productAdapter.notifyDataSetChanged()

        if (productList.isEmpty()) {
            bind.noDataView.isVisible = true
            bind.recycler.isVisible = false
        } else {
            bind.noDataView.isVisible = false
            bind.recycler.isVisible = true
        }
    }

    private fun loadSurpriseSets(loadMore: Boolean = false) {
        bind.bottomLoader.isVisible = loadMore
        bind.loader.isVisible = !loadMore
        viewModel.getSurpriseProduct(surprisePage.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun auctionSettingsSheet(productId: String, price: String) {
        AuctionSettingsSheetHelper.show(
            context = mCtx,
            initialPrice = price
        ) { result ->
            val productIds = mutableListOf(productId)

            socketManager?.startAuction(
                viewModel.currentRoomId,
                productIds,
                result.startingBid,
                result.requiredTimeSeconds,
                result.counterTimerSeconds,
                result.suddenDeath,
                auctionTypeId
            )
            dismiss()
        }
    }

    private fun addProductSheet() {
        val sheetView = SelectProductTypeSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.select_product_type_sheet,
                null,
                false
            )
        )

        val mSellSheet = Alerts.appBottomSheet(mCtx, true, sheetView)

        val sellList = mutableListOf(
            SellModel(
                R.drawable.ic_tag_outline,
                R.color.primaryContainer,
                "Create Quality Listing",
                ""
            ),
            SellModel(
                R.drawable.ic_tile_grid,
                R.color.primaryContainer,
                "Create a Surprise Set",
                ""
            )
        )


        val exploreAdapter = SellAdapter(sellList, "", object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {
                when (pos) {
                    1 -> {
                        addProductLauncher.launch(mCtx.toListProduct().putExtra("from", "surprise"))
                    }

                    else -> {
                        addProductLauncher.launch(
                            mCtx.toListProduct().putExtra("category", viewModel.categoryId)
                        )
                    }
                }
                mSellSheet.dismiss()
            }
        })

        sheetView.recycler.adapter = exploreAdapter

        sheetView.close.setHapticClickListener {
            mSellSheet.dismiss()
        }
        mSellSheet.show()
    }
}
