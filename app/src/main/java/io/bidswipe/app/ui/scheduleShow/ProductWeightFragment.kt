package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.WeightAdapter
import io.bidswipe.app.databinding.FragmentProductWeightBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import okhttp3.MultipartBody
import java.io.File

class ProductWeightFragment : BaseFragment<ScheduleShowViewModel, FragmentProductWeightBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentProductWeightBinding.inflate(inflater, view, false)

	private var mList = mutableListOf("", "", "", "", "", "")
	private lateinit var adapter: WeightAdapter

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		adapter = WeightAdapter(mList)
		bind.recycler.adapter = adapter

		bind.continueBtn.setHapticClickListener {
			bind.loader.isVisible = true
			val imageFiles = viewModel.productImages.filterNotNull().map { File(it) }

			if (imageFiles.isEmpty()) {
				createProduct(null)
				return@setHapticClickListener
			}

			uploadImages(imageFiles)
		}

		setupObservers()
	}

	private fun uploadImages(imageFiles: List<File>) {
		val imagePartList = mutableListOf<MultipartBody.Part>()
		val thumbnailPartList = mutableListOf<MultipartBody.Part>()

		imageFiles.forEach { file ->
			val name = System.currentTimeMillis().toString() + "_product_gallery.jpeg"
			val imagePart = Utils.imagePart("images[]", name, file)
			imagePartList.add(imagePart)

			val thumbnailFile = File(file.absolutePath)
			val thumbnailName = System.currentTimeMillis().toString() + "_product_thumbnail.jpeg"
			val thumbnailPart = Utils.imagePart("thumbnail[]", thumbnailName, thumbnailFile)
			thumbnailPartList.add(thumbnailPart)
		}

		viewModel.storeProductMeta(imagePartList, thumbnailPartList)
	}

	private fun createProduct(imageUrls: List<Map<String, String>>?) {

		viewModel.storeProduct(
			categoryId = viewModel.productCategoryId,
			title = viewModel.productTitle,
			description = viewModel.productDescription,
			quantity = viewModel.productQuantity.toString(),
			pricing = viewModel.productPrice.ifEmpty { "1" },
			flashSale = "0",
			acceptOffers = "0",
			reserveForLive = "0",
			auction = viewModel.productSalesFormat == "auction",
			shippingProfileId = "4",
			type = "live",
			status = "active",
			productImages = imageUrls,
			subCategoryId = viewModel.productSubCategoryId.ifEmpty { null }?.toInt(),
			variant = viewModel.variantData,
			weight = viewModel.productWeight,
			height = viewModel.productHeight,
			length = viewModel.productLength,
			width = viewModel.productWidth,
			mailClass = viewModel.productMailClass?.label ?: "",
			processingCategory = viewModel.productProcessingCategory ?: "",
			productCondition = viewModel.condition ,
		)

	}

	private fun setupObservers() {
		viewModel.storeProductMetaRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					viewModel.storeProductMetaRepo.value=null
					val imageData = it.value.data?.mapNotNull { data ->
						if (data?.images != null && data.thumbnail != null) {
							mapOf(
								"image" to data.images, "thumbnail" to data.thumbnail
							)
						} else {
							null
						}
					}
					createProduct(imageData)
				}

				is Resource.Error -> {
					viewModel.storeProductMetaRepo.value=null
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

		viewModel.storeProductRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					viewModel.storeProductRepo.value=null
					bind.loader.isVisible = false

					val data = it.value.data

					val product =
						LiveShowModel.Product(
							data?.category?.name,
							data?.id.toString(),
							data?.images?.get(0),
							data?.status,
							data?.title,
							data?.pricing,
							data?.quantity
						)

					viewModel.currentProducts.add(product)

					findNavController().navigate(ids.addProductFragment)
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.storeProductRepo.value=null
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