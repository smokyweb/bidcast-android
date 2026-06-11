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
import io.bidswipe.app.controller.SoldProductsAdapter
import io.bidswipe.app.controller.SortingOptionAdapter
import io.bidswipe.app.databinding.FragmentShopBinding
import io.bidswipe.app.databinding.SortingOptionSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveMoreOption
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.network.response.isLiveAuctionFormat
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class ShopFragment : BaseFragment<SellerViewModel, FragmentShopBinding>() {
	override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentShopBinding.inflate(inflater, view, false)

	private var productList = mutableListOf<Product?>()
	private var soldOrderList = mutableListOf<GetOrdersResponse.Data?>()
	private lateinit var shopAdapter: ShopAdapter
	private lateinit var soldProductsAdapter: SoldProductsAdapter
	private val optionList = mutableListOf<LiveMoreOption?>()
	private var sellerId = ""
	private var sortBy = "title_asc"
	private var selectedShopTab = ShopTab.AUCTION
	private var type = ""
	private var page = 1
	private var isLoading = false
	private var suppressSearchReload = false
	private var suppressChipReload = false
	private var useLegacyShopFilters = false

	private enum class ShopTab {
		AUCTION,
		BUY_NOW,
		SOLD
	}

	private data class ShopFilters(
		val saleType: String? = null,
		val status: String? = "active",
		val marketPlace: String? = null
	)

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

	private val soldOrderClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			if (status == "profile") return

			val productId = soldOrderList.getOrNull(pos)?.productId ?: return
			startActivity(
				Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
					"productId",
					productId.toString()
				)
			)
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sellerId = (activity as? SellerProfileActivity)?.sellerId
			?.takeIf { it.isNotBlank() }
			?: readSellerIdExtra()

		setUpChips()

		optionList.clear()

		optionList.add(LiveMoreOption("Title (A–Z)", isSelected = true))
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
				val lastDataPosition = if (selectedShopTab == ShopTab.SOLD) {
					soldOrderList.size - 1
				} else {
					productList.size - 1
				}
				if (lastDataPosition >= 0 && lastItemPosition == lastDataPosition) {
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
				if (suppressSearchReload) return

				bind.loader.isVisible = true
				page = 1
				loadData()

			}
		})

		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			loadData()
		}


		bind.filter.setHapticClickListener {
			sortOptionSheet()
		}

		shopAdapter = ShopAdapter(productList, mClick)
		soldProductsAdapter = SoldProductsAdapter(soldOrderList, soldOrderClick)
		bind.recycler.adapter = shopAdapter
		loadData()
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
						productList.addAll(mData.filter { product -> product.shouldShowInSelectedShopTab() })
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
					if (!retryWithLegacyShopFilters(it)) {
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
				}

				else -> {}

			}
		}

		viewModel.getSellerSoldOrdersRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.noInternet.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.noData.isVisible = false

					if (page == 1) {
						soldOrderList.clear()
					}

					it.value.data?.let { orders ->
						soldOrderList.addAll(orders)
					}

					if (soldOrderList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}

					isLoading = page >= (it.value.totalPage ?: 0)
					soldProductsAdapter.notifyDataSetChanged()
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

	/*override fun onResume() {
		super.onResume()

		bind.noInternet.isVisible = false
		bind.loader.isVisible = true
		page = 1
		viewModel.getUserProducts(userId = sellerId.request(), page = page.toString().request())

	}*/

	private fun loadData() {
		updateNoDataTitle()
		bind.recycler.adapter = if (selectedShopTab == ShopTab.SOLD) soldProductsAdapter else shopAdapter

		if (selectedShopTab == ShopTab.SOLD) {
			viewModel.getSellerSoldOrders(
				userId = sellerId.request(),
				page = page.toString().request(),
				search = bind.search.value().ifEmpty { null }?.request()
			)
			return
		}

		val filters = currentShopFilters()
		viewModel.getUserProducts(
			userId = sellerId.request(),
			saleType = filters.saleType?.request(),
			type = type.ifEmpty { null }?.request(),
			status = filters.status?.request(),
			marketPlace = filters.marketPlace?.request(),
			sortBy = sortBy.ifEmpty { null }?.request(),
			page = page.toString().request(),
			search = bind.search.value().ifEmpty { null }?.request()
		)
	}

	private fun setUpChips() {
		setSearchTextSilently("")
		bind.chipGroup.removeAllViews()

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

				if (suppressChipReload) return@runSafe

				type = ""
				page = 1
				useLegacyShopFilters = false
				isLoading = false

				when (index) {
					/*0 -> {
						sortOptionSheet()
					}*/

					0 -> {
						selectedShopTab = ShopTab.AUCTION
					}

					1 -> {
						selectedShopTab = ShopTab.BUY_NOW
					}

					2 -> {
						selectedShopTab = ShopTab.SOLD
					}

				}

				bind.loader.isVisible = true
				setSearchTextSilently("")
				productList.clear()
				soldOrderList.clear()
				loadData()

			}
		}

		suppressChipReload = true
		bind.chipGroup.check(bind.chipGroup.getChildAt(0).id)
		suppressChipReload = false


	}

	private fun setSearchTextSilently(text: String) {
		suppressSearchReload = true
		bind.search.setText(text)
		suppressSearchReload = false
	}

	private fun currentShopFilters(): ShopFilters {
		return when (selectedShopTab) {
			ShopTab.AUCTION -> ShopFilters(saleType = "auction")
			ShopTab.BUY_NOW -> {
				if (useLegacyShopFilters) {
					ShopFilters(marketPlace = "false")
				} else {
					ShopFilters(saleType = "buy_now")
				}
			}
			ShopTab.SOLD -> {
				if (useLegacyShopFilters) {
					ShopFilters(status = "inactive")
				} else {
					ShopFilters(saleType = "sold", status = null)
				}
			}
		}
	}

	private fun updateNoDataTitle() {
		bind.noData.title.text = if (selectedShopTab == ShopTab.SOLD) {
			"No Sold Orders Found"
		} else {
			"No Product Found"
		}
	}

	private fun readSellerIdExtra(): String {
		val rawSellerId = activity?.intent?.extras?.get("sellerId")
		return when (rawSellerId) {
			is String -> rawSellerId
			is Number -> rawSellerId.toString()
			else -> rawSellerId?.toString().orEmpty()
		}
	}

	private fun retryWithLegacyShopFilters(error: Resource.Error): Boolean {
		val canRetry = selectedShopTab != ShopTab.AUCTION && !useLegacyShopFilters
		val isSaleTypeValidation = error.errorResponse?.message
			?.contains("selected sale type", ignoreCase = true) == true

		if (!canRetry || !isSaleTypeValidation) return false

		useLegacyShopFilters = true
		page = 1
		bind.loader.isVisible = true
		bind.bottomLoader.isVisible = false
		loadData()
		return true
	}

	private fun Product?.matchesSelectedShopTab(): Boolean {
		val product = this ?: return false
		return when (selectedShopTab) {
			ShopTab.AUCTION -> !product.isSoldOutForShop() && product.isLiveAuctionFormat()
			ShopTab.BUY_NOW -> !product.isSoldOutForShop() && !product.isLiveAuctionFormat()
			ShopTab.SOLD -> product.hasSalesForShop()
		}
	}

	private fun Product?.shouldShowInSelectedShopTab(): Boolean {
		if (this == null) return false

		// The Sold tab already asks the API for sale_type=sold. Those results
		// are order-backed on the server, so do not drop processing sales just
		// because a product counter/status field came back stale or missing.
		if (selectedShopTab == ShopTab.SOLD && !useLegacyShopFilters) return true

		return matchesSelectedShopTab()
	}

	private fun Product.hasSalesForShop(): Boolean {
		if (isSoldOutForShop()) return true
		return purchasedQuantity.asIntOrZero() > 0
	}

	private fun Product.isSoldOutForShop(): Boolean {
		val statusText = status?.lowercase().orEmpty()
		if (statusText == "sold" || statusText == "inactive") return true

		val listedQuantity = quantity?.toIntOrNull()
		val purchased = purchasedQuantity?.toIntOrNull()
		return listedQuantity != null && purchased != null && listedQuantity <= purchased
	}

	private fun String?.asIntOrZero(): Int = this?.toIntOrNull() ?: 0

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

					setSearchTextSilently("")

					sortBy = ""

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
