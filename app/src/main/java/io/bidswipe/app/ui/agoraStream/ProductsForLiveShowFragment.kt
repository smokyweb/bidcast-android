package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@AndroidEntryPoint
class ProductsForLiveShowFragment : BottomSheetDialogFragment() {

    private lateinit var productAdapter: FirebaseProductAdapter
    private var productList = mutableListOf<GetProductsResponse.Data?>()

    private lateinit var mCtx: Context

    private var _binding: FragmentProductsForLiveShowBinding? = null
    private val bind get() = _binding!!

    private val viewModel by viewModels<DashViewModel>()

    private var page = 1
    private var isLoading = false
    private var selectedPos = -1
    private var saleType = ""

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


        saleType = "auction"

        bind.chipGroup.apply {
            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Auction",
                    selected = false,
                    closeIconVisible = false
                )
            )
            addView(
                Utils.makeAChip(
                    mCtx,
                    text = "Buy Now",
                    selected = false,
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
//                    bind.loader.isVisible = true

                    saleType = when (index) {
                        0 -> "auction"
                        1 -> "buy_now"
                        2 -> "sold"
                        3 -> "accept_offers"
                        else -> ""
                    }

                    page = 1
                    loadData()
                }
            }
        }


        bind.chipGroup.check(bind.chipGroup[0].id)



//        bind.bottomLoader.isVisible = true
//        loadData()

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.bottomLoader.isVisible = false
                    val mData = it.value.data

                    if (page == 1) {
                        productList.clear()
                    }

                    if (mData != null) {
                        productList.addAll(mData)
                        productAdapter.notifyDataSetChanged()
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

                bind.bottomLoader.isVisible = true
                page = 1
                loadData()
            }
        })

        productAdapter =
            FirebaseProductAdapter(
                from = "live_show",
                mList = productList,
                object : RecyclerClicks {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun itemClick(pos: Int, status: String?) {
                        if (productList[pos]?.status == "sold") {
                            Alerts.error(mCtx, "This product is already sold")
                        } else {
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

            if (selectedPos == -1) {
                Alerts.error(mCtx, "Please select a product")
                return@setHapticClickListener
            }

//            val isAnyProductLive = productList.any { it?.isCurrent == true }
//
//            if (isAnyProductLive) {
//                Alerts.error(mCtx, "One Product is Already Live")
//                return@setHapticClickListener
//            }

            val selectedProduct = productList[selectedPos]

//            socketManager?.setNextProduct(roomID, selectedProduct?.id)
//            productSheet.dismiss()

        }

    }

    private fun loadData() {
        viewModel.getUserProducts(
            page = page.toString().request(),
            saleType = saleType.ifEmpty { null }?.request(),
            search = bind.search.value().ifEmpty { null }?.request(),
            categoryIds = "14".request()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}