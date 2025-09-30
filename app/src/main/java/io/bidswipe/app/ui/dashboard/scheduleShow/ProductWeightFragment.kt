package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.WeightAdapter
import io.bidswipe.app.databinding.FragmentProductWeightBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import okhttp3.MultipartBody
import java.io.File

class ProductWeightFragment : BaseFragment<ScheduleShowViewModel , FragmentProductWeightBinding>() {
	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentProductWeightBinding.inflate(inflater , view , false)

	private var mList = mutableListOf("" , "" , "" , "" , "" , "")
	private lateinit var adapter : WeightAdapter

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val productData = arguments

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		adapter = WeightAdapter(mList)
		bind.recycler.adapter = adapter

        bind.continueBtn.setHapticClickListener {
			bind.loader.isVisible = true
			val imagePaths = productData?.getString("imagePaths")
			val imageFiles = imagePaths?.split(",")?.map { File(it) } ?: emptyList()

			if (imageFiles.isEmpty()) {
				createProduct(productData , null)
                return@setHapticClickListener
			}

			uploadImages(imageFiles)
		}

		setupObservers()
	}

	private fun uploadImages(imageFiles : List<File>) {
		val imagePartList = mutableListOf<MultipartBody.Part>()
		val thumbnailPartList = mutableListOf<MultipartBody.Part>()

		imageFiles.forEach { file ->
			val name = System.currentTimeMillis().toString() + "_product_gallery.jpeg"
			val imagePart = Utils.imagePart("images[]" , name , file)
			imagePartList.add(imagePart)

			val thumbnailFile = File(file.absolutePath)
			val thumbnailName = System.currentTimeMillis().toString() + "_product_thumbnail.jpeg"
			val thumbnailPart = Utils.imagePart("thumbnail[]" , thumbnailName , thumbnailFile)
			thumbnailPartList.add(thumbnailPart)
		}

		viewModel.storeProductMeta(imagePartList , thumbnailPartList)
	}

	private fun createProduct(productData : Bundle? , imageUrls : List<Map<String , String>>?) {

		viewModel.storeProduct(
			categoryId = productData?.getString("categoryId") ?: "" ,
			title = productData?.getString("title") ?: "" ,
			description = productData?.getString("description") ?: "" ,
			quantity = (productData?.getInt("quantity") ?: 1).toString() ,
			pricing = productData?.getString("price") ?: "1" ,
			flashSale = "0" ,
			acceptOffers = "0" ,
			reserveForLive = "0" ,
			shippingProfileId = "4" ,
			status = "active" ,
			productImages = imageUrls ,
			subCategoryId = productData?.getString("subCategoryId")?.ifEmpty { null }?.toInt() ,
			variant = viewModel.variantData ,
			weight = productData?.getString("weight") ?: "" ,
			height = productData?.getString("height") ?: "" ,
			length = productData?.getString("length") ?: "" ,
			width = productData?.getString("width") ?: "" ,
			mailClass = productData?.getString("mailClass") ?: "" ,
			processingCategory = productData?.getString("processingCategory") ?: "" ,

			)

	}

	private fun setupObservers() {
		viewModel.storeProductMetaRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					val imageData = it.value.data?.mapNotNull { data ->
						if (data?.images != null && data.thumbnail != null) {
							mapOf(
								"image" to data.images , "thumbnail" to data.thumbnail
							)
						} else {
							null
						}
					}
					val productData = arguments
					createProduct(productData , imageData)
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}
						})
					}
				}

				else -> {}
			}
		}

		viewModel.storeProductRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					findNavController().navigate(ids.addProductFragment)
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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