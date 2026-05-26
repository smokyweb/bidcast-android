package io.bidswipe.app.ui.randomizer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
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
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@AndroidEntryPoint
class RandomizerTemplatesActivity : BaseActivity() {

    private val bind by bind(ActivityRandomizerTemplatesBinding::inflate)

    private lateinit var vm: RandomizerViewModel
    private lateinit var templateAdapter: RandomizerTemplateAdapter
    private var slotAdapter: RandomizerSlotAdapter? = null
    private var colorAdapter: ColorSwatchAdapter? = null
    private var iconAdapter: IconPickerAdapter? = null

    // Currently open builder sheet handles
    private var builderSheet: BottomSheetDialog? = null
    private var builderBind: SheetRandomizerBuilderBinding? = null
    private var slotEditorSheet: BottomSheetDialog? = null

    // Which slot is currently being edited
    private var editingSlotIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        vm = ViewModelProvider(this)[RandomizerViewModel::class.java]

        setupHeader()
        setupRecycler()
        setupObservers()

        bind.fabAddTemplate.setHapticClickListener { openBuilder(null) }

        vm.loadTemplates()
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun setupHeader() {
        bind.header.setHeaderText("Randomizer Templates")
        bind.header.onBackClick { finish() }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecycler() {
        templateAdapter = RandomizerTemplateAdapter(
            onEdit    = { openBuilder(it) },
            onDelete  = { confirmDelete(it) },
            onRelease = { confirmRelease(it) }
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
                    builderSheet?.dismiss()
                    vm.loadTemplates()
                }
                is Resource.Error -> Alerts.error(this, res.errorResponse?.message ?: "Failed to save template")
                else -> Unit
            }
        }

        vm.updateResponse.observe(this) { res ->
            builderBind?.btnSaveTemplate?.isEnabled = true
            when (res) {
                is Resource.Success -> {
                    builderSheet?.dismiss()
                    vm.loadTemplates()
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
    }

    // ── Builder sheet ─────────────────────────────────────────────────────────

    private fun openBuilder(template: RandomizerTemplate?) {
        vm.editingTemplate = template

        // Initialise slot drafts
        val slotCount = template?.slotCount ?: 6
        val existing = template?.slots?.map { s ->
            SlotDraft(
                position       = s.position ?: 0,
                color          = s.color ?: RandomizerViewModel.PALETTE_COLORS[0],
                icon           = s.icon,
                productId      = s.productId,
                productTitle   = s.product?.title,
                productThumbnail = s.product?.thumbnail
            )
        } ?: emptyList()
        vm.initBuilderSlots(slotCount, existing)

        val sheet = BottomSheetDialog(this)
        val bb = SheetRandomizerBuilderBinding.inflate(layoutInflater)
        sheet.setContentView(bb.root)
        builderSheet = sheet
        builderBind  = bb

        // Pre-fill if editing
        bb.tvBuilderTitle.text  = if (template == null) "Create Template" else "Edit Template"
        bb.etTemplateName.setText(template?.name ?: "")
        bb.etEntryCost.setText(template?.entryCost ?: "0")
        bb.tvSlotCountValue.text = slotCount.toString()

        // Type dropdown
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, RandomizerViewModel.TYPE_LABELS)
        bb.actvTemplateType.setAdapter(typeAdapter)
        val currentTypeIdx = RandomizerViewModel.TYPE_VALUES.indexOf(template?.type ?: "product_raffle")
        bb.actvTemplateType.setText(RandomizerViewModel.TYPE_LABELS[maxOf(0, currentTypeIdx)], false)

        // Slot grid
        slotAdapter = RandomizerSlotAdapter(vm.builderSlots) { idx -> openSlotEditor(idx) }
        bb.recyclerSlots.layoutManager = GridLayoutManager(this, 2)
        bb.recyclerSlots.adapter = slotAdapter

        // Stepper ─/+
        var count = slotCount
        bb.btnSlotMinus.setOnClickListener {
            if (count > 2) {
                count--
                bb.tvSlotCountValue.text = count.toString()
                vm.initBuilderSlots(count, vm.builderSlots.take(count))
                slotAdapter?.setData(vm.builderSlots)
            }
        }
        bb.btnSlotPlus.setOnClickListener {
            if (count < 12) {
                count++
                bb.tvSlotCountValue.text = count.toString()
                vm.initBuilderSlots(count, vm.builderSlots)
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

        sb.tvSlotEditorTitle.text = "Configure Slot #${slotIndex + 1}"

        // Product indicator
        if (draft.productTitle != null) {
            sb.tvSelectedProduct.isVisible = true
            sb.tvSelectedProduct.text = "✓ ${draft.productTitle}"
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

        // Product picker
        sb.btnPickProduct.setOnClickListener {
            sheet.dismiss()
            openProductPicker(slotIndex)
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
