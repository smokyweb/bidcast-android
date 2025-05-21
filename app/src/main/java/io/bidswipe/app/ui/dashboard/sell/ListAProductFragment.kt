package io.bidswipe.app.ui.dashboard.sell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.databinding.FragmentListAProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File

class ListAProductFragment : BaseFragment<DashViewModel, FragmentListAProductBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentListAProductBinding.inflate(inflater, view, false)
	
	var imageList = mutableListOf<String?>()
	var uploadItemIndex = -1
	
	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent
			val imagePath = result.getUriFilePath(mCtx, true)
			if (imagePath != null) {
				if (uploadItemIndex == -1) {
					imageList.add(imagePath)
				} else {
					imageList[uploadItemIndex] = imagePath
					uploadItemIndex = -1
				}
				bind.imageLimit.text = "${imageList.size}/9"
				bind.images.adapter?.notifyDataSetChanged()
			}
		}
	}
	
	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var categoryId = ""
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		imageList.clear()
		imageList.add(null)
		
		bind.header.onBackClick {
			finish()
		}
		
		bind.images.adapter = ImageAdapter(imageList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				uploadItemIndex = pos
				uploadImage()
			}
		})
		
		bind.addNewImage.setOnClickListener {
			uploadItemIndex = -1
			uploadImage()
		}
		
		bind.publish.setOnClickListener {
			saveProduct()
		}
		
		bind.saveDraft.setOnClickListener {
			saveProduct("draft")
		}
		
		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					if (it.value.data?.isNotEmpty() == true) {
						categoryList.clear()
						categoryList.addAll(it.value.data)
						
						val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, categoryList.map { it?.name })
						bind.category.setAdapter(adapter)
						val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
						bind.category.setDropDownBackgroundDrawable(draw)
						
						bind.category.setOnItemClickListener { _, _, position, _ ->
							categoryId = categoryList[position]?.id.toString()
						}
						bind.category.setOnClickListener {
							bind.category.showDropDown()
						}
					}
				}
				
				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
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
		
		viewModel.storeProductRepo.observe(viewLifecycleOwner) {
			bind.loader.isVisible = false
			
			when (it) {
				is Resource.Success -> {
					Alerts.showBottomSheet(mCtx, it.value.message ?: "Product added successfully", "Success", false)
				}
				
				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
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
	
	fun uploadImage() {
		if (imageList.size < 9) {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
				}
			}
		} else {
			Alerts.error(mCtx, "You can select max 9 images only")
		}
	}
	
	fun saveProduct(type: String = "active") {
		when {
			imageList.filterNotNull().isEmpty() -> {
				Alerts.error(mCtx, "Please select at least one image")
			}
			
			categoryId.isEmpty() -> {
				Alerts.error(mCtx, "Please select category")
			}
			
			bind.productTitle.value().isEmpty() -> {
				bind.productTitle.requestFocus()
				Alerts.error(mCtx, "Please enter product title")
			}
			
			bind.description.value().isEmpty() -> {
				bind.description.requestFocus()
				Alerts.error(mCtx, "Please enter description")
			}
			
			bind.quantity.value().isEmpty() -> {
				bind.quantity.requestFocus()
				Alerts.error(mCtx, "Please enter quantity")
			}
			
			bind.price.value().isEmpty() -> {
				bind.price.requestFocus()
				Alerts.error(mCtx, "Please enter price")
			}
			
			/*	bind.shippingProfile.value().isEmpty() -> {
					Alerts.error(mCtx, "Please select shipping")
				}
				*/
			else -> {
				bind.loader.isVisible = true
				val imagePartList = mutableListOf<MultipartBody.Part>()
				imageList.forEach { image ->
					if (image != null) {
						val name =
							System.currentTimeMillis().toString() + "_product_gallery.jpeg"
						val imagePart = Utils.imagePart("images[]", name, File(image ?: ""))
						imagePart.let { element -> imagePartList.add(element) }
					}
				}
				
				viewModel.storeProduct(
					categoryId = categoryId.request(),
					title = bind.productTitle.value().request(),
					description = bind.description.value().request(),
					quantity = bind.quantity.value().request(),
					pricing = bind.price.value().request(),
					flashSale = (if (bind.flashSell.isChecked == true) "1" else "0").toString().request(),
					acceptOffers = (if (bind.acceptOffers.isChecked == true) "1" else "0").toString().request(),
					reserveForLive = (if (bind.reserveForLive.isChecked == true) "1" else "0").toString().request(),
					shippingProfileId = "1".request(),
					status = type.request(),
					productImages = imagePartList
				)
			}
		}
	}
	
}