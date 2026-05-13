package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
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
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.request.StoreProductRequest
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.PriceFormatter
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import android.content.Intent

@SuppressLint("NotifyDataSetChanged")
class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentListAProductBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentListAProductBinding.inflate(inflater, view, false)

    private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()
    private var uploadItemIndex = -1
    private var profiles = mutableListOf<GetShippingProfilesResponse.Data?>()
    private var profileId = ""
    var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
    private val imageList get() = viewModel.productImages
    private lateinit var variantAdapter: ProductVariantAdapter
    private val processingCategories = listOf("LETTERS", "FLATS", "MACHINABLE", "NONSTANDARD", "NON MACHINABLE")

    lateinit var imageAdapter: ImageAdapter
    private fun getPhotoCount(): Int = imageList.count { !it.isVideo }
    private fun getVideoCount(): Int = imageList.count { it.isVideo }

    private var selectedCondition = ""
    private var submitType: String = "active"

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
                            val name = it.getString(nameIndex) ?: "video_${System.currentTimeMillis()}.mp4"
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearProductData()

        bind.price.addTextChangedListener(PriceFormatter(bind.price))

        // Product is created for same category as the show.
        bind.category.isEnabled = false
        bind.subCategory.isEnabled = false

        variantAdapter = ProductVariantAdapter(variantList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
            }
        })

        bind.variants.adapter = variantAdapter

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
        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.condition.setDropDownBackgroundDrawable(draw)

        bind.condition.setOnItemClickListener { _, _, position, _ ->
            selectedCondition = conditionList[position].replace(" ", "_")
            viewModel.condition = selectedCondition
        }

        bind.condition.setHapticClickListener {
            bind.condition.showDropDown()
        }

        bind.productTitle.setText(viewModel.productTitle)
        bind.description.setText(viewModel.productDescription)
        bind.quantity.setText(viewModel.productQuantity.toString())
        bind.condition.setText(viewModel.condition.toString())
        bind.width.setText(viewModel.productWidth)
        bind.height.setText(viewModel.productHeight)
        bind.length.setText(viewModel.productLength)
        bind.weight.setText(viewModel.productWeight)

        viewModel.productMailClass?.let {
            bind.mailClass.setText(it.label, false)
        }
        viewModel.productProcessingCategory?.let {
            bind.proCategory.setText(it.replace("_", " "), false)
        }

        bind.productTitle.doAfterTextChanged {
            viewModel.productTitle = it?.toString()?.trim().orEmpty()
        }
        bind.description.doAfterTextChanged {
            viewModel.productDescription = it?.toString()?.trim().orEmpty()
        }
        bind.condition.doAfterTextChanged {
            viewModel.condition = it?.toString()?.trim().orEmpty()
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
        bind.mainLayout.setHapticClickListener { hideKeyboard(it) }
        bind.clearShippingSelection.setHapticClickListener {
            clearShippingProfileSelection()
        }
        profileId = viewModel.shippingProfile
        updateShippingDependentFields()

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
                        viewModel.productSalesFormat = "Buy It Now"
                    }

                    1 -> {
                        bind.acceptOffersLayout.isVisible = false
                        bind.flashLayout.isVisible = false
                        bind.reserveLayout.isVisible = true
                        viewModel.productSalesFormat = "Auction"
                        // Auction listings are live-only; keep switch on and non-editable.
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

        bind.seeOtherOptions.setHapticClickListener {
            bind.otherOptions.isExpanded = !bind.otherOptions.isExpanded
            if (bind.otherOptions.isExpanded) {
                bind.scroll.postDelayed({
                    bind.scroll.fullScroll(View.FOCUS_DOWN)
                }, 300)
            }
        }

        setupProcessingCategoryDropdown()

        imageAdapter = ImageAdapter(imageList.map { it.path }.toMutableList(), object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                uploadItemIndex = pos
                val item = imageList[pos]
                if (item.isVideo) {
                    uploadVideo(true)
                } else {
                    uploadImage()
                }
            }
        })

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

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.publish.setHapticClickListener {
            validateProductData("active")
        }

        bind.saveDraft.setHapticClickListener {
            validateProductData("draft")
        }

        setupMailClassDropdown()

        viewModel.getMailClasses()
        viewModel.getShippingProfile()

        setupObservers()

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

   /*     Product will be created for the same category type as the show, hence it will be disabled

     if (viewModel.getCategoryRepo.value == null) {
            bind.loader.isVisible = true
            viewModel.getCategory()
        }

        if (viewModel.getProductSubCategoryRepo.value == null) {
            bind.loader.isVisible = true
            viewModel.getProductSubCategory(viewModel.productCategoryId, "subCategory")
        }

        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.getCategoryRepo.value = null

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {

                        categoryList.clear()
                        categoryList.addAll(mData)

                        val categoryAdapter = ArrayAdapter(
                            mCtx,
                            android.R.layout.simple_list_item_1,
                            categoryList.map { it?.name })

                        bind.category.setAdapter(categoryAdapter)

                        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                        bind.category.setDropDownBackgroundDrawable(draw)

                        bind.category.setOnItemClickListener { _, _, position, _ ->
                            log("SELECTED ${categoryList[position]?.id}")
                            variantList.clear()
                            viewModel.productCategoryId = categoryList[position]?.id.toString()
                            viewModel.productCategoryName = categoryList[position]?.name.orEmpty()
                            viewModel.productSubCategoryId = ""
                            viewModel.productSubCategoryName = ""

                            bind.loader.isVisible = true
                            viewModel.getProductSubCategory(viewModel.productCategoryId, "subCategory")

                            if (categoryList[position]?.extraFields?.isNotEmpty() == true) {
                                variantList.addAll(categoryList[position]?.extraFields ?: mutableListOf())
                                variantAdapter.notifyDataSetChanged()
                            }
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
            when (it) {
                is Resource.Success -> {
                    viewModel.getProductSubCategoryRepo.value = null
                    bind.loader.isVisible = false

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

                        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                        bind.subCategory.setDropDownBackgroundDrawable(draw)

                        bind.subCategory.setOnItemClickListener { _, _, pos, _ ->
                            bind.subCategory.setText(subCategoryList[pos]?.name, false)
                            viewModel.productSubCategoryId = subCategoryList[pos]?.id.toString()
                            viewModel.productSubCategoryName = subCategoryList[pos]?.name.orEmpty()

                            if (subCategoryList[pos]?.extraFields?.isNotEmpty() == true) {
                                variantList.addAll(subCategoryList[pos]?.extraFields ?: mutableListOf())
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
        */

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
                    bind.shippingProfile.setDropDownBackgroundDrawable(draw)

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


    }

    private fun setupProcessingCategoryDropdown() {
        val proCategoryAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            processingCategories
        )
        bind.proCategory.setAdapter(proCategoryAdapter)
        val proDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.proCategory.setDropDownBackgroundDrawable(proDrawable)

        bind.proCategory.setOnItemClickListener { _, _, position, _ ->
            viewModel.productProcessingCategory = processingCategories[position]
            log("Selected processing category: ${viewModel.productProcessingCategory}")
            bind.proCategory.setText(viewModel.productProcessingCategory?.replace("_", " "), false)
        }
        bind.proCategory.setHapticClickListener {
            bind.proCategory.showDropDown()
        }
        viewModel.productProcessingCategory?.let {
            bind.proCategory.setText(it.replace("_", " "), false)
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

            if (viewModel.productMailClass?.maxWidthIn != null) {
                bind.widthTitle.text = buildSpannedString {
                    append("Width ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: " + viewModel.productMailClass?.maxWidthIn.toString() + " inches)")
                    }
                }
            }

            if (viewModel.productMailClass?.maxHeightIn != null) {
                bind.heightTitle.text = buildSpannedString {
                    append("Height ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: " + viewModel.productMailClass?.maxHeightIn.toString() + " inches)")
                    }
                }
            }
            if (viewModel.productMailClass?.maxLengthIn != null) {
                bind.lengthTitle.text = buildSpannedString {
                    append("Length ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: " + viewModel.productMailClass?.maxLengthIn.toString() + " inches)")
                    }
                }
            }

            if (viewModel.productMailClass?.maxWeightLbs != null) {
                bind.weightTitle.text = buildSpannedString {
                    append("Weight ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: " + viewModel.productMailClass?.maxWeightLbs.toString() + " lbs)")
                    }
                }
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

    private fun validateProductData(type: String = "active") {
        hideKeyboard(bind.root)
        submitType = type

        viewModel.productQuantity = bind.quantity.value().toIntOrNull()?.coerceAtLeast(1) ?: 1
        bind.quantity.setText(viewModel.productQuantity.toString())

        val packageWidth = bind.width.value().toDoubleOrNull() ?: 0.0
        val packageHeight = bind.height.value().toDoubleOrNull() ?: 0.0
        val packageLength = bind.length.value().toDoubleOrNull() ?: 0.0
        val packageWeight = bind.weight.value().toDoubleOrNull() ?: 0.0

        viewModel.productWidth = bind.width.value()
        viewModel.productHeight = bind.height.value()
        viewModel.productLength = bind.length.value()
        viewModel.productWeight = bind.weight.value()
        viewModel.productDescription = bind.description.value()
        viewModel.productTitle = bind.productTitle.value()
        viewModel.productPrice = bind.price.value()
        viewModel.variantData = variantAdapter.getAllVariantData().toMutableList()
        viewModel.shippingProfile = profileId
        viewModel.productFormFlashSale = bind.flashSell.isChecked
        viewModel.productFormAcceptOffers = bind.acceptOffers.isChecked
        // Tab 1 = Auction: always reserve for live (matches pricing tab, not switch timing).
        viewModel.productFormReserveForLive = bind.tabs.selectedTabPosition == 1

        // QA fix: the category field is disabled on this screen because the product inherits
        // the show's category. clearProductData() resets productCategoryId, then calls
        // updateCategoryField() to re-sync it from viewModel.categoryId. If for any reason
        // that sync left productCategoryId empty (e.g. show category not yet set), ensure it
        // is re-synced here before validation so the seller is never stuck on a disabled field.
        if (viewModel.productCategoryId.isEmpty() && viewModel.categoryId.isNotEmpty()) {
            viewModel.productCategoryId = viewModel.categoryId
        }

        if (type == "draft") {
            when {
                viewModel.productCategoryId.isEmpty() -> Alerts.error(mCtx, "Please select category")
                bind.productTitle.value().isEmpty() -> Alerts.error(mCtx, "Please enter product title")
                else -> saveProduct("draft")
            }
            return
        }

        when {
            imageList.none { !it.isVideo } -> Alerts.error(mCtx, "Please select at least one photo")
            viewModel.productCategoryId.isEmpty() -> Alerts.error(mCtx, "Please select category")
            bind.productTitle.value().isEmpty() -> Alerts.error(mCtx, "Please enter product title")
            bind.description.value().isEmpty() -> Alerts.error(mCtx, "Please enter description")
            profileId.isEmpty() && (packageWidth <= 0 || packageHeight <= 0 || packageLength <= 0 || packageWeight <= 0) -> Alerts.error(
                mCtx,
                "Please enter all package dimensions"
            )

            profileId.isEmpty() && viewModel.productMailClass == null -> Alerts.error(mCtx, "Please select a mail class")
            profileId.isEmpty() && viewModel.productMailClass?.maxWidthIn != null && packageWidth > (viewModel.productMailClass?.maxWidthIn ?: 0.0) ->
                Alerts.error(mCtx, "Width exceeds maximum of ${viewModel.productMailClass?.maxWidthIn} inches")

            profileId.isEmpty() && viewModel.productMailClass?.maxHeightIn != null && packageHeight > (viewModel.productMailClass?.maxHeightIn ?: 0.0) ->
                Alerts.error(mCtx, "Height exceeds maximum of ${viewModel.productMailClass?.maxHeightIn} inches")

            profileId.isEmpty() && viewModel.productMailClass?.maxLengthIn != null && packageLength > (viewModel.productMailClass?.maxLengthIn ?: 0.0) ->
                Alerts.error(mCtx, "Length exceeds maximum of ${viewModel.productMailClass?.maxLengthIn} inches")

            profileId.isEmpty() && viewModel.productMailClass?.maxWeightLbs != null && packageWeight > (viewModel.productMailClass?.maxWeightLbs ?: 0.0) ->
                Alerts.error(mCtx, "Weight exceeds maximum of ${viewModel.productMailClass?.maxWeightLbs} lbs")

            bind.proCategory.value().isEmpty() -> Alerts.error(mCtx, "Please enter processing category")
            bind.price.value().isEmpty() -> Alerts.error(mCtx, "Please enter price")
            else -> saveProduct("active")
        }
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

    private fun saveProduct(type: String = "active") {
        bind.loader.isVisible = true

        val imagePartList = mutableListOf<MultipartBody.Part>()
        val thumbnailPartList = mutableListOf<MultipartBody.Part>()
        val videoPartList = mutableListOf<MultipartBody.Part>()

        imageList.filter { !it.path.contains(Const.BASE_URL) }.forEach { mediaItem ->
            if (mediaItem.path.isNotEmpty()) {
                if (mediaItem.isVideo) {
                    val name = System.currentTimeMillis().toString() + "_product_video.mp4"
                    val videoPart = Utils.imagePart("videos[]", name, File(mediaItem.path))
                    videoPartList.add(videoPart)
                } else {
                    val name = System.currentTimeMillis().toString() + "_product_gallery.jpeg"
                    val thumbnailName = System.currentTimeMillis().toString() + "_product_thumbnail.jpeg"

                    val imagePart = Utils.imagePart("images[]", name, File(mediaItem.path))
                    imagePartList.add(imagePart)

                    val thumbnailFile = File(mediaItem.path)
                    val thumbnailPart = Utils.imagePart("thumbnails[]", thumbnailName, thumbnailFile)
                    thumbnailPartList.add(thumbnailPart)
                }
            }
        }

        if (imagePartList.isNotEmpty() || videoPartList.isNotEmpty()) {
            viewModel.storeProductMeta(imagePartList, videoPartList, thumbnailPartList)
        } else {
            createProduct(type, emptyList(), emptyList())
        }
    }

    private fun createProduct(
        type: String,
        images: List<Map<String, String?>>?,
        videos: List<Map<String, String?>>?
    ) {
        viewModel.storeProduct(
            StoreProductRequest(
                categoryId = viewModel.categoryId,
                subCategoryId = viewModel.productSubCategoryId.ifEmpty { null }?.toInt(),
                title = viewModel.productTitle,
                description = viewModel.productDescription,
                quantity = viewModel.productQuantity.toString(),
                pricing = viewModel.productPrice.ifEmpty { "1" },
                flashSale = viewModel.productFormFlashSale,
                acceptOffers = viewModel.productFormAcceptOffers,
                reserveForLive = viewModel.productFormReserveForLive,
                shippingProfileId = viewModel.shippingProfile.ifEmpty { null },
                status = type,
                images = images,
                videos = videos,
                variant = viewModel.variantData,
                width = viewModel.productWidth,
                height = viewModel.productHeight,
                length = viewModel.productLength,
                weight = viewModel.productWeight,
                mailClass = viewModel.productMailClass?.label,
                processingCategory = bind.proCategory.value().replace(" ", "_"),
                productCondition = selectedCondition.ifEmpty { viewModel.condition },
                hazardousMaterial = bind.isHazardous.isChecked,
                sku = bind.sku.value().ifEmpty { null },
                costPerItem = bind.costPerItem.value().ifEmpty { null },
            ),
            null
        )
    }

    private fun setupObservers() {
        viewModel.storeProductMetaRepo.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    viewModel.storeProductMetaRepo.value = null

                    val imageData = res.value.data?.images?.mapNotNull { data ->
                        if (data?.images != null && data.thumbnail != null) {
                            mapOf("image" to data.images, "thumbnail" to data.thumbnail)
                        } else null
                    }

                    val videoData = res.value.data?.videos?.mapNotNull { data ->
                        if (data?.videos != null) mapOf("videos" to data.videos) else null
                    }

                    createProduct(submitType, imageData, videoData)
                }

                is Resource.Error -> {
                    viewModel.storeProductMetaRepo.value = null
                    bind.loader.isVisible = false
                    res.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.storeProductRepo.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    viewModel.storeProductRepo.value = null
                    bind.loader.isVisible = false

                    val mData = res.value.data
                    if (mData != null) {
                        viewModel.currentProducts.add(mData)
                    }
                    findNavController().navigate(io.bidswipe.app.utils.ids.addProductFragment)
                }

                is Resource.Error -> {
                    viewModel.storeProductRepo.value = null
                    bind.loader.isVisible = false
                    res.parse(mCtx, TAG, object : AlertClicks {
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

    private fun updateCategoryField() {

        viewModel.productCategoryId = viewModel.categoryId
        viewModel.productSubCategoryId = viewModel.subCategoryId

        if (viewModel.productCategoryName.isEmpty()) {
            bind.category.setText("")
        }else{
            bind.category.setText(viewModel.productCategoryName)
        }

        if (viewModel.productSubCategoryName.isEmpty()) {
            bind.subCategory.setText("")
            bind.subCategoryLayout.isVisible=false
        }else{
            bind.subCategoryLayout.isVisible=true
            bind.subCategory.setText(viewModel.productSubCategoryName)
        }

    }

    private fun clearProductData() {
        viewModel.productImages.clear()
        viewModel.variantData.clear()
        viewModel.productTitle = ""
        viewModel.productDescription = ""
        viewModel.productCategoryId = ""
//        viewModel.productCategoryName = ""
        viewModel.productSubCategoryId = ""
//        viewModel.productSubCategoryName = ""
        viewModel.productQuantity = 1
        viewModel.productWidth = ""
        viewModel.productHeight = ""
        viewModel.productLength = ""
        viewModel.productWeight = ""
        viewModel.productMailClass = null
        viewModel.productProcessingCategory = null
        viewModel.condition = ""
        viewModel.shippingProfile = ""
        viewModel.productSalesFormat = ""
        viewModel.productPrice = ""
        viewModel.productFormFlashSale = false
        viewModel.productFormAcceptOffers = false
        viewModel.productFormReserveForLive = false

        updateCategoryField()
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

}
