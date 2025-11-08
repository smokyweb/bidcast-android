package io.bidswipe.app.ui.sell

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryListAdapter
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.controller.ProductVariantAdapter
import io.bidswipe.app.databinding.CategoryBottomSheetBinding
import io.bidswipe.app.databinding.FragmentListAProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const

import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File

class ListAProductFragment : BaseFragment<DashViewModel, FragmentListAProductBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentListAProductBinding.inflate(inflater, view, false)
	
	var imageList = mutableListOf<String?>()
	var uploadItemIndex = -1
	var isSubCategory = false
	private var product: GetMyInventoryResponse.Data? = null
	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()
	
	private var categoryId = ""
	private var subCategoryId = ""
	var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
	private lateinit var variantAdapter: ProductVariantAdapter
	
	private var packageWidth = 0.0
	private var packageHeight = 0.0
	private var packageLength = 0.0
	private var packageWeight = 0.0
	private var selectedMailClass: GetMailClassesResponse.Data.MailClasses? = null
	
	private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
		if (result.isSuccessful) {
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
	
/*	private val cropImageLauncher = registerForActivityResult(CustomCropImageContract()) { uri ->
		if (uri != null) {
			val imagePath = CustomCropImageContract.getUriFilePath(mCtx, uri)
			if (imagePath != null) {
				if (uploadItemIndex == -1) {
					imageList.add(imagePath)
					log("IMAGES $imageList")
					bind.images.adapter?.notifyItemInserted(imageList.lastIndex)
				} else {
					imageList[uploadItemIndex] = imagePath
					bind.images.adapter?.notifyItemChanged(uploadItemIndex)
					uploadItemIndex = -1
				}
				bind.imageLimit.text = "${imageList.size}/9"
				
				// Save state to ViewModel
				saveStateToViewModel()
			}
		}
	}
	
	// Manager handles image picking (gallery/camera) internally
	private val imagePickerManager = CustomCropImageHelper.createManager(this, cropImageLauncher)
	*/
	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}
	}
	
	private fun restoreStateFromViewModel() {
		// Restore from ViewModel if state exists
		if (viewModel.productFormImageList.isNotEmpty() || viewModel.productFormCategoryId.isNotEmpty()) {
			imageList.clear()
			imageList.addAll(viewModel.productFormImageList)
			categoryId = viewModel.productFormCategoryId
			subCategoryId = viewModel.productFormSubCategoryId
			variantList.clear()
			variantList.addAll(viewModel.productFormVariantList)
			packageWidth = viewModel.productFormPackageWidth
			packageHeight = viewModel.productFormPackageHeight
			packageLength = viewModel.productFormPackageLength
			packageWeight = viewModel.productFormPackageWeight
			selectedMailClass = viewModel.productFormSelectedMailClass
			product = viewModel.productFormProduct
			isSubCategory = viewModel.productFormIsSubCategory
			
			// Restore form fields
			bind.productTitle.setText(viewModel.productFormProductTitle)
			bind.description.setText(viewModel.productFormDescription)
			bind.quantity.setText(viewModel.productFormQuantity)
			bind.width.setText(viewModel.productFormWidth)
			bind.height.setText(viewModel.productFormHeight)
			bind.length.setText(viewModel.productFormLength)
			bind.weight.setText(viewModel.productFormWeight)
			bind.mailclass.setText(viewModel.productFormMailClassText, false)
			bind.proCategory.setText(viewModel.productFormProcessingCategory, false)
			bind.price.setText(viewModel.productFormPrice)
			bind.flashSell.isChecked = viewModel.productFormFlashSale
			bind.acceptOffers.isChecked = viewModel.productFormAcceptOffers
			bind.reserveForLive.isChecked = viewModel.productFormReserveForLive
			
			if (viewModel.productFormCategoryText.isNotEmpty()) {
				if (subCategoryId.isNotEmpty()) {
					bind.category.setText(buildSpannedString {
						append(viewModel.productFormCategoryText)
						append("(${viewModel.productFormProduct})")
					})
				} else {
					bind.category.setText(viewModel.productFormCategoryText,false)
				}
			}
			
			bind.imageLimit.text = "${imageList.size}/9"
		}
	}
	
	private fun saveStateToViewModel() {
		// Save current state to ViewModel
		viewModel.productFormImageList.clear()
		viewModel.productFormImageList.addAll(imageList)
		viewModel.productFormCategoryId = categoryId
		viewModel.productFormSubCategoryId = subCategoryId
		viewModel.productFormVariantList.clear()
		viewModel.productFormVariantList.addAll(variantList)
		viewModel.productFormPackageWidth = packageWidth
		viewModel.productFormPackageHeight = packageHeight
		viewModel.productFormPackageLength = packageLength
		viewModel.productFormPackageWeight = packageWeight
		viewModel.productFormSelectedMailClass = selectedMailClass
		viewModel.productFormProduct = product
		viewModel.productFormIsSubCategory = isSubCategory
		
		// Save form field values
		viewModel.productFormProductTitle = bind.productTitle.value()
		viewModel.productFormDescription = bind.description.value()
		viewModel.productFormQuantity = bind.quantity.value()
		viewModel.productFormWidth = bind.width.value()
		viewModel.productFormHeight = bind.height.value()
		viewModel.productFormLength = bind.length.value()
		viewModel.productFormWeight = bind.weight.value()
		viewModel.productFormMailClassText = bind.mailclass.value()
		viewModel.productFormProcessingCategory = bind.proCategory.value()
		viewModel.productFormPrice = bind.price.value()
		viewModel.productFormFlashSale = bind.flashSell.isChecked
		viewModel.productFormAcceptOffers = bind.acceptOffers.isChecked
		viewModel.productFormReserveForLive = bind.reserveForLive.isChecked
		viewModel.productFormCategoryText = bind.category.text?.toString() ?: ""
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		// Restore state from ViewModel if available
		restoreStateFromViewModel()
		
		// Get product from intent only if not already restored
		if (product == null) {
			product = activity?.intent?.getSerializableExtra("product") as? GetMyInventoryResponse.Data
		}
		
		bind.root.setOnClickListener {
			hideKeyboard(it)
		}
		
		bind.mainLayout.setOnClickListener {
			hideKeyboard(it)
		}
		
		// Only load product data if not restored from ViewModel
		if (product != null && viewModel.productFormProduct == null) {
			bind.saveDraft.isVisible = false
			bind.publish.text = "Update"
			bind.header.setHeaderText("Update Product")
			addProductData(product)
		} else if (product != null && viewModel.productFormProduct != null) {
			// Restore UI state for edit mode
			bind.saveDraft.isVisible = false
			bind.publish.text = "Update"
			bind.header.setHeaderText("Update Product")
		}
		
		log(product.toString())
		
		variantAdapter = ProductVariantAdapter(variantList, mClick)
		
		bind.variants.adapter = variantAdapter
		
		// Notify adapter if we restored variants
		if (viewModel.productFormVariantList.isNotEmpty()) {
			variantAdapter.notifyDataSetChanged()
		}
		
		bind.header.onBackClick {
			// Clear ViewModel state when leaving
			clearViewModelState()
			finish()
		}
		
		val processingCategories = listOf("LETTERS", "FLATS", "MACHINABLE", "NONSTANDARD", "NON_MACHINABLE")
		val proCategoryAdapter = ArrayAdapter(
			mCtx,
			android.R.layout.simple_list_item_1,
			processingCategories
		)
		bind.proCategory.setAdapter(proCategoryAdapter)
		val proDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
		bind.proCategory.setDropDownBackgroundDrawable(proDrawable)
		var selectedProcessingCategory: String? = null
		
		bind.proCategory.setOnItemClickListener { _, _, position, _ ->
			selectedProcessingCategory = processingCategories[position]
			log("Selected processing category: $selectedProcessingCategory")
		}

		bind.proCategory.setHapticClickListener {
			bind.proCategory.showDropDown()
		}

		bind.images.adapter = ImageAdapter(imageList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				uploadItemIndex = pos
				uploadImage()
			}
		})

		bind.images.layoutManager = LinearLayoutManager(mCtx, LinearLayoutManager.HORIZONTAL, false)
		bind.images.setHasFixedSize(false)
		
		bind.addNewImage.setHapticClickListener {
			uploadItemIndex = -1
			uploadImage()
		}
		
		bind.publish.setHapticClickListener {
			saveProduct()
		}
		
		bind.saveDraft.setHapticClickListener {
			saveProduct("draft")
		}
		
		bind.category.setHapticClickListener {
			showCategorySheet(categoryList, "category")
		}
		
		viewModel.getCategory()
		
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.getCategoryRepo.value = null
					bind.loader.isVisible = false
					
					val mData = it.value.data

					if (mData?.isNotEmpty() == true) {
						categoryList.clear()
						categoryList.addAll(mData)
					}

				}
				
				is Resource.Error -> {
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

		viewModel.getProductSubCategoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.getProductSubCategoryRepo.value = null
					bind.loader.isVisible = false

					val mData = it.value.data

					if (mData?.isNotEmpty() == true) {
						subCategoryList.clear()
						subCategoryList.addAll(mData)
						showCategorySheet(subCategoryList, "subCategory")
					}

				}

				is Resource.Error -> {
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

		viewModel.storeProductRepo.observe(viewLifecycleOwner) {
			bind.loader.isVisible = false
			
			when (it) {
				is Resource.Success -> {
					// Clear ViewModel state on successful save
					clearViewModelState()
					Alerts.showBottomSheet(
						mCtx,
						it.value.message ?: "Product added successfully",
						"Success",
						false,
						object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								finish()
								dialog.dismiss()
							}
							
							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						})
				}
				
				is Resource.Error -> {
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
		
		viewModel.getMailClasses()
		viewModel.getMailClassesRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					mailClassesList.clear()
					
					val mData = it.value.data
					if (mData?.mailClasses?.isNotEmpty() == true) {
						mailClassesList.addAll(mData.mailClasses)
						setupMailClassDropdown()
						
						// Restore selected mail class if available
						if (selectedMailClass != null) {
							val sel = mailClassesList.findLast { mailClass ->
								selectedMailClass?.label?.lowercase() == mailClass?.label?.lowercase()
							}
							if (sel != null) {
								selectedMailClass = sel
								bind.mailclass.setText(sel.label,false)
							}
						} else {
							// Original logic for product edit mode
							product?.let { pr->
								val sel = mailClassesList.findLast { mailClass ->
									log("CLASS ${pr.mailClass} ${mailClass?.label} ${pr.mailClass?.lowercase() == mailClass?.label?.lowercase()}")
									pr.mailClass?.lowercase() == mailClass?.label?.lowercase()
								}
								
								if (sel != null) {
									selectedMailClass = sel
									bind.mailclass.setText(sel.label,false)
								}
								log("Selected mail class: $selectedMailClass")
							}
						}
						
					}
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
	
	private fun setupMailClassDropdown() {
		val mailClassNames = mailClassesList.map { it?.label ?: "" }.toTypedArray()
		
		val adapter = ArrayAdapter(
			mCtx,
			android.R.layout.simple_list_item_1,
			mailClassNames
		)
		
		bind.mailclass.setAdapter(adapter)
		
		val drawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
		bind.mailclass.setDropDownBackgroundDrawable(drawable)
		
		bind.mailclass.setOnItemClickListener { _, _, position, _ ->
			selectedMailClass = mailClassesList[position]
			log("Selected mail class: ${selectedMailClass?.label}")
			// Save state to ViewModel
			saveStateToViewModel()
		}
		
		bind.mailclass.setHapticClickListener {
			if (mailClassesList.isNotEmpty()) {
				bind.mailclass.showDropDown()
			} else {
				viewModel.getMailClasses()
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
		hideKeyboard(bind.root)
		try {
			packageWidth = bind.width.value().toDoubleOrNull() ?: 0.0
			packageHeight = bind.height.value().toDoubleOrNull() ?: 0.0
			packageLength = bind.length.value().toDoubleOrNull() ?: 0.0
			packageWeight = bind.weight.value().toDoubleOrNull() ?: 0.0
		} catch (_: NumberFormatException) {
			Alerts.error(mCtx, "Please enter valid numeric values for dimensions")
			return
		}
		
		val variantData = getVariantData()
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
			
			
			packageWidth <= 0 || packageHeight <= 0 || packageLength <= 0 || packageWeight <= 0 -> {
				Alerts.error(mCtx, "Please enter all package dimensions")
			}
			
			selectedMailClass?.maxWidthIn != null && (packageWidth > (selectedMailClass?.maxWidthIn
				?: 0.0)) -> {
				Alerts.error(
					mCtx,
					"Width exceeds maximum of ${selectedMailClass?.maxWidthIn} cm"
				)
			}
			
			selectedMailClass?.maxHeightIn != null && (packageHeight > (selectedMailClass?.maxHeightIn
				?: 0.0)) -> {
				Alerts.error(
					mCtx,
					"Height exceeds maximum of ${selectedMailClass?.maxHeightIn} cm"
				)
			}
			
			selectedMailClass?.maxLengthIn != null && (packageLength > (selectedMailClass?.maxLengthIn
				?: 0.0)) -> {
				Alerts.error(
					mCtx,
					"Length exceeds maximum of ${selectedMailClass?.maxLengthIn} cm"
				)
			}
			
			selectedMailClass?.maxWeightLbs != null && (packageWeight > (selectedMailClass?.maxWeightLbs
				?: 0.0)) -> {
				Alerts.error(
					mCtx,
					"Weight exceeds maximum of ${selectedMailClass?.maxWeightLbs} lbs"
				)
			}
			
			selectedMailClass == null -> {
				Alerts.error(mCtx, "Please select a mail class")
			}
			
			bind.proCategory.value().isEmpty() -> {
				bind.proCategory.requestFocus()
				Alerts.error(mCtx, "Please enter processing category")
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
				// Save final state before submission
				saveStateToViewModel()
				
				val imagePartList = mutableListOf<MultipartBody.Part>()
				val thumbnailPartList = mutableListOf<MultipartBody.Part>()
				imageList.filter { it?.contains(Const.BASE_URL) == false }.forEach { image ->
					if (image != null) {
						val name = System.currentTimeMillis().toString() + "_product_gallery.jpeg"
						val thumbnailName =
							System.currentTimeMillis().toString() + "_product_thumbnail.jpeg"
						
						val imagePart = Utils.imagePart("images[]", name, File(image))
						imagePart.let { element -> imagePartList.add(element) }
						
						val thumbnailFile = File(image)
						val thumbnailPart =
							Utils.imagePart("thumbnails[]", thumbnailName, thumbnailFile)
						thumbnailPart.let { element -> thumbnailPartList.add(element) }
					}
				}
				val productId = if (product != null) product?.id.toString() else null
				if (imagePartList.isNotEmpty()) {
					viewModel.storeProductMeta(imagePartList, thumbnailPartList)

					viewModel.storeProductMetaRepo.observe(viewLifecycleOwner) {
						when (it) {
							is Resource.Success -> {
								createProduct(
									productId,
									type,
									it.value.data?.map {
										mapOf(
											"image" to it?.images,
											"thumbnail" to it?.thumbnail
										)
									},
									variantData
								)
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
					
				} else {
					createProduct(productId.toString(), type, emptyList(), variantData)
				}
				
			}
		}
	}
	
	private fun addProductData(product: GetMyInventoryResponse.Data?) {
		categoryId = if(product?.categoryId!=null) product.categoryId.toString() else ""
		subCategoryId = product?.subCategoryId?:""
		if (product?.subCategory != null) {
			bind.category.setText(buildSpannedString {
				append(product.category?.name)
				append("(${product.subCategory.name})")
			})
		} else {
			bind.category.setText(product?.category?.name)
		}
		bind.productTitle.setText(product?.title?:"")
		bind.description.setText(product?.description?:"")
		bind.quantity.setText((product?.quantity?:"").toString())
		bind.width.setText((product?.width?:"").toString())
		bind.height.setText((product?.height?:"").toString())
		bind.length.setText((product?.length?:"").toString())
		bind.weight.setText((product?.weight?:"").toString())
		bind.mailclass.setText(product?.mailClass?:"",false)
		bind.proCategory.setText(product?.processingCategory?:"", false)
		bind.price.setText((product?.pricing?:"").toString())
		bind.flashSell.isChecked = product?.flashSale == true
		bind.acceptOffers.isChecked = product?.acceptOffers == true
		bind.reserveForLive.isChecked = product?.reserveForLive == true
		imageList.clear()
		product?.images?.forEachIndexed { index, imageUrl ->
			imageUrl?.let {
				if (imageList.size < 9) {
					imageList.add(it)
				}
			}
		}
		
		bind.imageLimit.text = "${imageList.size}/9"
		bind.images.adapter?.notifyDataSetChanged()
	}
	
	@SuppressLint("NotifyDataSetChanged")
	private fun showCategorySheet(
		categoryList: MutableList<GetCategoryResponse.Data?>,
		type: String,
	) {
		val categorySheetBind =
			CategoryBottomSheetBinding.bind(
				layoutInflater.inflate(
					R.layout.category_bottom_sheet,
					null,
					false
				)
			)
		val categorySheet = Alerts.appBottomSheet(mCtx, true, categorySheetBind)
		variantList.clear()
		variantAdapter.notifyDataSetChanged()
		categorySheetBind.recycler.adapter = CategoryListAdapter(
			if (type == "category") categoryList else subCategoryList,
			object : RecyclerClicks {
				
				override fun itemClick(pos: Int, status: String?) {
					if (type == "category") {
						categoryId = categoryList[pos]?.id.toString()
						bind.category.setText(categoryList[pos]?.name.toString())
						bind.loader.isVisible = true
						subCategoryId = ""
						viewModel.getProductSubCategory(categoryId, "subCategory")
						isSubCategory = true
						if (categoryList[pos]?.extraFields?.isNotEmpty() == true) {
							variantList.addAll(categoryList[pos]?.extraFields ?: mutableListOf())
							variantAdapter.notifyDataSetChanged()
						}
					} else {
						bind.category.setText(buildSpannedString {
							append(bind.category.text)
							append("(${subCategoryList[pos]?.name.toString()})")
						})
						subCategoryId = subCategoryList[pos]?.id.toString()
						isSubCategory = false
						if (subCategoryList[pos]?.extraFields?.isNotEmpty() == true) {
							variantList.addAll(subCategoryList[pos]?.extraFields ?: mutableListOf())
							variantAdapter.notifyDataSetChanged()
						}
					}
					// Save state to ViewModel
					saveStateToViewModel()
					categorySheet.dismiss()
				}
			})
		
		if (type == "subCategory") {
			categorySheetBind.sheetTitle.text = "Select Product Sub Category"
		} else {
			categorySheetBind.sheetTitle.text = "Select Product Category"
		}
		
		categorySheetBind.close.setHapticClickListener {
			categorySheet.dismiss()
		}
		
		categorySheet.show()
		
	}
	
	fun getVariantData(): List<Map<String?, Any?>> {
		return (bind.variants.adapter as ProductVariantAdapter)
			.getAllVariantData()
	}
	
	fun createProduct(
		productId: String?,
		type: String,
		images: List<Map<String, String?>>? = null,
		variantData: List<Map<String?, Any?>>? = null,
	) {
		viewModel.storeProduct(
			productId = productId,
			categoryId = categoryId,
			subCategoryId = if(subCategoryId.isEmpty()) null else subCategoryId.toInt(),
			title = bind.productTitle.value(),
			description = bind.description.value(),
			quantity = bind.quantity.value(),
			pricing = bind.price.value(),
			flashSale = (if (bind.flashSell.isChecked) "1" else "0"),
			acceptOffers = (if (bind.acceptOffers.isChecked) "1" else "0"),
			reserveForLive = (if (bind.reserveForLive.isChecked) "1" else "0"),
			shippingProfileId = "4",
			status = type,
			productImages = images,
			variant = variantData,
			width = bind.width.value(),
			height = bind.height.value(),
			length = bind.length.value(),
			weight = bind.weight.value(),
			mailClass = selectedMailClass?.label,
			processingCategory = bind.proCategory.value()
		)
		
	}
	
	private fun clearViewModelState() {
		// Clear ViewModel state when leaving the fragment
		viewModel.productFormImageList.clear()
		viewModel.productFormCategoryId = ""
		viewModel.productFormSubCategoryId = ""
		viewModel.productFormVariantList.clear()
		viewModel.productFormProduct = null
		viewModel.productFormSelectedMailClass = null
		viewModel.productFormProductTitle = ""
		viewModel.productFormDescription = ""
		viewModel.productFormQuantity = ""
		viewModel.productFormPrice = ""
		viewModel.productFormCategoryText = ""
	}
	
	override fun onPause() {
		super.onPause()
		// Save state when fragment is paused (including orientation changes)
		saveStateToViewModel()
	}
}
