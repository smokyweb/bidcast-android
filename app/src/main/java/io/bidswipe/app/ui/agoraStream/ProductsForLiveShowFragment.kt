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
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.PriceFormatter
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

    private lateinit var productAdapter: FirebaseProductAdapter
    private var productList = mutableListOf<Product?>()

    private lateinit var mCtx: Context

    private var _binding: FragmentProductsForLiveShowBinding? = null
    private val bind get() = _binding!!

    val viewModel: DashViewModel by activityViewModels()

    private var page = 1
    private var isLoading = false
    private var selectedPos = -1
    private var saleType = ""
    private var type = ""
    private var status = ""
    private var socketManager: SocketManager? = null

    var from = "live_show"
    var auctionTypeId = AuctionType.LIVE.id

    private var addProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("TAG", "CALLL RESULT")
            if (result.resultCode == Activity.RESULT_OK) {
                page = 1
                loadData(1)
            }
        }

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
                bottomSheetDialog.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.background)
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
        }

        if (auctionTypeId == AuctionType.BUY_NOW.id) {
            type = "buy_now"
            saleType = ""
        } else {
            saleType = "auction"
            type = ""
        }

        if (from == "freebie") {
            bind.title.text = "Select Product for Freebie"
            bind.addBtn.text = "Start Freebie"
        }

        socketManager = SocketManager.getInstance(requireContext())

        bind.chipGroup.apply {
            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Auction",
                    selected = auctionTypeId == AuctionType.LIVE.id,
                    closeIconVisible = false
                )
            )
            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Buy Now",
                    selected = auctionTypeId == AuctionType.BUY_NOW.id,
                    closeIconVisible = false
                )
            )
            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Sold",
                    selected = false,
                    closeIconVisible = false
                )
            )

            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Offers",
                    selected = false,
                    closeIconVisible = false
                )
            )

            setOnCheckedStateChangeListener { chipGroup, _ ->
                runSafe {
                    val chipId = chipGroup.checkedChipId
                    val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))

                    Log.d("TAG", "onViewCreated: AUCTION $auctionTypeId--$saleType--$index")
                    when (index) {
                        0 -> {
                            saleType = "auction"
                            type = ""
                            status = ""
                        }

                        1 -> {
                            type = "buy_now"
                            status = ""
                            saleType = ""
                        }

                        2 -> {
                            status = "inactive"
                            type = ""
                            saleType = ""
                        }

                        3 -> {
                            saleType = "accept_offers"
                            type = ""
                            status = ""
                        }

                        else -> ""
                    }

                    page = 1
                    loadData(2)
                }
            }
        }

        bind.chipGroup.check(bind.chipGroup[0].id)

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.bottomLoader.isVisible = false
                    val mData = it.value.products

                    if (page == 1) {
                        productList.clear()
                    }

                    if (mData != null) {
                        productList.addAll(mData)
                        productAdapter.notifyDataSetChanged()
                    }

                    productList.forEach {
                        if (viewModel.pinnedProducts.contains(it?.id.toString())) {
                            it?.selected = true
                        }
                    }

                    if (productList.isEmpty()) {
                        bind.noDataView.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noDataView.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)
                }

                is Resource.Error -> {
                    bind.bottomLoader.isVisible = false

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
                        loadData(3)
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

                bind.bottomLoader.isVisible = true
                page = 1
                loadData(4)
            }
        })

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
                        if (auctionTypeId == AuctionType.LIVE.id) {
                            auctionSettingsSheet(selectedProduct?.id.toString(), selectedProduct?.pricing ?: "")

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

                    } else if (status == "set_next") {
                        selectedPos = pos
                        socketManager?.pinProduct(roomId = viewModel.currentRoomId, productId = selectedProduct?.id.toString())
                    } else if (status == "freebie") {
                        productList.forEachIndexed { index, item ->
                            item?.selected = index == pos
                            bind.recycler.adapter?.notifyDataSetChanged()
                        }
                        selectedPos = pos
                    }

                }

            })

        bind.recycler.adapter = productAdapter

        bind.close.setHapticClickListener {
            dismiss()
        }

        bind.addBtn.setHapticClickListener {
            if (from == "freebie") {
                if (selectedPos == -1) {
                    Alerts.error(mCtx, "Please select a product")
                    return@setHapticClickListener
                }

                val selectedProduct = productList[selectedPos]

                socketManager?.createFreebie(roomId = viewModel.currentRoomId, productId = selectedProduct?.id.toString(), time = "1")
                dismiss()

            } else {
                addProductLauncher.launch(mCtx.toListProduct().putExtra("category", viewModel.categoryId))
            }
        }

    }

    private fun loadData(call: Int) {
        Log.d("TAG", "$call loadData: $saleType")
        bind.bottomLoader.isVisible = true
        viewModel.getUserProducts(
            page = page.toString().request(),
            saleType = saleType.ifEmpty { null }?.request(),
            type = type.ifEmpty { null }?.request(),
            status = status.ifEmpty { null }?.request(),
            search = bind.search.value().ifEmpty { null }?.request(),
            categoryIds = viewModel.categoryId.request()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun auctionSettingsSheet(productId: String, price: String) {
        var selectedCounterTimer = 5
        var selectedRequiredTime = 30

        val auctionSettingsSheetBind = AuctionSettingsSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.auction_settings_sheet,
                null,
                false
            )
        )

        val sheet = Alerts.appBottomSheet(mCtx, true, auctionSettingsSheetBind)

        val extraTimer = listOf(5, 7, 10)
        extraTimer.forEachIndexed { index, time ->
            val chip = Utils.makeAChip(
                mCtx = mCtx,
                text = "${time}s",
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                auctionSettingsSheetBind.timerChips.check(chip.id)
                selectedCounterTimer = time
            }
            auctionSettingsSheetBind.timerChips.addView(chip)
        }

        val requiredTimeList = listOf(15, 30, 45)
        val requiredTimeAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            requiredTimeList
        )

        auctionSettingsSheetBind.requiredTime.setAdapter(requiredTimeAdapter)

        auctionSettingsSheetBind.requiredTime.setText("30s", false)

        auctionSettingsSheetBind.requiredTime.setOnItemClickListener { _, _, position, _ ->
            selectedRequiredTime = requiredTimeList[position]
            auctionSettingsSheetBind.requiredTime.setText("${requiredTimeList[position]}s", false)
        }

        auctionSettingsSheetBind.requiredTime.setHapticClickListener {
            auctionSettingsSheetBind.requiredTime.showDropDown()
        }

        auctionSettingsSheetBind.startingBid.addTextChangedListener(PriceFormatter(auctionSettingsSheetBind.startingBid))
        auctionSettingsSheetBind.startingBid.setText(price)

        auctionSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }

        auctionSettingsSheetBind.suddenDeath.setOnCheckedChangeListener { _, v ->
            auctionSettingsSheetBind.counterTimerLayout.isVisible = !v
            if (v) selectedCounterTimer = 0
        }

        auctionSettingsSheetBind.start.setHapticClickListener {

            when {
                selectedRequiredTime == 0 -> {
                    Alerts.error(mCtx, "Please select required time")
                    return@setHapticClickListener
                }

                selectedCounterTimer == 0 && !auctionSettingsSheetBind.suddenDeath.isChecked -> {
                    Alerts.error(mCtx, "Please select counter timer")
                    return@setHapticClickListener
                }

                auctionSettingsSheetBind.startingBid.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter starting bid")
                    return@setHapticClickListener
                }

                else -> {
                    val productIds = mutableListOf<String>()
                    productIds.add(productId)

                    socketManager?.startAuction(
                        viewModel.currentRoomId,
                        productIds,
                        auctionSettingsSheetBind.startingBid.value(),
                        selectedRequiredTime,
                        selectedCounterTimer,
                        auctionSettingsSheetBind.suddenDeath.isChecked,
                        auctionTypeId
                    )
                    sheet.dismiss()
                    dismiss()

                }

            }

            /* auctionSettingsSheetBind.startingBid.value()
             selectedRequiredTime
             selectedCounterTimer
             auctionSettingsSheetBind.suddenDeath.isChecked*/

        }

        sheet.show()

    }

}