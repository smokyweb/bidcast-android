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
import io.bidswipe.app.controller.SurpriseProductAdapter
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
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
    private var surpriseProductList = mutableListOf<GetSurpriseProductsResponse.Data?>()
    private lateinit var surpriseProductAdapter: SurpriseProductAdapter
    private var surprisePage = 1
    private var surpriseIsLoading = false
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
                if (chipIndex == 4) {
                    surprisePage = 1
                    loadSurpriseSets()
                } else {
                    page = 1
                    loadData(1)
                }
            }
        }

    private var productTypesList = mutableListOf("Auction", "Buy Now", "Sold", "Offers", "Surprise Sets")
    var chipIndex = 0

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

        bind.switcher.showNext()

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

                    Log.d("TAG", "onViewCreated: AUCTION $auctionTypeId--$saleType--$chipIndex")
                    when (chipIndex) {
                        0 -> {
                            saleType = "auction"
                            type = ""
                            status = ""
                            page = 1
                            loadData(2)
                        }

                        1 -> {
                            type = "buy_now"
                            status = ""
                            saleType = ""
                            page = 1
                            loadData(2)
                        }

                        2 -> {
                            status = "inactive"
                            type = ""
                            saleType = ""

                            page = 1
                            loadData(2)
                        }

                        3 -> {
                            saleType = "accept_offers"
                            type = ""
                            status = ""

                            page = 1
                            loadData(2)
                        }

                        4 -> {
                            loadSurpriseSets()
                        }

                        else -> ""
                    }

                }
            }
        }

        bind.chipGroup.check(bind.chipGroup[0].id)

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.bottomLoader.isVisible = false
                    bind.loader.isVisible = false
                    bind.switcher.showNext()
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
                    bind.switcher.showPrevious()
                    val mData = it.value.data
                    if (surprisePage == 1) {
                        surpriseProductList.clear()
                    }
}