package io.bidswipe.app.ui.sell

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.controller.ProductVariantAdapter
import io.bidswipe.app.databinding.AttachmentChooserSheetBinding
import io.bidswipe.app.databinding.FragmentListAProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MediaItem
import io.bidswipe.app.network.request.StoreProductRequest
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.PriceFormatter
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream

@SuppressLint("NotifyDataSetChanged")
class ListAProductFragment : BaseFragment<DashViewModel, FragmentListAProductBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentListAProductBinding.inflate(inflater, view, false)

    var imageList = mutableListOf<MediaItem>()
    lateinit var imageAdapter: ImageAdapter
    var uploadItemIndex = -1
    var isSubCategory = false
    private var product: Product? = null
    private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()

    private var categoryId = ""
    private var selectedCondition = ""
    private var profileId = ""
    private var subCategoryId = ""
    var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
    private lateinit var variantAdapter: ProductVariantAdapter

    private var packageWidth = 0.0
    private var packageHeight = 0.0
    private var packageLength = 0.0
    private var packageWeight = 0.0
    private var selectedMailClass: GetMailClassesResponse.Data.MailClasses? = null
    private var profiles = mutableListOf<GetShippingProfilesResponse.Data?>()

    // Helper functions to get photo and video counts
    private fun getPhotoCount(): Int = imageList.count { !it.isVideo }
    private fun getVideoCount(): Int = imageList.count { it.isVideo }

    private fun updateMediaCounts() {
        bind.imageLimit.text = buildSpannedString {
            append("Photos: ")
            color(ContextCompat.getColor(mCtx, R.color.primary)) {
                append("${getPhotoCount()}/8")
            }
            append("  ")
            append("Video: ")
            color(ContextCompat.getColor(mCtx, R.color.primary)) {
                append("${getVideoCount()}/1")
            }
        }

        // Update RecyclerView visibility
        bind.images.isVisible = imageList.isNotEmpty()

        if (::imageAdapter.isInitialized) {
            val adapterList = imageList.map { it.path }.toMutableList()
            imageAdapter.mList.clear()
            imageAdapter.mList.addAll(adapterList)
            imageAdapter.notifyDataSetChanged()
        }

        log("imageList ${imageList.size}")
    }

    private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
        if (result.isSuccessful) {
            val imagePath = result.getUriFilePath(mCtx, true)
            if (imagePath != null) {
                val photoCount = getPhotoCount()
                if (photoCount >= 8) {
                    Alerts.error(mCtx, "You can select max 8 photos only")
                    return@registerForActivityResult
                }

                if (uploadItemIndex == -1) {
                    imageList.add(MediaItem(imagePath, isVideo = false))
                } else {
                    val existingItem = imageList[uploadItemIndex]
                    // Only allow replacing with same type
                    if (!existingItem.isVideo) {
                        imageList[uploadItemIndex] = MediaItem(imagePath, isVideo = false)
                        uploadItemIndex = -1
                    } else {
                        Alerts.error(mCtx, "Cannot replace video with photo")
                        uploadItemIndex = -1
                        return@registerForActivityResult
                    }
                }

                updateMediaCounts()
            }
        }
    }

    // Video picker contract
    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val videoPath = getVideoFilePath(it)
                if (videoPath != null) {
                    val videoCount = getVideoCount()
                    if (videoCount >= 1 && !(imageList[uploadItemIndex].isVideo)) {
                        Alerts.error(mCtx, "You can select max 1 video only")
                        return@registerForActivityResult
                    }

                    if (uploadItemIndex == -1) {
                        imageList.add(MediaItem(videoPath, isVideo = true))
                    } else {
                        val existingItem = imageList[uploadItemIndex]
                        if (existingItem.isVideo) {
                            imageList[uploadItemIndex] = MediaItem(videoPath, isVideo = true)
                            uploadItemIndex = -1
                        } else {
                            Alerts.error(mCtx, "Cannot replace photo with video")
                            uploadItemIndex = -1
                            return@registerForActivityResult
                        }
                    }

                    updateMediaCounts()
                }
            }
        }

    private fun getVideoFilePath(uri: Uri): String? {
        return try {
            if (uri.scheme == "content") {
                val cursor = mCtx.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name =
                                it.getString(nameIndex) ?: "video_${System.currentTimeMillis()}.mp4"
                            val cacheFile = File(mCtx.cacheDir, name)
                            mCtx.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (cacheFile.exists()) return cacheFile.absolutePath
                        }
                    }
                }
                // Fallback
                val file = File(mCtx.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                mCtx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.exists()) file.absolutePath else null
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                if (file.exists()) file.absolutePath else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun restoreStateFromViewModel() {
        // Restore from ViewModel if state exists
        if (viewModel.productFormImageList.isNotEmpty() || viewModel.productFormCategoryId.isNotEmpty()) {
            imageList.clear()
            // Convert String list to MediaItem list (assume all are photos for backward compatibility)
            // You may need to store type info in ViewModel if needed
            imageList.addAll(viewModel.productFormImageList.map {
                MediaItem(
                    it ?: "",
                    isVideo = false
                )
            })
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
            profileId = viewModel.shippingProfile

            // Restore form fields
            bind.productTitle.setText(viewModel.productFormProductTitle)
            bind.description.setText(viewModel.productFormDescription)
            bind.quantity.setText(viewModel.productFormQuantity)
            bind.width.setText(viewModel.productFormWidth)
            bind.height.setText(viewModel.productFormHeight)
            bind.length.setText(viewModel.productFormLength)
            bind.weight.setText(viewModel.productFormWeight)
            bind.mailClass.setText(viewModel.productFormMailClassText, false)
            bind.proCategory.setText(
                viewModel.productFormProcessingCategory.replace("_", " "),
                false
            )
            bind.price.setText(viewModel.productFormPrice)
            bind.isHazardous.isChecked = viewModel.isHazardous
            bind.condition.setText(viewModel.productCondition, false)

            bind.tabs.post {
                if (viewModel.productFormReserveForLive) {
                    bind.tabs.getTabAt(1)?.select()
                } else {
                    bind.tabs.getTabAt(0)?.select()
                    bind.flashSell.isChecked = viewModel.productFormFlashSale
                    bind.acceptOffers.isChecked = viewModel.productFormAcceptOffers
                }
            }

            if (viewModel.productFormCategoryText.isNotEmpty()) {
                if (subCategoryId.isNotEmpty()) {
                    bind.category.setText(buildSpannedString {
                        append(viewModel.productFormCategoryText)
                        append("(${viewModel.productFormProduct})")
                    })
                } else {
                    bind.category.setText(viewModel.productFormCategoryText, false)
                }
            }

            updateMediaCounts()
        }
    }

    private fun saveStateToViewModel() {
        // Save current state to ViewModel
        viewModel.productFormImageList.clear()
        // Convert MediaItem list back to String list
        viewModel.productFormImageList.addAll(imageList.map { it.path })
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
        viewModel.productFormQuantity = bind.quantity.value().toIntOrNull() ?: 1
        viewModel.productFormWidth = bind.width.value()
        viewModel.productFormHeight = bind.height.value()
        viewModel.productFormLength = bind.length.value()
        viewModel.productFormWeight = bind.weight.value()
        viewModel.productFormMailClassText = bind.mailClass.value()
        viewModel.productFormProcessingCategory = bind.proCategory.value()
        viewModel.productFormPrice = bind.price.value()
        viewModel.productFormFlashSale = bind.flashSell.isChecked
        viewModel.productFormAcceptOffers = bind.acceptOffers.isChecked
        viewModel.productFormReserveForLive = bind.tabs.selectedTabPosition == 1
        viewModel.isHazardous = bind.isHazardous.isChecked
        viewModel.productFormCategoryText = bind.category.text?.toString() ?: ""
        viewModel.productCondition = bind.condition.text?.toString() ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore state from ViewModel if available
        restoreStateFromViewModel()

        // Get product from intent only if not already restored
        if (product == null) {
            product = activity?.intent?.getSerializableExtra("product") as? Product
            if (product != null) {
                viewModel.getProductDetails(product?.id.toString().request())
            }
        }

        if (activity?.intent?.hasExtra("category") == true) {
            categoryId = activity?.intent?.getStringExtra("category").toString()
            viewModel.categoryId = categoryId
            bind.category.isEnabled = false
        } else {
            bind.category.isEnabled = true
        }

        val adapterBg = ContextCompat.getDrawable(mCtx, R.drawable.card_8)

        bind.root.setOnClickListener {
            hideKeyboard(it)
        }

        bind.mainLayout.setOnClickListener {
            hideKeyboard(it)
        }
        bind.clearShippingSelection.setHapticClickListener {
            clearShippingProfileSelection()
        }
        updateShippingDependentFields()

        // Only load product data if not restored from ViewModel
        if (product != null && viewModel.productFormProduct == null) {

            bind.saveDraft.isVisible = product?.status == "draft"

            bind.publish.text = buildSpannedString {
                append("Update")
            }
            bind.header.setHeaderText("Update Product")
        } else if (product != null && viewModel.productFormProduct != null) {
            // Restore UI state for edit mode
            bind.saveDraft.isVisible = product?.status == "draft"

            bind.publish.text = buildSpannedString {
                append("Update")
            }
            bind.header.setHeaderText("Update Product")
        }

        bind.price.addTextChangedListener(PriceFormatter(bind.price))

        bind.hazardousDesc.text = buildSpannedString {
            append("Carriers restrict shipping ")
            color(ContextCompat.getColor(mCtx, R.color.primary)) {
                append("items that may pose risk to safety")
            }
            append(", like lithium batteries.")
        }

        log(product.toString())

        variantAdapter = ProductVariantAdapter(variantList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })

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

        val processingCategories =
            listOf("LETTERS", "FLATS", "MACHINABLE", "NONSTANDARD", "NON MACHINABLE")
        val proCategoryAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            processingCategories
        )

        bind.reserveForLive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bind.acceptOffers.isChecked = false
                bind.flashSell.isChecked = false
            }
        }

        bind.acceptOffers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bind.reserveForLive.isChecked = false
            }
        }

        bind.flashSell.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bind.reserveForLive.isChecked = false
            }
        }

        bind.proCategory.setAdapter(proCategoryAdapter)
        bind.proCategory.setDropDownBackgroundDrawable(adapterBg)

        bind.proCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedProcessingCategory = processingCategories[position]
            log("Selected processing category: $selectedProcessingCategory")
        }

        bind.proCategory.setHapticClickListener {
            bind.proCategory.showDropDown()
        }

        // Update ImageAdapter to work with MediaItem
        imageAdapter = ImageAdapter(
            imageList.map { it.path }.toMutableList(),
            object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {
                    uploadItemIndex = pos
                    val item = imageList[pos]
                    if (item.isVideo) {
                        uploadVideo(true)
                    } else {
                        uploadImage()
                    }
                }
            }
        )

        bind.images.adapter = imageAdapter
        bind.images.layoutManager = LinearLayoutManager(mCtx, LinearLayoutManager.HORIZONTAL, false)
        bind.images.setHasFixedSize(false)

        // Set initial visibility
        bind.images.isVisible = imageList.isNotEmpty()

        // Update counts initially
        updateMediaCounts()

        bind.addNewImage.setHapticClickListener {
            uploadItemIndex = -1
            showMediaSelectionDialog()
        }

        bind.publish.setHapticClickListener {
            validateProductData()
        }

        bind.saveDraft.setHapticClickListener {
            validateProductData("draft")
        }

        val conditionList = mutableListOf<String>(
            "New",
            "Like New",
            "Gently Loved",
            "Well Loved",
            "Other",
            "Trending"
        )

        val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, conditionList)
        bind.condition.setAdapter(adapter)

        bind.condition.setDropDownBackgroundDrawable(adapterBg)

        bind.condition.setOnItemClickListener { _, _, position, _ ->
            selectedCondition = conditionList[position].replace(" ", "_")
        }

        bind.condition.setHapticClickListener {
            bind.condition.showDropDown()
        }

        bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                bind.flashSell.isChecked = false
                bind.acceptOffers.isChecked = false

                when (tab?.position) {
                    0 -> {
                        bind.acceptOffersLayout.isVisible = true
                        bind.flashLayout.isVisible = true
                        bind.reserveLayout.isVisible = false
                        bind.reserveForLive.isEnabled = true
                        bind.reserveForLive.isChecked = false
                        viewModel.productFormReserveForLive = false
                    }

                    1 -> {
                        bind.acceptOffersLayout.isVisible = false
                        bind.flashLayout.isVisible = false
                        bind.reserveLayout.isVisible = true
                        bind.reserveForLive.isEnabled = false
                        bind.reserveForLive.isChecked = true
                        viewModel.productFormReserveForLive = true
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }

        })

        bind.seeOtherOptions.setHapticClickListener {
            bind.otherOptions.isExpanded = !bind.otherOptions.isExpanded
            if (bind.otherOptions.isExpanded) {
                bind.scroll.postDelayed({
                    bind.scroll.fullScroll(View.FOCUS_DOWN)
                }, 300)
            }
        }

        bind.increaseQuantity.setHapticClickListener {
            viewModel.productFormQuantity++
            bind.quantity.setText(viewModel.productFormQuantity.toString())
        }

        bind.decreaseQuantity.setHapticClickListener {
            if (viewModel.productFormQuantity > 1) {
                viewModel.productFormQuantity--
                bind.quantity.setText(viewModel.productFormQuantity.toString())
            }
        }

        bind.quantity.setText(viewModel.productFormQuantity.toString())

        bind.quantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s?.trim().toString().toIntOrNull() ?: 1
                viewModel.productFormQuantity = if (input < 1) 1 else input
            }
        })

        viewModel.getCategory()

        viewModel.getShippingProfile()

        viewModel.getMailClasses()

        viewModel.getShippingProfileRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getShippingProfileRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {
                        profiles.clear()
                        profiles.addAll(mData)
                    }

                    val profileAdapter = ArrayAdapter(
                        mCtx,
                        android.R.layout.simple_list_item_1,
                        profiles.map { it?.name })

                    bind.shippingProfile.setAdapter(profileAdapter)

                    bind.shippingProfile.setDropDownBackgroundDrawable(adapterBg)

                    bind.shippingProfile.setOnItemClickListener { _, _, position, _ ->

                        profileId = profiles[position]?.id.toString()
                        viewModel.shippingProfile = profileId

                        bind.shippingProfile.setText(profiles[position]?.name, false)
                        updateShippingDependentFields()

                    }

                    if (viewModel.shippingProfile.isNotEmpty()) {

                        profileId = viewModel.shippingProfile

                        val selectedShippingProfile = profiles.findLast { profile ->
                            viewModel.shippingProfile == profile?.id.toString()
                        }

                        bind.shippingProfile.setText(selectedShippingProfile?.name, false)
                    }
                    updateShippingDependentFields()

                    bind.shippingProfile.setHapticClickListener {
                        if (profiles.isEmpty()) {
                            addShippingProfile()
                        } else {
                            bind.shippingProfile.showDropDown()
                        }

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

        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getCategoryRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {
                        categoryList.clear()
                        categoryList.addAll(mData)
                        if (categoryId.isNotEmpty()) {
                            variantList.clear()
                            val cat = categoryList.find { it?.id.toString() == categoryId }
                            bind.category.setText(cat?.name, false)
                            if (cat?.extraFields?.isNotEmpty() == true) {
                                variantList.addAll(
                                    cat.extraFields ?: mutableListOf()
                                )
                                variantAdapter.notifyDataSetChanged()
                            }
                            subCategoryId = ""
                            viewModel.getProductSubCategory(categoryId, "subCategory")
                        }

                        val categoryAdapter = ArrayAdapter(
                            mCtx,
                            android.R.layout.simple_list_item_1,
                            categoryList.map { it?.name })

                        bind.category.setAdapter(categoryAdapter)
                        bind.category.setDropDownBackgroundDrawable(adapterBg)

                        bind.category.setOnItemClickListener { _, _, position, _ ->
                            variantList.clear()
                            categoryId = categoryList[position]?.id.toString()

                            bind.category.setText(categoryList[position]?.name, false)
                            if (categoryList[position]?.extraFields?.isNotEmpty() == true) {
                                variantList.addAll(
                                    categoryList[position]?.extraFields ?: mutableListOf()
                                )
                                variantAdapter.notifyDataSetChanged()
                            }
                            bind.loader.isVisible = true
                            subCategoryId = ""
                            viewModel.getProductSubCategory(categoryId, "subCategory")
                            isSubCategory = true

                        }

                        bind.category.setHapticClickListener {
                            bind.category.showDropDown()
                        }
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
            bind.loader.isVisible = false
            when (it) {
                is Resource.Success -> {
                    viewModel.getProductSubCategoryRepo.value = null

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {
                        subCategoryList.clear()
                        subCategoryList.addAll(mData)

                        bind.subCategoryLayout.isVisible = true

                        val subCategoryAdapter = ArrayAdapter(
                            mCtx,
                            android.R.layout.simple_list_item_1,
                            subCategoryList.map { it?.name })

                        bind.subCategory.setAdapter(subCategoryAdapter)

                        bind.subCategory.setDropDownBackgroundDrawable(adapterBg)

                        bind.subCategory.setOnItemClickListener { _, _, position, _ ->

                            subCategoryId = subCategoryList[position]?.id.toString()

                            bind.subCategory.setText(subCategoryList[position]?.name, false)

                            if (subCategoryList[position]?.extraFields?.isNotEmpty() == true) {
                                variantList.addAll(
                                    subCategoryList[position]?.extraFields ?: mutableListOf()
                                )
                                variantAdapter.notifyDataSetChanged()
                            }

                        }

                        bind.subCategory.setHapticClickListener {
                            bind.subCategory.showDropDown()
                        }

                    } else {
                        bind.subCategoryLayout.isVisible = false
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
                                activity?.setResult(RESULT_OK)
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
                                selectedMailClass?.label.equals(mailClass?.label, ignoreCase = true)
                            }
                            if (sel != null) {
                                selectedMailClass = sel
                                bind.mailClass.setText(sel.label, false)
                                setMailClassTexts()
                            }
                        } else {
                            val sel = mailClassesList.find { mailClasses -> mailClasses?.label == product?.mailClass }

                            if (sel != null) {
                                selectedMailClass = sel
                                bind.mailClass.setText(sel.label, false)
                                setMailClassTexts()
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

        viewModel.getProductDetailsRepo.observe(viewLifecycleOwner) {
            bind.loader.isVisible = false

            when (it) {
                is Resource.Success -> {
                    // Clear ViewModel state on successful save
                    val mData = it.value.data

                    addProductData(mData)

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
            selectedMailClass = mailClassesList[position]
            log("Selected mail class: ${selectedMailClass?.label}")
            // Save state to ViewModel
            saveStateToViewModel()

            bind.mailClass.setText(selectedMailClass?.label ?: "", false)
            setMailClassTexts()
        }

        bind.mailClass.setHapticClickListener {
            if (mailClassesList.isNotEmpty()) {
                bind.mailClass.showDropDown()
            } else {
                viewModel.getMailClasses()
            }
        }
    }

    fun setMailClassTexts() {
        if (selectedMailClass?.maxWidthIn != null) {
            bind.widthTitle.text = buildSpannedString {
                append("Width ")
                color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                    append("(Max: " + selectedMailClass?.maxWidthIn.toString() + " inches)")
                }
            }
        }

        if (selectedMailClass?.maxHeightIn != null) {
            bind.heightTitle.text = buildSpannedString {
                append("Height ")
                color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                    append("(Max: " + selectedMailClass?.maxHeightIn.toString() + " inches)")
                }
            }
        }

        if (selectedMailClass?.maxLengthIn != null) {
            bind.lengthTitle.text = buildSpannedString {
                append("Length ")
                color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                    append("(Max: " + selectedMailClass?.maxLengthIn.toString() + " inches)")
                }
            }
        }

        if (selectedMailClass?.maxWeightLbs != null) {
            bind.weightTitle.text = buildSpannedString {
                append("Weight ")
                color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                    append("(Max: " + selectedMailClass?.maxWeightLbs.toString() + " lbs)")
                }
            }
        }
    }

    private fun showMediaSelectionDialog() {
        val photoCount = getPhotoCount()
        val videoCount = getVideoCount()

        val options = mutableListOf<String>()
        if (photoCount < 8) {
            options.add("Add Photo")
        }
        if (videoCount < 1) {
            options.add("Add Video")
        }

        if (options.isEmpty()) {
            Alerts.error(mCtx, "Maximum media limit reached (8 photos, 1 video)")
            return
        }

        if (options.size == 1) {
            // Only one option available, directly call it
            if (options[0] == "Add Photo") {
                uploadImage()
            } else {
                uploadVideo()
            }
        } else {
            // Show dialog to choose
            val attachmentSheetBind =
                AttachmentChooserSheetBinding.bind(
                    layoutInflater.inflate(
                        R.layout.attachment_chooser_sheet,
                        null,
                        false
                    )
                )
            val attachmentSheet = Alerts.appBottomSheet(mCtx, true, attachmentSheetBind)

            attachmentSheetBind.image.setHapticClickListener {
                attachmentSheet.dismiss()
                uploadImage()
            }
            attachmentSheetBind.video.setHapticClickListener {
                attachmentSheet.dismiss()
                uploadVideo()
            }
            attachmentSheetBind.cancel.setHapticClickListener {
                attachmentSheet.dismiss()
            }
            attachmentSheet.show()

        }
    }

    fun uploadImage() {
        val photoCount = getPhotoCount()
        if (photoCount >= 8) {
            Alerts.error(mCtx, "You can select max 8 photos only")
            return
        }

        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                imageResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
            }
        }
    }

    fun uploadVideo(isReplace: Boolean = false) {
        val videoCount = getVideoCount()
        if (videoCount >= 1 && !isReplace) {
            Alerts.error(mCtx, "You can select max 1 video only")
            return
        }

        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                videoPicker.launch("video/*")
            }
        }
    }

    fun validateProductData(type: String = "active") {
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
        val hasSelectedShippingProfile = profileId.isNotEmpty()

        if (type == "draft") {

            when {

                categoryId.isEmpty() -> {
                    Alerts.error(mCtx, "Please select category")
                }

                bind.productTitle.value().isEmpty() -> {
                    bind.productTitle.requestFocus()
                    Alerts.error(mCtx, "Please enter product title")
                }

                else -> {

                    saveProduct("draft")

                }
            }

        } else {

            when {

                imageList.none { !it.isVideo } -> {
                    Alerts.error(mCtx, "Please select at least one photo")
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

                !hasSelectedShippingProfile && (packageWidth <= 0 || packageHeight <= 0 || packageLength <= 0 || packageWeight <= 0) -> {
                    Alerts.error(mCtx, "Please enter all package dimensions")
                }

                !hasSelectedShippingProfile && selectedMailClass?.maxWidthIn != null && (packageWidth > (selectedMailClass?.maxWidthIn
                    ?: 0.0)) -> {
                    Alerts.error(
                        mCtx,
                        "Width exceeds maximum of ${selectedMailClass?.maxWidthIn} inches"
                    )
                }

                !hasSelectedShippingProfile && selectedMailClass?.maxHeightIn != null && (packageHeight > (selectedMailClass?.maxHeightIn
                    ?: 0.0)) -> {
                    Alerts.error(
                        mCtx,
                        "Height exceeds maximum of ${selectedMailClass?.maxHeightIn} inches"
                    )
                }

                !hasSelectedShippingProfile && selectedMailClass?.maxLengthIn != null && (packageLength > (selectedMailClass?.maxLengthIn
                    ?: 0.0)) -> {
                    Alerts.error(
                        mCtx,
                        "Length exceeds maximum of ${selectedMailClass?.maxLengthIn} inches"
                    )
                }

                !hasSelectedShippingProfile && selectedMailClass?.maxWeightLbs != null && (packageWeight > (selectedMailClass?.maxWeightLbs
                    ?: 0.0)) -> {
                    Alerts.error(
                        mCtx,
                        "Weight exceeds maximum of ${selectedMailClass?.maxWeightLbs} lbs"
                    )
                }

                !hasSelectedShippingProfile && selectedMailClass == null -> {
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

                else -> {
                    saveProduct("active")
                }
            }
        }
    }

    private fun addProductData(product: GetProductDetailsResponse.Data?) {

        categoryId = if (product?.categoryId != null) product.categoryId.toString() else ""

        subCategoryId = if (product?.subCategoryId != null) product.subCategoryId.toString() else ""

        bind.category.setText(product?.category?.name ?: "Other", false)

        if (!subCategoryId.isEmpty()) {
            bind.subCategoryLayout.isVisible = true
            bind.subCategory.setText(product?.subCategory?.name ?: "Other", false)
            viewModel.getProductSubCategory(categoryId, "subCategory")
        }

        bind.productTitle.setText(product?.title ?: "")
        bind.description.setText(product?.description ?: "")
        bind.quantity.setText((product?.quantity ?: ""))
        bind.width.setText((product?.width ?: "").toString())
        bind.height.setText((product?.height ?: "").toString())
        bind.length.setText((product?.length ?: "").toString())
        bind.weight.setText((product?.weight ?: "").toString())
        bind.mailClass.setText(product?.mailClass ?: "", false)
        bind.proCategory.setText(product?.processingCategory?.replace("_", " ") ?: "", false)
        selectedCondition = product?.productCondition.toString()
        bind.condition.setText(product?.productCondition?.replace("_", " ") ?: "", false)
        bind.price.setText((product?.pricing ?: ""))
        bind.tabs.post {
            when {
                product?.auction == true -> bind.tabs.getTabAt(1)?.select()
                else -> {
                    bind.tabs.getTabAt(0)?.select()
                    bind.flashSell.isChecked = product?.flashSale == true
                    bind.acceptOffers.isChecked = product?.acceptOffers == true
                }
            }
        }
        bind.isHazardous.isChecked = product?.hazardousMaterial == true
        viewModel.shippingProfile = product?.shippingProfileId.toString()

        if (profiles.isNotEmpty()) {
            val selectedShippingProfile = profiles.findLast { profile ->
                viewModel.shippingProfile == profile?.id.toString()
            }

            profileId = viewModel.shippingProfile

            bind.shippingProfile.setText(selectedShippingProfile?.name, false)
        }
        updateShippingDependentFields()

        imageList.clear()
        product?.images?.forEachIndexed { index, imageUrl ->
            imageUrl?.let {
                val photoCount = getPhotoCount()
                if (photoCount < 8) {
                    imageList.add(MediaItem(it, isVideo = false))
                }
            }
        }

        product?.videos?.forEachIndexed { index, videoUrl ->
            videoUrl?.let {
                val videoCount = getVideoCount()
                if (videoCount < 1) {
                    imageList.add(MediaItem(it, isVideo = true))
                }
            }
        }
        // Note: If product has video, you'll need to add it here

        updateMediaCounts()
        // Update adapter if initialized
        if (::imageAdapter.isInitialized) {
            imageAdapter.mList.clear()
            imageAdapter.mList.addAll(imageList.map { it.path })
            imageAdapter.notifyDataSetChanged()
        }
    }

    private fun clearShippingProfileSelection() {
        profileId = ""
        viewModel.shippingProfile = ""
        bind.shippingProfile.setText("", false)
        updateShippingDependentFields()
    }

    private fun updateShippingDependentFields() {
        val hasSelectedShippingProfile = profileId.isNotEmpty()
        bind.shippingDependentFields.isVisible = !hasSelectedShippingProfile
        bind.clearShippingSelection.isVisible = hasSelectedShippingProfile
    }

    fun getVariantData(): List<Map<String?, Any?>> {
        return (bind.variants.adapter as ProductVariantAdapter)
            .getAllVariantData()
    }

    fun createProduct(
        productId: String?,
        type: String,
        images: List<Map<String, String?>>? = null,
        videos: List<Map<String, String?>>? = null,
        variantData: List<Map<String?, Any?>>? = null,
    ) {
        viewModel.storeProduct(
            StoreProductRequest(
                categoryId = categoryId,
                subCategoryId = if (subCategoryId.isEmpty()) null else subCategoryId.toInt(),
                title = bind.productTitle.value(),
                description = bind.description.value(),
                quantity = bind.quantity.value(),
                pricing = bind.price.value(),
                flashSale = bind.flashSell.isChecked,
                acceptOffers = bind.acceptOffers.isChecked,
                reserveForLive = bind.tabs.selectedTabPosition == 1,
                shippingProfileId = profileId.ifEmpty { null },
                status = type,
                images = images,
                videos = videos,
                variant = variantData,
                width = bind.width.value(),
                height = bind.height.value(),
                length = bind.length.value(),
                weight = bind.weight.value(),
                mailClass = selectedMailClass?.label,
                processingCategory = bind.proCategory.value().replace(" ", "_"),
                productCondition = selectedCondition,
                hazardousMaterial = bind.isHazardous.isChecked,
                sku = bind.sku.value(),
                costPerItem = bind.costPerItem.value(),
            ),
            productId = productId?.ifEmpty { null }
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
        viewModel.productFormQuantity = 1
        viewModel.productFormPrice = ""
        viewModel.productFormCategoryText = ""
    }

    private fun saveProduct(type: String = "active") {

        val variantData = getVariantData()

        bind.loader.isVisible = true
        // Save final state before submission
        saveStateToViewModel()

        val imagePartList = mutableListOf<MultipartBody.Part>()
        val thumbnailPartList = mutableListOf<MultipartBody.Part>()
        val videoPartList = mutableListOf<MultipartBody.Part>()

        // Separate photos and videos
        imageList.filter { !it.path.contains(Const.BASE_URL) }.forEach { mediaItem ->
            if (mediaItem.path.isNotEmpty()) {
                if (mediaItem.isVideo) {
                    // Handle video upload
                    val name = System.currentTimeMillis().toString() + "_product_video.mp4"
                    val videoPart = Utils.imagePart("videos[]", name, File(mediaItem.path))
                    videoPart.let { element -> videoPartList.add(element) }
                } else {
                    // Handle photo upload
                    val name = System.currentTimeMillis().toString() + "_product_gallery.jpeg"
                    val thumbnailName =
                        System.currentTimeMillis().toString() + "_product_thumbnail.jpeg"

                    val imagePart = Utils.imagePart("images[]", name, File(mediaItem.path))
                    imagePart.let { element -> imagePartList.add(element) }

                    val thumbnailFile = File(mediaItem.path)
                    val thumbnailPart =
                        Utils.imagePart("thumbnails[]", thumbnailName, thumbnailFile)
                    thumbnailPart.let { element -> thumbnailPartList.add(element) }
                }
            }
        }

        val productId = if (product != null) product?.id.toString() else ""
        if (imagePartList.isNotEmpty() || videoPartList.isNotEmpty()) {
            // You may need to update storeProductMeta to handle videos
            viewModel.storeProductMeta(imagePartList, videoPartList, thumbnailPartList)

            viewModel.storeProductMetaRepo.observe(viewLifecycleOwner) {
                when (it) {
                    is Resource.Success -> {
                        createProduct(
                            productId,
                            type,
                            it.value.data?.images?.map { productMeta ->
                                mapOf(
                                    "image" to productMeta?.images,
                                    "thumbnail" to productMeta?.thumbnail
                                )
                            },

                            it.value.data?.videos?.map { productMeta ->
                                mapOf(
                                    "videos" to productMeta?.videos,
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
            createProduct(productId.toString(), type, emptyList(), emptyList(), variantData)
        }

    }

    override fun onPause() {
        super.onPause()
        // Save state when fragment is paused (including orientation changes)
        saveStateToViewModel()
    }

    private fun addShippingProfile() {
        AppBottomSheet(
            mCtx,
            R.drawable.ic_info,
            "No Shipping Profile Found!",
            "You have not added any shipping profile. Are you sure you want to add shipping profile?",
            primaryBtnText = "Yes",
            secondaryBtnText = "No",
            canCancel = true,
            showSecondary = true,
            alertType = AlertType.INFO,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()

                    startActivity(
                        Intent(mCtx, SellerHubActivity::class.java).putExtra(
                            "slug",
                            "createShippingProfile"
                        )
                    )


                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }

        ).show()

    }

    override fun onResume() {
        super.onResume()
        viewModel.getShippingProfile()
    }

}

