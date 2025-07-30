package io.bidswipe.app.ui.dashboard.scheduleShow

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.databinding.FragmentCreateProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentCreateProductBinding>() {
	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentCreateProductBinding.inflate(inflater, view, false)

	private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
	private var categoryId = ""
	private var currentQuantity = 1
	private var imageList = mutableListOf<String?>()
	private var uploadItemIndex = -1

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

		imageList.clear()
		imageList.add(null)

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

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.continueBtn.setOnClickListener {
			if (validateAndNavigate()) {
				val imagePaths = ArrayList(imageList.filterNotNull())
				val bundle = bundleOf(
					"categoryId" to categoryId,
					"title" to bind.productTitle.text.toString().trim(),
					"description" to bind.description.text.toString().trim(),
					"quantity" to currentQuantity,
					"imagePaths" to imagePaths.joinToString(",")
				)

				findNavController().navigate(ids.goToChooseSalesFormatFragment, bundle)
			}
		}

		bind.useProduct.setOnClickListener {
			findNavController().navigate(ids.createProductAddProductFragment)
		}

		bind.category.setOnClickListener {
			bind.category.showDropDown()
		}

		bind.category.setOnItemClickListener { _, _, position, _ ->
			categoryId = categoryList[position]?.id.toString()
			bind.category.setText(categoryList[position]?.name)
		}

		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					if (it.value.data?.isNotEmpty() == true) {
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
					}
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

		viewModel.storeProductRepo.observe(viewLifecycleOwner) {
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
		}

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

}
