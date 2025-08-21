package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentCreateProductBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentCreateProductBinding.inflate(inflater, view, false)

	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var currentQuantity = 1
	private var imageList = mutableListOf<String?>()
	private var uploadItemIndex = -1
	var isSubCategory = false
	private var categoryId = ""
	private var subCategoryId = ""

	var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
	private lateinit var variantAdapter: ProductVariantAdapter

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent
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
		val variantData = productData?.getSerializable("variantData") as? ArrayList<Map<String?, Any?>>

		imageList.clear()
		imageList.add(null)

		variantAdapter = ProductVariantAdapter(variantList, object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
			}
		})
		bind.variants.adapter = variantAdapter
		bind.addVariant.setOnClickListener {

		}
		bind.root.setOnClickListener {
			hideKeyboard(it)
		}
		bind.main.setOnClickListener {
			hideKeyboard(it)
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

		bind.quantity.setText(currentQuantity.toString())

		bind.increaseQuantity.setOnClickListener {
			currentQuantity++
			bind.quantity.setText(currentQuantity.toString())
		}

		bind.decreaseQuantity.setOnClickListener {
			if (currentQuantity > 1) {
				currentQuantity--
				bind.quantity.setText(currentQuantity.toString())
			}
		}

		bind.quantity.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) {
				val input = bind.quantity.text.toString().toIntOrNull() ?: 1
				currentQuantity = if (input < 1) 1 else input
				bind.quantity.setText(currentQuantity.toString())
			}
		}

		bind.category.setOnClickListener {
			showCategorySheet(categoryList,"category")
		}

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.continueBtn.setOnClickListener {
			if (validateAndNavigate()) {
				val imagePaths = ArrayList(imageList.filterNotNull())
				viewModel.variantData = variantAdapter.getAllVariantData().toMutableList()

				val bundle = bundleOf(
					"categoryId" to categoryId,
					"subCategoryId" to subCategoryId,
					"title" to bind.productTitle.text.toString().trim(),
					"description" to bind.description.text.toString().trim(),
					"quantity" to currentQuantity,
					"imagePaths" to imagePaths.joinToString(","),
				)

				try {
					findNavController().navigate(ids.goToChooseSalesFormatFragment, bundle)
				} catch (_: Exception) {
					errorToast("Error navigating to next screen")
				}
			}
		}

		bind.useProduct.setOnClickListener {
			findNavController().navigate(ids.createProductAddProductFragment)
		}

	/*	bind.category.setOnClickListener {
			bind.category.showDropDown()
		}
*/
		/*bind.category.setOnItemClickListener { _, _, position, _ ->
			categoryId = categoryList[position]?.id.toString()
			bind.category.setText(categoryList[position]?.name)
		}*/

		if (viewModel.getCategoryRepo.value == null){
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
							showCategorySheet(subCategoryList, "subCategory")
						} else {
							categoryList.clear()
							categoryList.addAll(mData)
//							isSubCategory = true
						}
					}

					isSubCategory = !isSubCategory


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

	private fun uploadImage() {
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
		val title = bind.productTitle.text.toString().trim()
		val description = bind.description.text.toString().trim()

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
		categorySheetBind.close.setOnClickListener {
			categorySheet.dismiss()
		}
		categorySheet.show()
	}

}
