package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.R
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
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

		socketManager = SocketManager.getInstance(requireContext())

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
					loadData()
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

		productAdapter =
			FirebaseProductAdapter(
				from = "live_show",
				mList = productList,
				object : RecyclerClicks {
					@SuppressLint("NotifyDataSetChanged")
					override fun itemClick(pos: Int, status: String?) {

						val selectedProduct = productList[pos]

						if (productList[pos]?.status == "sold") {
							Alerts.error(mCtx, "This product is already sold")
						} else if (status == "start_auction") {

							auctionSettingsSheet(selectedProduct?.id.toString(), selectedProduct?.pricing ?: "")
						} else if (status == "set_next") {
							selectedPos = pos

							socketManager?.pinProduct(roomId = viewModel.currentRoomId, productId = selectedProduct?.id.toString())


//                            productList.forEachIndexed { index, item ->
//                                item?.selected = index == pos
//                                bind.recycler.adapter?.notifyDataSetChanged()
//                            }
//                            selectedPos = pos
						}

					}

				})

		bind.recycler.adapter = productAdapter

		bind.close.setHapticClickListener {
			dismiss()
		}

		bind.addBtn.setHapticClickListener {

			/*if (selectedPos == -1) {
				Alerts.error(mCtx, "Please select a product")
				return@setHapticClickListener
			}*/

//            val isAnyProductLive = productList.any { it?.isCurrent == true }
//
//            if (isAnyProductLive) {
//                Alerts.error(mCtx, "One Product is Already Live")
//                return@setHapticClickListener
//            }

//            val selectedProduct = productList[selectedPos]

//            socketManager?.setNextProduct(roomID, selectedProduct?.id)
//            productSheet.dismiss()

		}

	}

	private fun loadData() {
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
		auctionSettingsSheetBind.start.setHapticClickListener {

			when {
				selectedRequiredTime == 0 -> {
					Alerts.error(mCtx, "Please select required time")
					return@setHapticClickListener
				}

				selectedCounterTimer == 0 -> {
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
						auctionSettingsSheetBind.suddenDeath.isChecked
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