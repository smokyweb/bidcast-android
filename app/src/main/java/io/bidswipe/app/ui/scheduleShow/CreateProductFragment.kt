package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
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
import io.bidswipe.app.utils.cropper.CustomCropImageContract

import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

@SuppressLint("NotifyDataSetChanged")
class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentCreateProductBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentCreateProductBinding.inflate(inflater, view, false)

	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()
	private var uploadItemIndex = -1
	var isSubCategory = false
	var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
	private val imageList get() = viewModel.productImages
	private lateinit var variantAdapter: ProductVariantAdapter
	private val processingCategories = listOf("LETTERS", "FLATS", "MACHINABLE", "NONSTANDARD", "NON_MACHINABLE")

	private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
		if (result.isSuccessful) {
			val imagePath = result.getUriFilePath(mCtx, true)
			if (imagePath != null) {
				if (uploadItemIndex == -1) {
					if (imageList.size < 9) {
						imageList.add(imagePath)
					}
				} else {
					imageList[uploadItemIndex] = imagePath
					uploadItemIndex = -1
				}
				bind.images.adapter?.notifyDataSetChanged()
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val productData = arguments
		if (productData != null) {
			productData.getSerializable("product") as GetMyInventoryResponse.Data
		}

		variantAdapter = ProductVariantAdapter(variantList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
			}
		})
		bind.variants.adapter = variantAdapter
		bind.addVariant.setHapticClickListener {

		}
		bind.productTitle.setText(viewModel.productTitle)
		bind.description.setText(viewModel.productDescription)
		bind.quantity.setText(viewModel.productQuantity.toString())
		bind.width.setText(viewModel.productWidth)
		bind.height.setText(viewModel.productHeight)
		bind.length.setText(viewModel.productLength)
		bind.weight.setText(viewModel.productWeight)
		updateCategoryField()
		viewModel.productMailClass?.let {
			bind.mailClass.setText(it.label, false)
		}
		viewModel.productProcessingCategory?.let {
			bind.procategory.setText(it, false)
		}

		bind.productTitle.doAfterTextChanged {
			viewModel.productTitle = it?.toString()?.trim().orEmpty()
		}
		bind.description.doAfterTextChanged {
			viewModel.productDescription = it?.toString()?.trim().orEmpty()
		}
		bind.width.doAfterTextChanged {
			viewModel.productWidth = it?.toString()?.trim().orEmpty()
		}
		bind.height.doAfterTextChanged {
			viewModel.productHeight = it?.toString()?.trim().orEmpty()
		}
		bind.length.doAfterTextChanged {
			viewModel.productLength = it?.toString()?.trim().orEmpty()
		}
		bind.weight.doAfterTextChanged {
			viewModel.productWeight = it?.toString()?.trim().orEmpty()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}
		bind.main.setHapticClickListener {
			hideKeyboard(it)
		}
		setupProcessingCategoryDropdown()

		bind.images.adapter = ImageAdapter(imageList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				uploadItemIndex = pos
				uploadImage()
			}
		})

		bind.addNewImage.setHapticClickListener {
			uploadItemIndex = -1
			uploadImage()
		}

		bind.increaseQuantity.setHapticClickListener {
			viewModel.productQuantity++
			bind.quantity.setText(viewModel.productQuantity.toString())
		}

		bind.decreaseQuantity.setHapticClickListener {
			if (viewModel.productQuantity > 1) {
				viewModel.productQuantity--
				bind.quantity.setText(viewModel.productQuantity.toString())
			}
		}

		bind.quantity.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) {
				val input = bind.quantity.text.toString().toIntOrNull() ?: 1
				viewModel.productQuantity = if (input < 1) 1 else input
				bind.quantity.setText(viewModel.productQuantity.toString())
			}
		}

		bind.category.setHapticClickListener {
			showCategorySheet(categoryList, "category")
		}

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.continueBtn.setHapticClickListener {
			viewModel.productQuantity = bind.quantity.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
			bind.quantity.setText(viewModel.productQuantity.toString())
			if (validateAndNavigate()) {
				viewModel.variantData = variantAdapter.getAllVariantData().toMutableList()

				Log.d(TAG, "onViewCreated: navigating to choose sales format")
				findNavController().navigate(ids.goToChooseSalesFormatFragment)
			}
		}

		bind.useProduct.setHapticClickListener {
			findNavController().navigate(ids.createProductAddProductFragment)
		}

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

	}

	private fun setupProcessingCategoryDropdown() {
		val proCategoryAdapter = ArrayAdapter(
			mCtx,
			android.R.layout.simple_list_item_1,
			processingCategories
		)
		bind.procategory.setAdapter(proCategoryAdapter)
		val proDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
		bind.procategory.setDropDownBackgroundDrawable(proDrawable)

		bind.procategory.setOnItemClickListener { _, _, position, _ ->
			viewModel.productProcessingCategory = processingCategories[position]
			log("Selected processing category: ${viewModel.productProcessingCategory}")
			bind.procategory.setText(viewModel.productProcessingCategory, false)
		}
		bind.procategory.setHapticClickListener {
			bind.procategory.showDropDown()
		}
		viewModel.productProcessingCategory?.let {
			bind.procategory.setText(it, false)
		}
	}

	private fun setupMailClassDropdown() {
		val mailClassNames = mailClassesList.map { it?.label ?: "" }.toTypedArray()

		val adapter = ArrayAdapter(
			mCtx,
			android.R.layout.simple_list_item_1,
			mailClassNames
		)

		bind.mailClass.setAdapter(adapter)

		val drawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
		bind.mailClass.setDropDownBackgroundDrawable(drawable)

		bind.mailClass.setOnItemClickListener { _, _, position, _ ->
			viewModel.productMailClass = mailClassesList[position]
			log("Selected mail class: ${viewModel.productMailClass?.label}")
			viewModel.productMailClass?.label?.let { label ->
				bind.mailClass.setText(label, false)
			}
		}

		bind.mailClass.setHapticClickListener {
			if (mailClassesList.isNotEmpty()) {
				bind.mailClass.showDropDown()
			} else {
				viewModel.getMailClasses()
			}
		}

		viewModel.productMailClass?.label?.let {
			bind.mailClass.setText(it, false)
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

	private fun validateAndNavigate(): Boolean {
		val title = viewModel.productTitle
		val description = viewModel.productDescription
		val width = viewModel.productWidth
		val height = viewModel.productHeight
		val length = viewModel.productLength
		val weight = viewModel.productWeight

		if (title.isEmpty()) {
			errorToast("Please enter product title")
			return false
		}

		if (description.isEmpty()) {
			errorToast("Please enter product description")
			return false
		}

		if (viewModel.productCategoryId.isEmpty()) {
			errorToast("Please select a category")
			return false
		}

		if (imageList.isEmpty()) {
			errorToast("Please add at least one image")
			return false
		}

		if (viewModel.productMailClass == null) {
			errorToast("Please select a mail class")
			return false
		}

		if (viewModel.productProcessingCategory == null) {
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

			viewModel.productMailClass?.let { mailClass ->
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
					Alerts.error(mCtx, errorMessage)
					return false
				}
			}
		} catch (e: NumberFormatException) {
			errorToast("Please enter valid numeric values for dimensions")
			return false
		}

		return true
	}

	private fun showCategorySheet(categoryList: MutableList<GetCategoryResponse.Data?>, type: String) {
		val categorySheetBind = CategoryBottomSheetBinding.bind(
			layoutInflater.inflate(R.layout.category_bottom_sheet, null, false)
		)
		val categorySheet = Alerts.appBottomSheet(mCtx, true, categorySheetBind)

		variantList.clear()
		variantAdapter.notifyDataSetChanged()

		categorySheetBind.recycler.adapter = CategoryListAdapter(
			if (type == "category") categoryList else subCategoryList,
			object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {
					if (type == "category") {
						viewModel.productCategoryId = categoryList[pos]?.id.toString()
						viewModel.productCategoryName = categoryList[pos]?.name.orEmpty()
						viewModel.productSubCategoryId = ""
						viewModel.productSubCategoryName = ""
						bind.loader.isVisible = true

						viewModel.getProductSubCategory(viewModel.productCategoryId, "subCategory")
						isSubCategory = true

						if (categoryList[pos]?.extraFields?.isNotEmpty() == true) {
							variantList.addAll(categoryList[pos]?.extraFields ?: mutableListOf())
							variantAdapter.notifyDataSetChanged()
						}

					} else {
						viewModel.productSubCategoryId = subCategoryList[pos]?.id.toString()
						viewModel.productSubCategoryName = subCategoryList[pos]?.name.orEmpty()
						isSubCategory = false

						if (subCategoryList[pos]?.extraFields?.isNotEmpty() == true) {
							variantList.addAll(subCategoryList[pos]?.extraFields ?: mutableListOf())
							variantAdapter.notifyDataSetChanged()
						}
					}
					updateCategoryField()
					categorySheet.dismiss()
				}
			})

		if (type == "subCategory") {
			categorySheetBind.sheetTitle.text = buildString {
				append("Select Product Sub Category")
			}
		} else {
			categorySheetBind.sheetTitle.text = buildString {
				append("Select Product Category")
			}
		}

		categorySheetBind.close.setHapticClickListener {
			categorySheet.dismiss()
		}
		categorySheet.show()
	}

	private fun updateCategoryField() {
		val categoryName = viewModel.productCategoryName
		val subCategoryName = viewModel.productSubCategoryName

		if (categoryName.isEmpty()) {
			bind.category.setText("")
			return
		}

		val displayText = if (subCategoryName.isEmpty()) {
			categoryName
		} else {
			"$categoryName ($subCategoryName)"
		}

		bind.category.setText(displayText)
	}

}
