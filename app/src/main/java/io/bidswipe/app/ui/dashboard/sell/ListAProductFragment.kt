package io.bidswipe.app.ui.dashboard.sell

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
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
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
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

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product = activity?.intent?.getSerializableExtra("product") as? GetMyInventoryResponse.Data
        Log.d("IMAGE_DEBUG", "Received product images: ${product?.images}")

        if (product != null) {
            bind.saveDraft.isVisible = false
            bind.publish.text = "Update"
            bind.header.setHeaderText("Update Product")
            addProductData(product)
        }

        log(product.toString())

        variantAdapter = ProductVariantAdapter(variantList, mClick)

        bind.variants.adapter = variantAdapter

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

        bind.category.setOnClickListener {
            showCategorySheet(categoryList, "category")
        }

        viewModel.getCategory()

        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false

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

                    /*val adapter = ArrayAdapter(
                        mCtx,
                        android.R.layout.simple_list_item_1,
                        categoryList.map { it?.name })
                    bind.category.setAdapter(adapter)
                    val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                    bind.category.setDropDownBackgroundDrawable(draw)

                    bind.category.setOnItemClickListener { _, _, position, _ ->
                        categoryId = categoryList[position]?.id.toString()
                    }
                    bind.category.setOnClickListener {
                        bind.category.showDropDown()
                    }*/

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
        val variantData = getVariantData()
        Log.d(TAG, "saveProduct: ${variantData}")
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
                imageList.filter { it?.contains(Const.BASE_URL) == false }.forEach { image ->
                    if (image != null) {
                        val name =
                            System.currentTimeMillis().toString() + "_product_gallery.jpeg"
                        val imagePart = Utils.imagePart("images[]", name, File(image ?: ""))
                        imagePart.let { element -> imagePartList.add(element) }
                    }
                }
                val productId = if (product != null) product?.id.toString() else null
                viewModel.storeProductMeta(imagePartList)
                viewModel.storeProductMetaRepo.observe(viewLifecycleOwner){
                    when (it) {
                        is Resource.Success -> {
//                            bind.loader.isVisible = false
                            viewModel.storeProduct(
                                productId = productId,
                                categoryId = categoryId,
                                title = bind.productTitle.value(),
                                description = bind.description.value(),
                                quantity = bind.quantity.value(),
                                pricing = bind.price.value(),
                                flashSale = (if (bind.flashSell.isChecked) "1" else "0"),
                                acceptOffers = (if (bind.acceptOffers.isChecked) "1" else "0"),
                                reserveForLive = (if (bind.reserveForLive.isChecked) "1" else "0"),
                                shippingProfileId = "4",
                                status = type,
                                subCategoryId = subCategoryId.ifEmpty { null },
                                productImages = it.value.data?.map { mapOf("image" to it?.images) },
                                variant = variantData.toList().map { mapOf(it.first to it.second) }
                            )
                        }
                        is Resource.Error -> {
                            bind.loader.isVisible = false
                        }
                        else -> {}
                    }
                }

            }
        }
     }

    private fun addProductData(product: GetMyInventoryResponse.Data?) {
        categoryId = product?.categoryId.toString()
        subCategoryId = product?.subCategoryId.toString()
        if (product?.subCategory != null) {
            bind.category.setText(buildSpannedString {
                append(product.category?.name)
                append("(${product.subCategory.name})")
            })
        } else {
            bind.category.setText(product?.category?.name)
        }
        bind.productTitle.setText(product?.title)
        bind.description.setText(product?.description)
        bind.quantity.setText(product?.quantity.toString())
        bind.price.setText(product?.pricing.toString())
        bind.flashSell.isChecked = product?.flashSale == true
        bind.acceptOffers.isChecked = product?.acceptOffers == true
        bind.reserveForLive.isChecked = product?.reserveForLive == true
        imageList.clear()
        product?.images?.forEach { imageUrl ->
            imageUrl?.let {
                if (imageList.size < 9) {
                    imageList.add(it)
                }
            }
        }

        Log.d(TAG, "addProductData: $imageList")
        bind.imageLimit.text = "${imageList.size}/9"
        bind.images.adapter?.notifyDataSetChanged()
    }

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
                        viewModel.getCategory(categoryId)
                        isSubCategory = true
                        if (categoryList[pos]?.extraFields?.isNotEmpty()== true){
                            Log.d(TAG, "itemClick: ${categoryList[pos]?.extraFields}")
                            variantList.addAll(categoryList[pos]?.extraFields?: mutableListOf())
                            variantAdapter.notifyDataSetChanged()
                        }
                    } else {
                        bind.category.setText(buildSpannedString {
                            append(bind.category.text)
                            append("(${subCategoryList[pos]?.name.toString()})")
                        })
                        subCategoryId = subCategoryList[pos]?.id.toString()
                        isSubCategory = false
                        if (subCategoryList[pos]?.extraFields?.isNotEmpty()== true){
                            variantList.addAll(subCategoryList[pos]?.extraFields?: mutableListOf())
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
    fun getVariantData(): Map<String, String> {
        return (bind.variants.adapter as ProductVariantAdapter)
            .getAllVariantData()
            .filter { it.second.isNotEmpty() }
            .toMap()
    }

}
