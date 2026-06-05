package io.bidswipe.app.ui.randomizer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ColorSwatchAdapter
import io.bidswipe.app.controller.IconPickerAdapter
import io.bidswipe.app.controller.RandomizerSlotAdapter
import io.bidswipe.app.controller.RandomizerTemplateAdapter
import io.bidswipe.app.databinding.ActivityRandomizerTemplatesBinding
import io.bidswipe.app.databinding.SheetRandomizerBuilderBinding
import io.bidswipe.app.databinding.SheetSlotEditorBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.request.RandomizerTemplateRequest
import io.bidswipe.app.network.response.RandomizerTemplate
import io.bidswipe.app.ui.agoraStream.ProductsForLiveShowFragment
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class RandomizerTemplatesActivity : BaseActivity() {

    private val bind by bind(ActivityRandomizerTemplatesBinding::inflate)

    private lateinit var vm: RandomizerViewModel
    private lateinit var templateAdapter: RandomizerTemplateAdapter
    private var slotAdapter: RandomizerSlotAdapter? = null
    private var colorAdapter: ColorSwatchAdapter? = null
    private var iconAdapter: IconPickerAdapter? = null
    private val isShowCreationPicker: Boolean
        get() = intent.getStringExtra("from") == "show_creation_picker" ||
            intent.getStringExtra("from") == "live_show_picker"

    // Currently open builder sheet handles
    private var builderSheet: BottomSheetDialog? = null
    private var builderBind: SheetRandomizerBuilderBinding? = null
    private var slotEditorSheet: BottomSheetDialog? = null
    private var slotEditorBind: SheetSlotEditorBinding? = null

    // Which slot is currently being edited
    private var editingSlotIndex: Int = -1

    // Image picker for custom slot image (registered at Activity level)
    private val slotImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSlotImagePicked(it) }
    }

    // ── Buyer-raffle helpers ──────────────────────────────────────────────────

    /** Returns the TYPE_VALUES entry currently shown in the builder dropdown. */
    private fun currentTypeValue(bb: SheetRandomizerBuilderBinding): String {
        val label = bb.actvTemplateType.text.toString()
        val idx   = RandomizerViewModel.TYPE_LABELS.indexOf(label)
        return if (idx >= 0) RandomizerViewModel.TYPE_VALUES[idx] else "product_raffle"
    }

    /**
     * Mirror [productId]/[productTitle] onto every slot draft.
     * Caller is responsible for refreshing the adapter afterward.
     */
    private fun mirrorBuyerRaffleProduct(productId: Int?, productTitle: String?) {
        vm.builderSlots.forEach { slot ->
            slot.productId    = productId
            slot.productTitle = productTitle
        }
    }

    /** Sync the prize-product display row in the builder sheet. */
    private fun updatePrizeProductDisplay(bb: SheetRandomizerBuilderBinding) {
        val title = vm.buyerRaffleProductTitle
        if (title != null) {
            bb.tvSelectedPrizeProduct.visibility = View.VISIBLE
            bb.tvSelectedPrizeProduct.text       = "\u2713 $title"
            bb.btnPickPrizeProduct.text          = "Change prize product"
        } else {
            bb.tvSelectedPrizeProduct.visibility = View.GONE
            bb.btnPickPrizeProduct.text          = "Select prize product"
        }
    }

    /**
     * Show/hide buyer-raffle-specific UI and mirror the stored prize product
     * to all slots when entering buyer_raffle mode.
     */
    private fun applyBuyerRaffleUi(bb: SheetRandomizerBuilderBinding, typeVal: String) {
        val isBuyerRaffle = typeVal == "buyer_raffle"
        bb.layoutBuyerRafflePrize.visibility = if (isBuyerRaffle) View.VISIBLE else View.GONE
        if (isBuyerRaffle) {
            mirrorBuyerRaffleProduct(vm.buyerRaffleProductId, vm.buyerRaffleProductTitle)
            updatePrizeProductDisplay(bb)
        }
        slotAdapter?.setData(vm.builderSlots)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.root.setPadding(0, system.top, 0, system.bottom)
            CONSUMED
        }

        vm = ViewModelProvider(this)[RandomizerViewModel::class.java]

        setupHeader()
        setupRecycler()
        setupObservers()

        bind.fabAddTemplate.setHapticClickListener { openBuilder(null) }

        vm.loadTemplates()
        if (intent.getBooleanExtra("start_new_template", false)) {
            intent.putExtra("start_new_template", false)
            bind.root.post { openBuilder(null) }
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun setupHeader() {
        bind.header.setHeaderText(if (isShowCreationPicker) "Select Randomizer" else "Randomizer Templates")
        bind.header.onBackClick { finish() }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecycler() {
        templateAdapter = RandomizerTemplateAdapter(
            onEdit    = { openBuilder(it) },
            onDelete  = { confirmDelete(it) },
            onRelease = { confirmRelease(it) },
            onSelect  = if (isShowCreationPicker) {
                { template -> selectTemplate(template) }
            } else null
        )
        bind.recyclerTemplates.apply {
            layoutManager = LinearLayoutManager(this@RandomizerTemplatesActivity)
            adapter = templateAdapter
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        vm.listResponse.observe(this) { res ->
            bind.loader.isVisible = false  // no loading state in this app
            when (res) {
                is Resource.Success -> {
                    val list = res.value?.data ?: emptyList()
                    templateAdapter.setData(list)
                    bind.emptyState.isVisible = list.isEmpty()
                    bind.recyclerTemplates.isVisible = list.isNotEmpty()
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to load templates")
                else -> Unit
            }
        }

        vm.createResponse.observe(this) { res ->
            builderBind?.btnSaveTemplate?.isEnabled = true
            when (res) {
                is Resource.Success -> {
                    if (isShowCreationPicker && res.value?.data != null) {
                        selectTemplate(res.value.data)
                    } else {
                        builderSheet?.dismiss()
                        vm.loadTemplates()
                    }
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to save template")
                else -> Unit
            }
        }

        vm.updateResponse.observe(this) { res ->
            builderBind?.btnSaveTemplate?.isEnabled = true
            when (res) {
                is Resource.Success -> {
                    if (isShowCreationPicker && res.value?.data != null) {
                        selectTemplate(res.value.data)
                    } else {
                        builderSheet?.dismiss()
                        vm.loadTemplates()
                    }
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to update template")
                else -> Unit
            }
        }

        vm.deleteResponse.observe(this) { res ->
            when (res) {
                is Resource.Success -> vm.loadTemplates()
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to delete template")
                else -> Unit
            }
        }

        vm.releaseResponse.observe(this) { res ->
            when (res) {
                is Resource.Success -> {
                    Alerts.success(this, "Products released")
                    vm.loadTemplates()
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to release products")
                else -> Unit
            }
        }

        vm.slotImageResponse.observe(this) { res ->
            when (res) {
                is Resource.Success -> {
                    val url = res.value?.url
                    if (!url.isNullOrEmpty() && editingSlotIndex >= 0 && editingSlotIndex < vm.builderSlots.size) {
                        vm.builderSlots[editingSlotIndex].imageUrl = url
                        slotAdapter?.notifySlotChanged(editingSlotIndex)
                        // Update preview in slot editor sheet if still open
                        slotEditorBind?.ivSlotImagePreview?.apply {
                            visibility = View.VISIBLE
                            loadUrl(this@RandomizerTemplatesActivity, url)
                        }
                    }
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to upload slot image")
                else -> Unit
            }
        }
    }

    private fun selectTemplate(template: RandomizerTemplate) {
        val result = Intent()
            .putExtra("selectedTemplateId", template.id ?: 0)
            .putExtra("selectedTemplateName", template.name)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    // ── Builder sheet ─────────────────────────────────────────────────────────

    private fun openBuilder(template: RandomizerTemplate?) {
        vm.editingTemplate = template

        // Initialise slot drafts
        val slotCount = template?.slotCount ?: 6
        val existing = template?.slots?.map { s ->
            SlotDraft(
                position         = s.position ?: 0,
                color            = s.color ?: RandomizerViewModel.PALETTE_COLORS[0],
                icon             = s.icon,
                productId        = s.productId,
                productTitle     = s.product?.title,
                productThumbnail = s.product?.thumbnail,
                imageUrl         = s.image
            )
        } ?: emptyList()
        vm.initBuilderSlots(slotCount, existing)

        // Reset buyer-raffle prize state, then seed from slot[0] when editing a buyer_raffle template
        vm.buyerRaffleProductId    = null
        vm.buyerRaffleProductTitle = null
        val initTypeVal = template?.type ?: "product_raffle"
        if (initTypeVal == "buyer_raffle") {
            vm.buyerRaffleProductId    = existing.firstOrNull()?.productId
            vm.buyerRaffleProductTitle = existing.firstOrNull()?.productTitle
        }

        val sheet = BottomSheetDialog(this)
        val bb = SheetRandomizerBuilderBinding.inflate(layoutInflater)
        sheet.setContentView(bb.root)
        builderSheet = sheet
        builderBind  = bb

        // Pre-fill if editing
        bb.tvBuilderTitle.text   = if (template == null) "Create Template" else "Edit Template"
        bb.etTemplateName.setText(template?.name ?: "")
        bb.etEntryCost.setText(template?.entryCost ?: "0")
        bb.tvSlotCountValue.text = slotCount.toString()

        // Type dropdown
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, RandomizerViewModel.TYPE_LABELS)
        bb.actvTemplateType.setAdapter(typeAdapter)
        val currentTypeIdx = RandomizerViewModel.TYPE_VALUES.indexOf(initTypeVal)
        bb.actvTemplateType.setText(RandomizerViewModel.TYPE_LABELS[maxOf(0, currentTypeIdx)], false)

        // Slot grid
        slotAdapter = RandomizerSlotAdapter(vm.builderSlots) { idx -> openSlotEditor(idx) }
        bb.recyclerSlots.layoutManager = GridLayoutManager(this, 2)
        bb.recyclerSlots.adapter = slotAdapter

        // Apply buyer-raffle UI for the initial type
        applyBuyerRaffleUi(bb, initTypeVal)

        // Type-change listener: toggle buyer-raffle UI
        bb.actvTemplateType.setOnItemClickListener { _, _, position, _ ->
            val typeVal = RandomizerViewModel.TYPE_VALUES.getOrElse(position) { "product_raffle" }
            applyBuyerRaffleUi(bb, typeVal)
        }

        // Prize-product picker (buyer_raffle only)
        bb.btnPickPrizeProduct.setOnClickListener {
            openPrizeProductPicker()
        }

        // Stepper ─/+
        var count = slotCount
        bb.btnSlotMinus.setOnClickListener {
            if (count > 2) {
                count--
                bb.tvSlotCountValue.text = count.toString()
                vm.initBuilderSlots(count, vm.builderSlots.take(count))
                // Re-mirror prize if in buyer_raffle mode
                if (currentTypeValue(bb) == "buyer_raffle") {
                    mirrorBuyerRaffleProduct(vm.buyerRaffleProductId, vm.buyerRaffleProductTitle)
                }
                slotAdapter?.setData(vm.builderSlots)
            }
        }
        bb.btnSlotPlus.setOnClickListener {
            if (count < 12) {
                count++
                bb.tvSlotCountValue.text = count.toString()
                vm.initBuilderSlots(count, vm.builderSlots)
                // Mirror prize onto the newly added slot
                if (currentTypeValue(bb) == "buyer_raffle") {
                    mirrorBuyerRaffleProduct(vm.buyerRaffleProductId, vm.buyerRaffleProductTitle)
                }
                slotAdapter?.setData(vm.builderSlots)
            }
        }

        // Save
        bb.btnSaveTemplate.setOnClickListener {
            val name = bb.etTemplateName.value()
            if (name.isBlank()) { Alerts.error(this, "Template name is required"); return@setOnClickListener }
            val typeLabel = bb.actvTemplateType.text.toString()
            val typeIdx   = RandomizerViewModel.TYPE_LABELS.indexOf(typeLabel)
            val typeVal   = if (typeIdx >= 0) RandomizerViewModel.TYPE_VALUES[typeIdx] else "product_raffle"
            val cost      = bb.etEntryCost.value().toDoubleOrNull()
            val req = RandomizerTemplateRequest(
                name       = name,
                type       = typeVal,
                entryCost  = cost,
                slotCount  = count,
                slots      = vm.toSlotRequests()
            )
            if (template == null) vm.createTemplate(req)
            else vm.updateTemplate(template.id ?: 0, req)
        }

        sheet.show()
    }

    // ── Slot editor sheet ─────────────────────────────────────────────────────

    private fun openSlotEditor(slotIndex: Int) {
        if (slotIndex < 0 || slotIndex >= vm.builderSlots.size) return
        editingSlotIndex = slotIndex
        val draft = vm.builderSlots[slotIndex]

        val sheet = BottomSheetDialog(this)
        val sb = SheetSlotEditorBinding.inflate(layoutInflater)
        sheet.setContentView(sb.root)
        slotEditorSheet = sheet
        slotEditorBind = sb

        // For buyer_raffle, slots represent buyers — hide per-slot product picker
        val isBuyerRaffle = run {
            val label = builderBind?.actvTemplateType?.text?.toString() ?: ""
            val idx   = RandomizerViewModel.TYPE_LABELS.indexOf(label)
            if (idx >= 0) RandomizerViewModel.TYPE_VALUES[idx] == "buyer_raffle" else false
        }

        sb.tvSlotEditorTitle.text = if (isBuyerRaffle)
            "Configure Buyer #${slotIndex + 1}"
        else
            "Configure Slot #${slotIndex + 1}"

        // Product indicator (hidden for buyer_raffle — prize is set at template level)
        sb.btnPickProduct.visibility    = if (isBuyerRaffle) View.GONE else View.VISIBLE
        if (!isBuyerRaffle && draft.productTitle != null) {
            sb.tvSelectedProduct.isVisible = true
            sb.tvSelectedProduct.text = "\u2713 ${draft.productTitle}"
        }

        // Color adapter (4 columns)
        colorAdapter = ColorSwatchAdapter(RandomizerViewModel.PALETTE_COLORS, draft.color) { hex ->
            vm.builderSlots[slotIndex].color = hex
        }
        sb.recyclerColors.layoutManager = GridLayoutManager(this, 6)
        sb.recyclerColors.adapter = colorAdapter

        // Icon adapter (6 columns)
        iconAdapter = IconPickerAdapter(RandomizerViewModel.PALETTE_ICONS, draft.icon) { emoji ->
            vm.builderSlots[slotIndex].icon = emoji
        }
        sb.recyclerIcons.layoutManager = GridLayoutManager(this, 6)
        sb.recyclerIcons.adapter = iconAdapter

        // Product picker (per-slot, only for non-buyer_raffle types)
        sb.btnPickProduct.setOnClickListener {
            sheet.dismiss()
            openProductPicker(slotIndex)
        }

        // Image picker for custom slot image
        val existingImageUrl = draft.imageUrl
        if (!existingImageUrl.isNullOrEmpty()) {
            sb.ivSlotImagePreview.visibility = View.VISIBLE
            sb.ivSlotImagePreview.loadUrl(this, existingImageUrl)
        }
        sb.btnPickSlotImage.setOnClickListener {
            editingSlotIndex = slotIndex
            slotImagePickerLauncher.launch("image/*")
        }

        // Done
        sb.btnSlotDone.setOnClickListener {
            slotAdapter?.notifySlotChanged(slotIndex)
            sheet.dismiss()
        }

        sheet.show()
    }

    // ── Product picker  (reuse ProductsForLiveShowFragment) ───────────────────

    private fun openProductPicker(slotIndex: Int) {
        editingSlotIndex = slotIndex
        val frag = ProductsForLiveShowFragment.newInstance(from = "randomizer_slot")
        frag.show(supportFragmentManager, "ProductPickerForSlot")
        frag.setOnProductSelectedListener { productId, productTitle ->
            vm.builderSlots[slotIndex].productId    = productId
            vm.builderSlots[slotIndex].productTitle = productTitle
            slotAdapter?.notifySlotChanged(slotIndex)
        }
    }

    /**
     * Buyer-raffle: pick the single prize product for the whole template.
     * The selected product is mirrored to every slot in the model.
     */
    private fun openPrizeProductPicker() {
        val frag = ProductsForLiveShowFragment.newInstance(from = "randomizer_prize")
        frag.show(supportFragmentManager, "ProductPickerForPrize")
        frag.setOnProductSelectedListener { productId, productTitle ->
            vm.buyerRaffleProductId    = productId
            vm.buyerRaffleProductTitle = productTitle
            mirrorBuyerRaffleProduct(productId, productTitle)
            slotAdapter?.setData(vm.builderSlots)
            builderBind?.let { bb -> updatePrizeProductDisplay(bb) }
        }
    }

    // ── Slot image upload ────────────────────────────────────────────────────

    private fun handleSlotImagePicked(uri: Uri) {
        try {
            val name = "slot_image_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            if (!file.exists()) return
            val part = Utils.imagePart("image", name, file)
            vm.uploadSlotImage(part)
        } catch (e: Exception) {
            Alerts.error(this, "Failed to process selected image")
        }
    }

    // ── Confirm dialogs ───────────────────────────────────────────────────────

    private fun confirmDelete(template: RandomizerTemplate) {
        AppBottomSheet(
            this,
            R.drawable.ic_trash,
            "Delete Template",
            "Delete \"${template.name}\"? This will release any reserved products.",
            primaryBtnText = "Delete",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            alertType = AlertType.ERROR,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    vm.deleteTemplate(template.id ?: 0)
                }
                override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
            }
        ).show()
    }

    private fun confirmRelease(template: RandomizerTemplate) {
        AppBottomSheet(
            this,
            R.drawable.ic_gift,
            "Release Products",
            "Release all product reservations tied to \"${template.name}\"?",
            primaryBtnText = "Release",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    vm.releaseProducts(template.id ?: 0)
                }
                override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
            }
        ).show()
    }
}
