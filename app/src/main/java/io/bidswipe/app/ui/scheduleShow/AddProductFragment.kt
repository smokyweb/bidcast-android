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
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductAdapter
import io.bidswipe.app.databinding.FragmentAddProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.toRandomizerTemplates
import okhttp3.MultipartBody
import java.io.File

@SuppressLint("NotifyDataSetChanged")
class AddProductFragment : BaseFragment<ScheduleShowViewModel, FragmentAddProductBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentAddProductBinding.inflate(inflater, view, false)

	private lateinit var productAdapter: ProductAdapter

	private var from = ""

	private val inventoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val data = result.data
			val selectedProducts = data?.getSerializableExtra("selectedProducts") as? ArrayList<Product>

			Log.d(TAG, "$selectedProducts ")
			selectedProducts?.forEach { data ->
				if (!viewModel.currentProducts.any { existing -> existing.id == data.id }) {
					viewModel.currentProducts.add(data)
				}
			}

			bind.productCount.text = buildSpannedString {
				append(viewModel.currentProducts.size.toString())
				append("/100")
			}

			productAdapter.notifyDataSetChanged()

			if (viewModel.currentProducts.isNotEmpty()) {
				bind.noData.isVisible = false
				bind.recycler.isVisible = true
			} else {
				bind.noData.isVisible = true
				bind.recycler.isVisible = false
			}
		}
	}

	private val randomizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val data = result.data
			val templateId = data?.getIntExtra("selectedTemplateId", 0)?.takeIf { it > 0 }
			if (templateId != null) {
				viewModel.selectedRandomizerTemplateId = templateId
				viewModel.selectedRandomizerTemplateName = data.getStringExtra("selectedTemplateName")
				updateRandomizerDisplay()
			}
		}
	}


	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			when (status) {

				"edit" -> {
					startActivity(mCtx.toListProduct().putExtra("product", viewModel.currentProducts[pos]))
				}

				"delete" -> {

					log("position : $pos , ${viewModel.currentProducts.size}")

					viewModel.currentProducts.removeAt(pos)

					bind.productCount.text = buildSpannedString {
						append(viewModel.currentProducts.size.toString())
						append("/100")
					}

					productAdapter.notifyItemRemoved(pos)
					productAdapter.notifyItemRangeChanged(pos, viewModel.currentProducts.size)

					if (viewModel.currentProducts.isNotEmpty()) {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					} else {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					}

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

		bind.productCount.text = buildSpannedString {
			append(viewModel.currentProducts.size.toString())
			append("/100")
		}

		if (viewModel.currentProducts.isNotEmpty()) {
			bind.noData.isVisible = false
			bind.recycler.isVisible = true
		} else {
			bind.noData.isVisible = true
			bind.recycler.isVisible = false
		}

		bind.selectInventoryLayout.setHapticClickListener {
			inventoryLauncher.launch(
				Intent(mCtx, SellerHubActivity::class.java)
					.putExtra("slug", "inventory")
					.putExtra("categoryId", viewModel.categoryId)
					.putExtra("auction_type", viewModel.auctionId)
					.putExtra("from", "addProduct")
			)
		}

		updateRandomizerDisplay()
		bind.addRandomizerLayout.setHapticClickListener {
			randomizerLauncher.launch(
				mCtx.toRandomizerTemplates()
					.putExtra("from", "show_creation_picker")
			)
		}

		bind.finishBtn.setHapticClickListener {

			val imagePartList = mutableListOf<MultipartBody.Part?>()
			val productIdList = mutableListOf<Int>()

			viewModel.currentProducts.forEach {
				productIdList.add(it.id ?: 0)
			}

			if (productIdList.isEmpty()) {
				Alerts.error(mCtx, "Please select product")
				return@setHapticClickListener
			}

			if (!viewModel.thumbnail.contains(Const.BASE_URL)) {
				imagePartList.add(
					Utils.imagePart(
						"thumbnail[]",
						viewModel.thumbnail,
						File(viewModel.thumbnail)
					)
				)
			}

			// Basecamp #9991372302: build stream quantities in lockstep with product ids.
			// streamQuantity defaults to 0 (unset) until the adapter initialises it to
			// full stock; clamp to [1, stock] defensively on submit.
			val streamQtyList = mutableListOf<Int>()
			viewModel.currentProducts.forEach { product ->
				val stock = product.quantity?.toIntOrNull() ?: 1
				var qty = if (product.streamQuantity < 1) stock else product.streamQuantity
				if (qty < 1) qty = 1
				if (stock > 0 && qty > stock) qty = stock
				streamQtyList.add(qty)
			}

			bind.loader.isVisible = true

			if (from == "showTutorial") {

				val data = Intent()
				data.putExtra(
					"title",
					TutorialShowModel(
                        showTitle = viewModel.showTitle,
                        categoryId = viewModel.categoryId,
                        subCategoryId = viewModel.subCategoryId,
                        actionId = viewModel.auctionId,
                        thumbnail = viewModel.thumbnail,
                        productIds = productIdList.joinToString(","),
                        repeatMode = viewModel.repeatMode,
                        repeatType = viewModel.repeatType,
                        explicitContent = viewModel.explicitContent,
                        primaryLanguage = viewModel.primaryLanguage,
                        discoverability = viewModel.discoverability
					)
				)

				activity?.setResult(Activity.RESULT_OK, data)
				finish()

			} else {
				// Browse-filter bundle (Basecamp #9928367737): parse comma-separated tag string,
				// trim, drop empties — mirrors PWA + iOS behavior. Backend reads as `tags[]` array.
				val parsedTags = viewModel.tagsRaw
					.split(",")
					.map { it.trim() }
					.filter { it.isNotEmpty() }
				val tagsRequest = if (parsedTags.isEmpty()) null else parsedTags.map { it.request() }

				viewModel.storeScheduleShow(
					title = viewModel.showTitle.request(),
					date = viewModel.date.request(),
					time = viewModel.time.request(),
					categoryId = viewModel.categoryId.request(),
					subCategoryId = viewModel.subCategoryId.request(),
					showDiscoverability = viewModel.discoverability.request(),
					auctionTypeId = viewModel.auctionId.request(),
					thumbnails = imagePartList,
					productIds = productIdList,
					isRepeat = viewModel.repeatMode.request(),
					repeatValue = viewModel.repeatType.request(),
					language = viewModel.primaryLanguage.request(),
					isExplicit = viewModel.explicitContent.request(),
					showId = viewModel.showId?.ifEmpty { null }?.request(),
					tags = tagsRequest,
					// Basecamp #9933883175 (2026-05-27): seller-controlled verified-only gate.
					// Only forward when the seller explicitly toggled it on — otherwise
					// send null so backend default (false) applies.
					isVerifiedOnly = if (viewModel.verifiedOnly == "1") "1".request() else null,
					// Basecamp #9991372302: per-product stream quantities, aligned with productIds.
					productStreamQuantities = streamQtyList,
				)
			}

		}

		viewModel.storeScheduleShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val data = it.value.data
					log("SHOW DATA Before Start Show: $data")
					// Attach randomizer template if one was selected in the show-create flow
					val tplId = viewModel.selectedRandomizerTemplateId
					val newShowId = data?.id?.toString()
					if (tplId != null && !newShowId.isNullOrEmpty()) {
						viewLifecycleOwner.lifecycleScope.launch {
							try {
								io.bidswipe.app.network.RetrofitService(mCtx).build()
									.attachRandomizerTemplate(newShowId, io.bidswipe.app.network.request.AttachTemplateRequest(tplId))
							} catch (e: Exception) {
								Log.w(TAG, "attach randomizer template failed: " + e.message)
							}
							activity?.setResult(Activity.RESULT_OK)
							finish()
						}
					} else {
						activity?.setResult(Activity.RESULT_OK)
						finish()
					}
				}

				is Resource.Error -> {
					viewModel.storeScheduleShowRepo.value = null
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

	private fun updateRandomizerDisplay() {
		val name = viewModel.selectedRandomizerTemplateName
		bind.randomizerSelectionText.text = if (name.isNullOrBlank()) {
			"Add Randomizer"
		} else {
			"Randomizer: $name"
		}
	}
}
