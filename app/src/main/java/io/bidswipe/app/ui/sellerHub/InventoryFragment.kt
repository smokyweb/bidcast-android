package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.controller.InventoryFilterAdapter
import io.bidswipe.app.controller.InventoryFilterModel
import io.bidswipe.app.controller.SortingOptionAdapter
import io.bidswipe.app.databinding.FragmentInventoryBinding
import io.bidswipe.app.databinding.SortingOptionSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct

@SuppressLint("NotifyDataSetChanged")
class InventoryFragment : BaseFragment<SellerHubViewModel, FragmentInventoryBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentInventoryBinding.inflate(inflater, view, false)

	private var itemList = mutableListOf<GetMyInventoryResponse.Data?>()
	private var filteredList = mutableListOf<GetMyInventoryResponse.Data?>()
	private lateinit var adapter: InventoryAdapter
	private var isLoading = false
	private var page = 1
	private val optionList = mutableListOf<LiveMoreOption?>()
	private var selectedTab = "active"

	private lateinit var filterSheet: BottomSheetBehavior<ConstraintLayout>

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val from = requireActivity().intent.getStringExtra("from")

//        from = arguments?.getString("from")

//		setupFilterChips()

		val isSelectionMode = from == "addProduct"

		if (isSelectionMode) {
			bind.tabs.isVisible = false
			bind.addNewProduct.text = buildString {
				append("Add Selected")
			}
			bind.addNewProduct.setHapticClickListener {
				val selectedItems = itemList.filter { it?.selected == true }
				if (selectedItems.isEmpty()) {
					Toast.makeText(mCtx, "Please select at least one product", Toast.LENGTH_SHORT).show()
					return@setHapticClickListener
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
			bind.addNewProduct.setHapticClickListener {
				startActivity(mCtx.toListProduct())
			}

		}

		adapter = InventoryAdapter(filteredList, isSelectionMode, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				log("POSITION : $pos STATUS : ${filteredList.size}")
				filteredList[pos]?.let { item ->
					if (status == "delete") {
						deleteProductDialog(item.id.toString(), pos)
					} else if (status == "longClick") {

						log("LONG CLICK")

						sortOptionSheet(item, pos)


					} else {
						if (isSelectionMode) {
							item.selected = !(item.selected ?: false)
							adapter.notifyItemChanged(pos)
						} else {
//							startActivity(mCtx.toListProduct().putExtra("product" , item))
						}
					}
				}
			}
		})

