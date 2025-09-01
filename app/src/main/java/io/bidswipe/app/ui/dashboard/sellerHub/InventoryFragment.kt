package io.bidswipe.app.ui.dashboard.sellerHub

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.databinding.FragmentInventoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toListProduct
@SuppressLint("NotifyDataSetChanged")
class InventoryFragment : BaseFragment<SellerHubViewModel, FragmentInventoryBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentInventoryBinding.inflate(inflater, view, false)

    private var itemList = mutableListOf<GetMyInventoryResponse.Data?>()
    private lateinit var adapter: InventoryAdapter
    private var isLoading = false
    private var page = 1
    private var selectedTab = "active"
    private var from: String? = null

    private val mClick = object : RecyclerClicks {

        override fun itemClick(pos: Int, status: String?) {
            startActivity(mCtx.toListProduct().putExtra("product", itemList[pos]))

        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val from = requireActivity().intent.getStringExtra("from")

//        from = arguments?.getString("from")

        val isSelectionMode = from == "addProduct"

        if (isSelectionMode) {
            bind.tabs.isVisible = false
            bind.addNewProduct.text = "Add Selected"
            bind.addNewProduct.setOnClickListener {
                val selectedItems = itemList.filter { it?.selected == true }
                if (selectedItems.isEmpty()) {
                    Toast.makeText(mCtx, "Please select at least one product", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedList = ArrayList<GetMyInventoryResponse.Data>()
                selectedItems.forEach { it?.let { selectedList.add(it) } }

                val intent = Intent()
                intent.putExtra("selectedProducts", selectedList)
                activity?.setResult(Activity.RESULT_OK, intent)
                finish()
            }


        } else {
            bind.addNewProduct.text = getString(R.string.new_product)
            bind.addNewProduct.setOnClickListener {
                startActivity(mCtx.toListProduct())
            }

        }

        adapter = InventoryAdapter(itemList, isSelectionMode, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                if(isSelectionMode) {
                    itemList[pos]?.selected = !(itemList[pos]?.selected ?: false)
                    adapter.notifyItemChanged(pos)

                    val selectedCount = itemList.count { it?.selected == true }
                    bind.addNewProduct.text =
                        if (selectedCount > 0) "Add ($selectedCount)" else "Add Selected"
                }
                else{
                    startActivity(mCtx.toListProduct().putExtra("product", itemList[pos]))
                }
            }
        })

        bind.header.onBackClick {
            finish()
        }

        bind.main.setOnClickListener {
            hideKeyboard(it)
        }
        bind.root.setOnClickListener {
            hideKeyboard(it)
        }
        bind.recycler.adapter = adapter

        bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTab = tab?.text.toString().lowercase()
                page = 1
                bind.loader.isVisible = true
                viewModel.getMyInventory(selectedTab.request(), page.toString().request())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (itemList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        bind.bottomLoader.isVisible = true
                        viewModel.getMyInventory(selectedTab.request(), page.toString().request())
                    }
                }
            }
        })

        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            isLoading = false
            itemList.clear()
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            viewModel.getMyInventory(selectedTab.request(), page.toString().request())
        }

        bind.noInternet.onClick {
            bind.noInternet.isVisible = false
            bind.bottomLoader.isVisible = false
            bind.loader.isVisible = true

            page = 1
            isLoading = false
            itemList.clear()
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            viewModel.getMyInventory(selectedTab.request(), page.toString().request())
        }

        bind.loader.isVisible = true

        viewModel.getMyInventory("active".request(), "1".request())
        viewModel.getMyInventoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.addNewProduct.isVisible = true

                    val mData = it.value.data

                    if (page == 1) {
                        itemList.clear()
                    }

                    if (mData != null) {
                        itemList.addAll(mData)
                    }

                    if (itemList.isNotEmpty()) {
                        bind.recycler.isVisible = true
                        bind.noData.isVisible = false
                    } else {
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = true
                        bind.addNewProduct.isVisible = false
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)

                    adapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false


                    if (it.isNetworkError) {
                        bind.loader.isVisible = false
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.addNewProduct.isVisible = false

                    } else {
                        bind.noInternet.isVisible = false
                        bind.addNewProduct.isVisible = true
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

}