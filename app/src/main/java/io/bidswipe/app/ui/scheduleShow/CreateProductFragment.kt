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
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryListAdapter
import io.bidswipe.app.controller.ImageAdapter
import io.bidswipe.app.controller.ProductVariantAdapter
import io.bidswipe.app.databinding.AttachmentChooserSheetBinding
import io.bidswipe.app.databinding.CategoryBottomSheetBinding
import io.bidswipe.app.databinding.FragmentCreateProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MediaItem
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import java.io.File
import java.io.FileOutputStream

@SuppressLint("NotifyDataSetChanged")
class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentCreateProductBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentCreateProductBinding.inflate(inflater, view, false)

    private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var mailClassesList = mutableListOf<GetMailClassesResponse.Data.MailClasses?>()
    private var uploadItemIndex = -1
	private var profiles = mutableListOf<GetShippingProfilesResponse.Data?>()
	private var profileId = ""
	private var selectedCondition = ""
    var isSubCategory = false
    var variantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
    private val imageList get() = viewModel.productImages
    private lateinit var variantAdapter: ProductVariantAdapter
    private val processingCategories = listOf("LETTERS", "FLATS", "MACHINABLE", "NONSTANDARD", "NON_MACHINABLE")

    lateinit var imageAdapter: ImageAdapter
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

        val productData = arguments
        if (productData != null) {
            productData.getSerializable("product") as GetMyInventoryResponse.Data
        }

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
            viewModel.condition = conditionList[position].replace(" ", "_")
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
        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }
        setupProcessingCategoryDropdown()

        imageAdapter = ImageAdapter(imageList.map { it.path }.toMutableList(), object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                uploadItemIndex = pos
                uploadImage()
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
                findNavController().navigate(ids.goToChooseSalesFormatFragment)
            }
        }

        bind.useProduct.setHapticClickListener {
            findNavController().navigate(ids.createProductAddProductFragment)
        }

        setupMailClassDropdown()

        viewModel.getMailClasses()
	    viewModel.getShippingProfile()

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

        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
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

				    }

				    if (viewModel.shippingProfile.isNotEmpty()) {

					    profileId = viewModel.shippingProfile

					    val selectedShippingProfile = profiles.findLast { profile ->
						    viewModel.shippingProfile == profile?.id.toString()
					    }

					    bind.shippingProfile.setText(selectedShippingProfile?.name, false)
				    }

				    bind.shippingProfile.setHapticClickListener {
					    bind.shippingProfile.showDropDown()

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

            if (viewModel.productMailClass?.maxWidthIn != null) {
                bind.widthTitle.text = buildSpannedString {
                    append("Width ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: "+viewModel.productMailClass?.maxWidthIn.toString() + " inches)")
                    }
                }
            }
            if (viewModel.productMailClass?.maxHeightIn != null) {
                bind.heightTitle.text = buildSpannedString {
                    append("Height ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: "+viewModel.productMailClass?.maxHeightIn.toString() + " inches)")
                    }
                }
            }
            if (viewModel.productMailClass?.maxLengthIn != null) {
                bind.widthTitle.text = buildSpannedString {
                    append("Length ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: "+viewModel.productMailClass?.maxLengthIn.toString() + " inches)")
                    }
                }
            }

            if (viewModel.productMailClass?.maxWeightLbs != null) {
                bind.widthTitle.text = buildSpannedString {
                    append("Weight ")
                    color(ContextCompat.getColor(mCtx, clr.onSurfaceVariant)) {
                        append("(Max: "+viewModel.productMailClass?.maxWeightLbs.toString() + " lbs)")
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
                log("MAIL CLAS $mailClass")

                if (mailClass.maxWidthIn != null && packageWidth > mailClass.maxWidthIn) {
                    errorToast("Width exceeds maximum of ${mailClass.maxWidthIn} inches")
                    return false
                }
                if (mailClass.maxHeightIn != null && packageHeight > mailClass.maxHeightIn) {
                    errorToast("Height exceeds maximum of ${mailClass.maxHeightIn} inches")
                    return false
                }
                if (mailClass.maxLengthIn != null && packageLength > mailClass.maxLengthIn) {
                    errorToast("Length exceeds maximum of ${mailClass.maxLengthIn} inches")
                    return false
                }

                if (mailClass.maxWeightLbs != null && packageWeight > mailClass.maxWeightLbs) {
                    errorToast("Weight exceeds maximum of ${mailClass.maxWeightLbs} lbs")
                    return false
                }

                if (mailClass.maxLengthPlusGirthIn != null) {
                    val lengthPlusGirth = packageLength + (2 * packageWidth) + (2 * packageHeight)
                    if (lengthPlusGirth > mailClass.maxLengthPlusGirthIn) {
                        errorToast("Length + girth exceeds maximum of ${mailClass.maxLengthPlusGirthIn} inches")
                        return false
                    } else {
                        return true
                    }
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