//		bind.recycler.adapter = adapter
		bind.searchBox.isEndIconVisible = false

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				val query = s?.toString()?.trim() ?: ""
				bind.searchBox.isEndIconVisible = query.isNotEmpty()
				page = 1
				isLoading = false

				if (query.isNotEmpty()) {
					bind.loader.isVisible = true
					bind.recycler.isVisible = false
					bind.noData.isVisible = false
					viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request(), search = query.request())
				} else {
					viewModel.getMyInventory(
						status = selectedTab.request(), page = page.toString().request(),
					)
				}
			}
		})
		bind.searchBox.setEndIconOnClickListener {

			bind.search.text?.clear()
			bind.searchBox.isEndIconVisible = false
			page = 1
			viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request())
		}

		bind.header.onBackClick {
			finish()
		}

		bind.main.setHapticClickListener {
			hideKeyboard(it)
		}
		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}

		setUpfilterSheet()
		bind.close.setHapticClickListener {
			filterSheet.state = BottomSheetBehavior.STATE_EXPANDED
		}

		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (filteredList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request())
					}
				}
			}
		})

		bind.recycler.adapter = adapter

		bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				selectedTab = tab?.text.toString().lowercase()
				page = 1
				bind.search.text?.clear()
				bind.searchBox.isEndIconVisible = false
				bind.loader.isVisible = true
				viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request())
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {}
			override fun onTabReselected(tab: TabLayout.Tab?) {}
		})

		bind.swipeRefreshLayout.setOnRefreshListener {
			bind.search.setText("")
			page = 1
			isLoading = false
			itemList.clear()
			filteredList.clear()
			viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request())
		}

		bind.noInternet.onClick {
			bind.noInternet.isVisible = false
			bind.bottomLoader.isVisible = false
			bind.loader.isVisible = true

			page = 1
			isLoading = false
			itemList.clear()
			filteredList.clear()
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			viewModel.getMyInventory(status = selectedTab.request(), page = page.toString().request())
		}

		bind.loader.isVisible = true

		viewModel.getMyInventory(status = "active".request(), page = "1".request())

		viewModel.getMyInventoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.noInternet.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.addNewProduct.isVisible = true

					val mData = it.value.data ?: emptyList()

					if (page == 1) {
						itemList.clear()
						filteredList.clear()
					}
					itemList.addAll(mData)

					filteredList.addAll(mData)

					if (filteredList.isNotEmpty()) {
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

		viewModel.deleteProductRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
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

		viewModel.updateProductStatusRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data

					bind.loader.isVisible = true

					page = 1

					viewModel.getMyInventory(status = selectedTab.request(), page = "1".request())

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
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

	private fun deleteProductDialog(productId: String, position: Int) {
		AppBottomSheet(
			mCtx,
			R.drawable.trash,
			"Delete!",
			"Are you sure you want to delete?",
			primaryBtnText = "Yes",
			secondaryBtnText = "No",
			canCancel = true,
			showSecondary = true,
			iconPadding = 16,
			alertType = AlertType.ERROR,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
					bind.loader.isVisible = true
					viewModel.deleteProduct(productId)

					filteredList.removeAt(position)
					adapter.notifyItemRemoved(position)
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}

		).show()

	}

	private fun setupFilterChips() {
		if (bind.filterChipGroup.childCount > 0) {
			return // Already set up
		}

		val filters = listOf("Marketplace", "Category", "Price range", "Condition", "Format")
		filters.forEachIndexed { index, filter ->
			val chip = Utils.makeAChip(
				mCtx = mCtx,
				text = filter,
				selected = index == 0,
				closeIconVisible = false,
				chipPadding = 12,
			)
			chip.setOnClickListener {
				// Update chip selection
				bind.filterChipGroup.check(chip.id)
			}
			bind.filterChipGroup.addView(chip)
		}

		// Select first chip (All)
		if (bind.filterChipGroup.isNotEmpty()) {
			bind.filterChipGroup.check(bind.filterChipGroup.getChildAt(0).id)
		}

	}

	private fun sortOptionSheet(data: GetMyInventoryResponse.Data, position: Int) {

		val sortingOptionSheetBinding = SortingOptionSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.sorting_option_sheet,
				null,
				false
			)
		)

		val sortingOptionSheet = Alerts.appBottomSheet(mCtx, true, sortingOptionSheetBinding)

		sortingOptionSheetBinding.title.text = "Actions"

		optionList.clear()

		optionList.add(LiveMoreOption("Edit", isSelected = false))
		optionList.add(LiveMoreOption(if (selectedTab == "active") "Deactivate" else "Activate", isSelected = false))
		optionList.add(LiveMoreOption("Delete", isSelected = false))

		sortingOptionSheetBinding.optionRecycler.adapter = SortingOptionAdapter(optionList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				optionList.forEachIndexed { index, item ->
					item?.isSelected = index == pos
				}

				sortingOptionSheetBinding.optionRecycler.adapter?.notifyDataSetChanged()

				var status = ""

				page = 1

				status = when (pos) {
					0 -> {
						"edit"
					}

					1 -> {
						if (selectedTab == "active") "inactive" else "active"
					}

					2 -> {
						"delete"
					}

					else -> {
						"edit"
					}

				}

				when (status) {
					"edit" -> {
						startActivity(mCtx.toListProduct().putExtra("product", data))
					}

					"delete" -> {
						deleteProductDialog(data.id.toString(), position)
					}

					else -> {
						bind.loader.isVisible = true

						viewModel.updateProductStatus(data.id.toString().request(), status.request())

					}
				}

				sortingOptionSheet.dismiss()

			}
		})

		sortingOptionSheetBinding.close.setHapticClickListener {
			sortingOptionSheet.dismiss()
		}

		sortingOptionSheet.show()

	}

	private fun filterOptionSheet(data: GetMyInventoryResponse.Data, position: Int) {

		val sortingOptionSheetBinding = SortingOptionSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.sorting_option_sheet,
				null,
				false
			)
		)

		val sortingOptionSheet = Alerts.appBottomSheet(mCtx, true, sortingOptionSheetBinding)

		sortingOptionSheetBinding.title.text = "Actions"

		optionList.clear()

		optionList.add(LiveMoreOption("Edit", isSelected = false))
		optionList.add(LiveMoreOption(if (selectedTab == "active") "Deactivate" else "Activate", isSelected = false))
		optionList.add(LiveMoreOption("Delete", isSelected = false))

		sortingOptionSheetBinding.optionRecycler.adapter = SortingOptionAdapter(optionList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				optionList.forEachIndexed { index, item ->
					item?.isSelected = index == pos
				}

				sortingOptionSheetBinding.optionRecycler.adapter?.notifyDataSetChanged()

				var status = ""

				page = 1

				status = when (pos) {
					0 -> {
						"edit"
					}

					1 -> {
						if (selectedTab == "active") "inactive" else "active"
					}

					2 -> {
						"delete"
					}

					else -> {
						"edit"
					}

				}

				if (status == "edit") {
					startActivity(mCtx.toListProduct().putExtra("product", data))
				} else if (status == "delete") {

					deleteProductDialog(data.id.toString(), position)

				} else {
					bind.loader.isVisible = true

					viewModel.updateProductStatus(data.id.toString().request(), status.request())

				}

				sortingOptionSheet.dismiss()

			}
		})

		sortingOptionSheetBinding.close.setHapticClickListener {
			sortingOptionSheet.dismiss()
		}

		sortingOptionSheet.show()
	}

	private fun setUpfilterSheet() {
		BottomSheetBehavior.from(bind.filtersheet.root)

		filterSheet = BottomSheetBehavior.from(bind.filtersheet.root).also {
			it.peekHeight = 0
			it.isHideable = true
			it.isDraggable = false
			it.isFitToContents = false
		}

		filterSheet.state = BottomSheetBehavior.STATE_COLLAPSED

		val conditionList = mutableListOf(
			InventoryFilterModel.InnerModel(null, "New"),
			InventoryFilterModel.InnerModel(null, "Like New"),
			InventoryFilterModel.InnerModel(null, "Gently Loved"),
			InventoryFilterModel.InnerModel(null, "Well Loved"),
			InventoryFilterModel.InnerModel(null, "Other"),
			InventoryFilterModel.InnerModel(null, "Trending"),
		)
		val sortList = mutableListOf(
			InventoryFilterModel.InnerModel(null, "Newest First"),
			InventoryFilterModel.InnerModel(null, "Oldest First"),
			InventoryFilterModel.InnerModel(null, "Price: Low to High"),
			InventoryFilterModel.InnerModel(null, "Price: High to Low"),
		)

		val filterList = mutableListOf(
			InventoryFilterModel(
				R.drawable.ic_tile_grid,
				"Category",
				"category",
				App.categoryList.map { InventoryFilterModel.InnerModel(it?.id, it?.name) }
					.toMutableList()
			),
			InventoryFilterModel(R.drawable.ic_star, "Condition", "condition", conditionList),
			InventoryFilterModel(R.drawable.ic_product_filter, "Sort By", "sort", sortList),
		)

		bind.filtersheet.recycler.adapter =
			InventoryFilterAdapter(filterList, object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {
					when (status) {
						"open" -> {
							if (filterList[pos].isOpened) {
								filterList[pos].isOpened = false
								bind.filtersheet.recycler.adapter?.notifyItemChanged(
									pos
								)
							} else {
								filterList.forEachIndexed { index, model ->
									model.isOpened = pos == index
									bind.filtersheet.recycler.adapter?.notifyItemChanged(
										index,
										model
									)
								}
							}
						}
					}
				}
			})

		bind.filtersheet.priceMoreIcon.setOnClickListener {
			bind.filtersheet.priceLayout.isExpanded = !bind.filtersheet.priceLayout.isExpanded
		}

		bind.filtersheet.close.setHapticClickListener {
			filterSheet.state = BottomSheetBehavior.STATE_COLLAPSED
		}


	}
}