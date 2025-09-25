package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryListAdapter
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.controller.ProductVariantAdapter
import io.bidswipe.app.databinding.CategoryBottomSheetBinding
import io.bidswipe.app.databinding.FragmentCreateProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class CreateProductFragment : BaseFragment<ScheduleShowViewModel , FragmentCreateProductBinding>() {
	override fun getModel() : Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentCreateProductBinding.inflate(inflater , view , false)

	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()
	private var currentQuantity = 1
	private var imageList = mutableListOf<String?>()
	private var uploadItemIndex = - 1
	var isSubCategory = false
	private var categoryId = ""
	private var subCategoryId = ""
	private var selectedMailClass : GetMailClassesResponse.Data.MailClasses? = null
	var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
	private lateinit var variantAdapter : ProductVariantAdapter
	private val processingCategories = listOf("LETTERS" , "FLATS" , "MACHINABLE" , "NONSTANDARD" , "NON_MACHINABLE")
	private var selectedProcessingCategory : String? = null

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent
			val imagePath = result.getUriFilePath(mCtx , true)

			if (imagePath != null) {
				if (uploadItemIndex == - 1) {
					if (imageList.size < 9) {
						imageList.add(imagePath)
					}
				} else {
					imageList[uploadItemIndex] = imagePath
					uploadItemIndex = - 1
				}
				bind.images.adapter?.notifyDataSetChanged()
			}
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val productData = arguments
		if (productData != null) {
			val product = productData.getSerializable("product") as GetMyInventoryResponse.Data
		}

		Log.d(TAG , "onViewCreated: oncreate")
		imageList.clear()
		imageList.add(null)

		variantAdapter = ProductVariantAdapter(variantList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {
			}
		})
		bind.variants.adapter = variantAdapter
        bind.addVariant.setHapticClickListener {

		}
        bind.root.setHapticClickListener {
			hideKeyboard(it)
		}
        bind.main.setHapticClickListener {
			hideKeyboard(it)
		}
		setupProcessingCategoryDropdown()

		bind.images.adapter = ImageAdapter(imageList , object : RecyclerClicks {
			override fun itemClick(pos : Int , status : String?) {
				uploadItemIndex = pos
				uploadImage()
			}
		})

        bind.addNewImage.setHapticClickListener {
			uploadItemIndex = - 1
			uploadImage()
		}

		bind.quantity.setText(currentQuantity.toString())

        bind.increaseQuantity.setHapticClickListener {
			currentQuantity ++
			bind.quantity.setText(currentQuantity.toString())
		}

        bind.decreaseQuantity.setHapticClickListener {
			if (currentQuantity > 1) {
				currentQuantity --
				bind.quantity.setText(currentQuantity.toString())
			}
		}

		bind.quantity.setOnFocusChangeListener { _ , hasFocus ->
			if (! hasFocus) {
				val input = bind.quantity.text.toString().toIntOrNull() ?: 1
				currentQuantity = if (input < 1) 1 else input
				bind.quantity.setText(currentQuantity.toString())
			}
		}

        bind.category.setHapticClickListener {
			showCategorySheet(categoryList , "category")
		}

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

        bind.continueBtn.setHapticClickListener {
			if (validateAndNavigate()) {
				val imagePaths = ArrayList(imageList.filterNotNull())
				viewModel.variantData = variantAdapter.getAllVariantData().toMutableList()

				val bundle = bundleOf(
					"categoryId" to categoryId ,
					"subCategoryId" to subCategoryId ,
					"title" to bind.productTitle.text.toString().trim() ,
					"description" to bind.description.text.toString().trim() ,
					"quantity" to currentQuantity ,
					"imagePaths" to imagePaths.joinToString(",") ,
					"width" to bind.width.value() ,
					"height" to bind.height.value() ,
					"length" to bind.length.value() ,
					"weight" to bind.weight.value() ,
					"mailClass" to selectedMailClass?.label ,
					"processingCategory" to selectedProcessingCategory
				)
				Log.d(TAG , "onViewCreated: $bundle")

				try {
					findNavController().navigate(ids.goToChooseSalesFormatFragment , bundle)
				} catch (_ : Exception) {
					bind.loader.isVisible = false
					errorToast("Error navigating to next screen")
				}
			}
		}

        bind.useProduct.setHapticClickListener {
			findNavController().navigate(ids.createProductAddProductFragment)
		}

        /*	bind.category.setHapticClickListener {
                bind.category.showDropDown()
            }
    */
		/*bind.category.setOnItemClickListener { _, _, position, _ ->
			categoryId = categoryList[position]?.id.toString()
			bind.category.setText(categoryList[position]?.name)
		}*/

		setupMailClassDropdown()

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
					}
				}

				is Resource.Error -> {
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

		if (viewModel.getCategoryRepo.value == null) {
			bind.loader.isVisible = true
			viewModel.getCategory()
		}

		viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.getCategoryRepo.value = null

					val mData = it.value.data

					if (mData?.isNotEmpty() == true) {
						if (isSubCategory) {
							subCategoryList.clear()
							subCategoryList.addAll(mData)
							showCategorySheet(subCategoryList , "subCategory")
						} else {
							categoryList.clear()
							categoryList.addAll(mData)
//							isSubCategory = true
						}
					}

					isSubCategory = ! isSubCategory


					/*if (it.value.data?.isNotEmpty() == true) {
						categoryList.clear()
						categoryList.addAll(it.value.data)

						val adapter = ArrayAdapter(
							mCtx,
							R.layout.simple_list_item_1,
							categoryList.map { it?.name }
						)
						bind.category.setAdapter(adapter)
						val draw =
							ContextCompat.getDrawable(mCtx, io.bidswipe.app.R.drawable.card_8)
						bind.category.setDropDownBackgroundDrawable(draw)
					}*/
				}

				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(io.bidswipe.app.R.string.no_internet))
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

		/*viewModel.storeProductRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					Alerts.showBottomSheet(
						mCtx,
						it.value.message ?: "Product created successfully",
						"Success",
						false,
						object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								findNavController().navigate(ids.goToChooseSalesFormatFragment)
							}

							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
						})
				}

				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(io.bidswipe.app.R.string.no_internet))
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
		}*/

	}

	private fun setupProcessingCategoryDropdown() {
		val proCategoryAdapter = ArrayAdapter(
			mCtx ,
			android.R.layout.simple_list_item_1 ,
			processingCategories
		)
		bind.procategory.setAdapter(proCategoryAdapter)
		val proDrawable = ContextCompat.getDrawable(mCtx , R.drawable.card_8)
		bind.procategory.setDropDownBackgroundDrawable(proDrawable)

		bind.procategory.setOnItemClickListener { _ , _ , position , _ ->
			selectedProcessingCategory = processingCategories[position]
			log("Selected processing category: $selectedProcessingCategory")
		}
        bind.procategory.setHapticClickListener {
			bind.procategory.showDropDown()
		}
	}

	private fun setupMailClassDropdown() {
		val mailClassNames = mailClassesList.map { it?.label ?: "" }.toTypedArray()

		val adapter = ArrayAdapter(
			mCtx ,
			android.R.layout.simple_list_item_1 ,
			mailClassNames
		)

		bind.mailClass.setAdapter(adapter)

		val drawable = ContextCompat.getDrawable(mCtx , R.drawable.card_8)
		bind.mailClass.setDropDownBackgroundDrawable(drawable)

		bind.mailClass.setOnItemClickListener { _ , _ , position , _ ->
			selectedMailClass = mailClassesList[position]
			log("Selected mail class: ${selectedMailClass?.label}")
		}

        bind.mailClass.setHapticClickListener {
			if (mailClassesList.isNotEmpty()) {
				bind.mailClass.showDropDown()
			} else {
				viewModel.getMailClasses()
			}
		}
	}

	private fun uploadImage() {
		if (imageList.size < 9) {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(mCtx , isCamera = true , isGallery = true))
				}
			}
		} else {
			Alerts.error(mCtx , "You can select max 9 images only")
		}
	}

	private fun validateAndNavigate() : Boolean {
		val title = bind.productTitle.text.toString().trim()
		val description = bind.description.text.toString().trim()
		val width = bind.width.value()
		val height = bind.height.value()
		val length = bind.length.value()
		val weight = bind.weight.value()

		if (title.isEmpty()) {
			errorToast("Please enter product title")
			return false
		}

		if (description.isEmpty()) {
			errorToast("Please enter product description")
			return false
		}

		if (categoryId.isEmpty()) {
			errorToast("Please select a category")
			return false
		}

		if (imageList.size <= 1) {
			errorToast("Please add at least one image")
			return false
		}

		if (selectedMailClass == null) {
			errorToast("Please select a mail class")
			return false
		}
		if (selectedProcessingCategory == null) {
			errorToast("Please select a processing category")
			return false
		}

		if (width.isEmpty() || height.isEmpty() || length.isEmpty() || weight.isEmpty()) {
			errorToast("Please enter all package dimensions")
			return false
		}
		try {
			val packageWidth = width.toDouble()
			val packageHeight = height.toDouble()
			val packageLength = length.toDouble()
			val packageWeight = weight.toDouble()

			selectedMailClass?.let { mailClass ->
				val errors = mutableListOf<String>()

				if (mailClass.maxLengthIn != null && packageLength > mailClass.maxLengthIn) {
					errors.add("Length exceeds maximum of ${mailClass.maxLengthIn} inches")
				}

				if (mailClass.maxWidthIn != null && packageWidth > mailClass.maxWidthIn) {
					errors.add("Width exceeds maximum of ${mailClass.maxWidthIn} inches")
				}

				if (mailClass.maxHeightIn != null && packageHeight > mailClass.maxHeightIn) {
					errors.add("Height exceeds maximum of ${mailClass.maxHeightIn} inches")
				}

				if (mailClass.maxWeightLbs != null && packageWeight > mailClass.maxWeightLbs) {
					errors.add("Weight exceeds maximum of ${mailClass.maxWeightLbs} lbs")
				}

				if (mailClass.maxLengthPlusGirthIn != null) {
					val lengthPlusGirth = packageLength + (2 * packageWidth) + (2 * packageHeight)
					if (lengthPlusGirth > mailClass.maxLengthPlusGirthIn) {
						errors.add("Length + girth exceeds maximum of ${mailClass.maxLengthPlusGirthIn} inches")
					}
				}

				if (errors.isNotEmpty()) {
					val errorMessage = "Package doesn't meet requirements for ${mailClass.label}:\n" +
							errors.joinToString("\n")
					Alerts.error(mCtx , errorMessage)
					return false
				}
			}
		} catch (e : NumberFormatException) {
			errorToast("Please enter valid numeric values for dimensions")
			return false
		}

		return true
	}

	private fun showCategorySheet(categoryList : MutableList<GetCategoryResponse.Data?> , type : String) {
		val categorySheetBind = CategoryBottomSheetBinding.bind(
			layoutInflater.inflate(R.layout.category_bottom_sheet , null , false)
		)
		val categorySheet = Alerts.appBottomSheet(mCtx , true , categorySheetBind)

		variantList.clear()
		variantAdapter.notifyDataSetChanged()

		categorySheetBind.recycler.adapter = CategoryListAdapter(
			if (type == "category") categoryList else subCategoryList ,
			object : RecyclerClicks {
				override fun itemClick(pos : Int , status : String?) {
					if (type == "category") {
						categoryId = categoryList[pos]?.id.toString()
						bind.category.setText(categoryList[pos]?.name.toString())
						bind.loader.isVisible = true
						viewModel.getCategory(categoryId)
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

}
