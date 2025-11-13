package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductAdapter
import io.bidswipe.app.databinding.FragmentAddProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct
import okhttp3.MultipartBody
import java.io.File

@SuppressLint("NotifyDataSetChanged")
class AddProductFragment : BaseFragment<ScheduleShowViewModel, FragmentAddProductBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentAddProductBinding.inflate(inflater, view, false)

	private lateinit var productAdapter: ProductAdapter

	//	private var productList = mutableListOf<GetMyInventoryResponse.Data?>()
	private var page = 1
	private var isLoading = false

	private val inventoryLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val data = result.data
				val selectedProducts =
					data?.getSerializableExtra("selectedProducts") as? ArrayList<GetMyInventoryResponse.Data>

				Log.d(TAG, "$selectedProducts ")
				selectedProducts?.forEach { data ->

					val product =
						LiveShowModel.Product(
							data.category?.name,
							data.id.toString(),
							data.images?.get(0),
							data.status,
							data.title,
							data.pricing.toString(),
							data.quantity.toString(),
							selected = true
						)

					if (!viewModel.currentProducts.any { existing -> existing.id == product.id }) {
						viewModel.currentProducts.add(product)
					}
				}

				productAdapter.notifyDataSetChanged()

				if (viewModel.currentProducts.isNotEmpty()) {
//					bind.noData.isVisible = false
					bind.recycler.isVisible = true
				}
			}
		}

	private var from = ""

	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			when (status) {
				"select" -> {
					viewModel.currentProducts[pos].selected = true
					productAdapter.notifyItemChanged(pos)
				}

				"edit" -> {
					startActivity(mCtx.toListProduct().putExtra("product", viewModel.currentProducts[pos]))
				}

				"delete" -> {

					viewModel.currentProducts.removeAt(pos)
					productAdapter.notifyItemRemoved(pos)

				}
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		from = activity?.intent?.getStringExtra("from") ?: ""

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		/*bind.contentScrollView.setOnScrollChangeListener { v: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
			val nestedScrollView = checkNotNull(v) {
				return@setOnScrollChangeListener
			}
			val lastChild = nestedScrollView.getChildAt(nestedScrollView.childCount - 1)
			if (lastChild != null) {
				if ((scrollY >= (lastChild.measuredHeight - nestedScrollView.measuredHeight)) && scrollY > oldScrollY) {
					if (!isLoading){
						isLoading = true
						page++
						viewModel.getUserProducts(userId.request(), categoryId = viewModel.categoryId.request(), page.toString().request())
					}
				}
			}
		}*/

		productAdapter = ProductAdapter(viewModel.currentProducts, mClick)
		bind.recycler.adapter = productAdapter

		bind.addProductLayout.setHapticClickListener {
			findNavController().navigate(ids.addProductFragment_to_createProductFragment)
		}

		bind.selectInventoryLayout.setHapticClickListener {
			inventoryLauncher.launch(
				Intent(mCtx, SellerHubActivity::class.java)
					.putExtra("slug", "inventory")
					.putExtra("from", "addProduct")
			)
		}

		bind.finishBtn.setHapticClickListener {

			val imagePartList = mutableListOf<MultipartBody.Part?>()
			val productIdList = mutableListOf<Int>()

			viewModel.currentProducts.forEach {
				if (it.selected == true) {
					productIdList.add(it.id?.toInt() ?: 0)
				}
			}

			if (productIdList.isEmpty()) {
				Alerts.error(mCtx, "Please select product")
				return@setHapticClickListener
			}

			imagePartList.add(
				Utils.imagePart(
					"thumbnail[]",
					viewModel.thumbnail,
					File(viewModel.thumbnail)
				)
			)

			bind.loader.isVisible = true

			if (from == "showTutorial") {

				val data = Intent()
				data.putExtra(
					"title",
					TutorialShowModel(
						viewModel.showTitle,
						viewModel.categoryId,
						viewModel.auctionId,
						viewModel.thumbnail,
						productIdList.joinToString(",")
					)
				)

				activity?.setResult(Activity.RESULT_OK, data)
				finish()

			} else {
				viewModel.storeScheduleShow(
					title = viewModel.showTitle.request(),
					date = viewModel.date.request(),
					time = viewModel.time.request(),
					categoryId = viewModel.categoryId.request(),
					auctionTypeId = viewModel.auctionId.request(),
					thumbnails = imagePartList,
					productIds = productIdList
				)
			}

		}


//		bind.loader.isVisible = true
//
//		viewModel.getUserProducts(userId.request(), categoryId = viewModel.categoryId.request(), page.toString().request())

		viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					it.value.data

					/*if (page == 1) productList.clear()

					mData?.forEach {
						productList.add(it)
					}

					log("DATA ${productList.size}")

					productAdapter.notifyDataSetChanged()

					isLoading = page >= (it.value.totalPage ?: 0)

					if (productList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}*/

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

		viewModel.storeScheduleShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val data = it.value.data

					log("SHOW DATA Before Start Shoe: $data")

					val products = data?.products?.map { it?.toLiveShowProduct() }

					products?.first()?.isCurrent = true

					val showData = LiveShowModel(
						seller = LiveShowModel.Seller(
							id = userId,
							image = userImage,
							name = userName,
							rating = ""
						),
						products = products?.map { p ->
							LiveShowModel.Product(
								data.category?.name,
								p?.id,
								p?.image,
								p?.status,
								p?.name,
								p?.price,
								"1",
							)
						}?.toList() ?: mutableListOf(),
						roomId = "live_room_${userId}_${data?.id.toString()}",
						showDetail = "Test Details",
						thumbnail = data?.thumbnail?.getOrNull(0) ?: "",
						viewerCount = "1",
						highestBid = LiveShowModel.HighestBid(
							bidAmount = "",
							userName = "",
							userImage = "",
							userId = "",
							productId = ""
						),
						isLive = true,
						time = Utils.timestamp().toString(),
						showId = data?.id.toString(),
						allowBidForAll = true,
						bidCountDown = "",
						showTimer = "",
					)

					val intent = Intent(mCtx, AgoraPublisherActivity::class.java).putExtra(
						"showData",
						showData
					).putExtra("time", data?.time)

					startActivity(intent)
					finish()
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

		viewModel.deleteProductRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					it.value.data

					/*viewModel.getUserProducts(
						userId.request(),
						categoryId = viewModel.categoryId.request()
					)*/

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
}