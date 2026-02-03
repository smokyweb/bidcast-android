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
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged")
class InventoryFragment : BaseFragment<SellerHubViewModel, FragmentInventoryBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentInventoryBinding.inflate(inflater, view, false)

	private var itemList = mutableListOf<Product?>()
	private var filteredList = mutableListOf<Product?>()
	private lateinit var adapter: InventoryAdapter
	private var isLoading = false
	private var page = 1
	private val optionList = mutableListOf<LiveMoreOption?>()
	private var selectedTab = "active"

	private lateinit var filterSheet: BottomSheetBehavior<ConstraintLayout>

	private var categoryIds = mutableListOf<Int?>()
	private var conditions = mutableListOf<String?>()
	private var minPrice: String? = null
	private var maxPrice: String? = null
	private var sort: String? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val from = requireActivity().intent.getStringExtra("from")
		val isSelectionMode = from == "addProduct"


		if (from == "addProduct") {
			val categoryId = requireActivity().intent.getStringExtra("categoryId")
			categoryIds.add(categoryId?.toInt())
		}

		if (isSelectionMode) {
			bind.tabs.isVisible = false
			bind.addNewProduct.text = buildString {
				append("Add Selected")
			}
			bind.addNewProduct.setHapticClickListener {
				val selectedItems = itemList.filter { it?.selected == true }
				if (selectedItems.isEmpty()) {
					Toast.makeText(mCtx, "Please select at least one product", Toast.LENGTH_SHORT)
						.show()
					return@setHapticClickListener
				}

				val selectedList = ArrayList<Product>()
				selectedItems.forEach { it?.let { selectedList.add(it) } }

				val intent = Intent()
				intent.putExtra("selectedProducts", selectedList)
				activity?.setResult(Activity.RESULT_OK, intent)
				finish()
			}

		} else {
			bind.addNewProduct.text = getString(R.string.new_product)
			bind.addNewProduct.setHapticClickListener {
				startActivity(mCtx.toListProduct().putExtra("from","surprise"))
			}

		}

		adapter = InventoryAdapter(filteredList, isSelectionMode, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

				log("POSITION : $pos STATUS : ${status}")
				filteredList[pos]?.let { item ->
					when (status) {
						"delete" -> {
							deleteProductDialog(item.id.toString(), pos)
						}

						"active", "inactive" -> {

							log("Status : $status")

							bind.loader.isVisible = true

							viewModel.updateProductStatus(
								item.id.toString().request(),
								status.request()
							)

						}

						"edit" -> {
							startActivity(mCtx.toListProduct().putExtra("product", item))
						}

						else -> {
							if (isSelectionMode) {
								item.selected = !(item.selected ?: false)
								adapter.notifyItemChanged(pos)
							} else {
								startActivity(
									Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
										"productId", item.id.toString()
									)
								)
							}
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
					getInventory(query)
				} else {
					getInventory()
				}
			}
		})

		bind.searchBox.setEndIconOnClickListener {
			bind.search.text?.clear()
			bind.searchBox.isEndIconVisible = false
			page = 1
			getInventory()
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

		setUpFilterSheet()
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
						getInventory()
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
				getInventory()
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
			getInventory()
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
			getInventory()
		}

		bind.loader.isVisible = true
		getInventory()
		viewModel.getMyInventoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.noInternet.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.addNewProduct.isVisible = true

					val mData = it.value.products ?: emptyList()

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
					viewModel.getMyInventoryRepo.value = null

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

					bind.loader.isVisible = true
					page = 1
					getInventory()
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
		optionList.add(
			LiveMoreOption(
				if (selectedTab == "active") "Deactivate" else "Activate",
				isSelected = false
			)
		)
		optionList.add(LiveMoreOption("Delete", isSelected = false))

		sortingOptionSheetBinding.optionRecycler.adapter =
			SortingOptionAdapter(optionList, object : RecyclerClicks {
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

							viewModel.updateProductStatus(
								data.id.toString().request(),
								status.request()
							)

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

	private fun setUpFilterSheet() {
		BottomSheetBehavior.from(bind.filtersheet.root)

		filterSheet = BottomSheetBehavior.from(bind.filtersheet.root).also {
			it.peekHeight = 0
			it.isHideable = true
			it.isDraggable = true
			it.isFitToContents = false
			it.skipCollapsed=true
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
					.toMutableList(),
				isMultiSelection = true
			),
			InventoryFilterModel(
				R.drawable.ic_star, "Condition", "condition", conditionList,
				isMultiSelection = true
			),
			InventoryFilterModel(R.drawable.ic_product_filter, "Sort By", "sort", sortList),
		)

		bind.filtersheet.recycler.adapter =
			InventoryFilterAdapter(filterList, object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {
					hideKeyboard(bind.root)
					val item = filterList[pos]
					when (status) {
						"open" -> {
							if (item.isOpened) {
								item.isOpened = false
								bind.filtersheet.recycler.adapter?.notifyItemChanged(pos)
							} else {
								filterList.forEachIndexed { index, model ->
									model.isOpened = pos == index
									bind.filtersheet.recycler.adapter?.notifyItemChanged(index)
								}
							}
						}

						else -> {
							if (status?.isNotEmpty() == true) {
								val statusInt = status.toInt()

								if (item.isMultiSelection) {
									item.list[statusInt].selected = !(item.list[statusInt].selected)

									when (item.slug) {
										"condition" -> {
											if (conditions.contains(item.list[statusInt].title)) {
												conditions.remove(item.list[statusInt].title)
											} else {
												conditions.add(item.list[statusInt].title)
											}
										}

										"category" -> {
											if (categoryIds.contains(item.list[statusInt].id)) {
												categoryIds.remove(item.list[statusInt].id)
											} else {
												categoryIds.add(item.list[statusInt].id)
											}
										}
									}
								} else {

									item.list.forEachIndexed { index, model ->
										model.selected = index == statusInt
										bind.filtersheet.recycler.adapter?.notifyItemChanged(index)
									}

									when (item.slug) {
										"sort" -> {
											sort = when (item.list[statusInt].title) {
												"Newest First" -> ""
												"Oldest First" -> ""
												"Price: Low to High" -> "asc"
												"Price: High to Low" -> "desc"
												else -> ""
											}
										}
									}
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

		bind.filtersheet.applyFilter.setHapticClickListener {
			minPrice = bind.filtersheet.min.value().ifEmpty { null }
			maxPrice = bind.filtersheet.max.value().ifEmpty { null }
			page = 1
			getInventory()
			filterSheet.state = BottomSheetBehavior.STATE_COLLAPSED
		}

		bind.filtersheet.recycler.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.filtersheet.clearFilter.setHapticClickListener {
			filterSheet.state = BottomSheetBehavior.STATE_COLLAPSED
			minPrice = null
			maxPrice = null
			sort = null
			categoryIds.clear()
			conditions.clear()

			bind.filtersheet.min.text?.clear()
			bind.filtersheet.max.text?.clear()

			filterList.forEach { filterModel ->
				filterModel.isOpened = false
				filterModel.list.forEach { it.selected = false }
			}
			bind.filtersheet.recycler.adapter?.notifyDataSetChanged()

			page = 1
			getInventory()
		}

	}

	fun getInventory(search: String? = null) {

		viewModel.getProducts(
			status = selectedTab.request(), page = page.toString().request(),
			search = search?.request(),
			categoryIds = if (categoryIds.isEmpty()) null else categoryIds.joinToString(",").request(),
			conditions = if (conditions.isEmpty()) null else conditions.joinToString(",").request(),
			minPrice = minPrice?.request(),
			maxPrice = maxPrice?.request(),
			format = sort?.request()
		)
	}
}