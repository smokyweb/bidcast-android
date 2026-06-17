package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExampleAdapter
import io.bidswipe.app.controller.TitleAdapter
import io.bidswipe.app.databinding.FragmentShowTitleBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.randomizer.RandomizerTemplatesActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class ShowTitleFragment : BaseFragment<ScheduleShowViewModel, FragmentShowTitleBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentShowTitleBinding.inflate(inflater, view, false)

    private lateinit var titleAdapter: TitleAdapter
    private lateinit var exampleAdapter: ExampleAdapter

    private var titleList = mutableListOf<GetAllTipsResponse.Data.Tip?>()
    private var exampleList = mutableListOf<String?>()
    private val randomizerTemplateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val templateId = data.getIntExtra("selectedTemplateId", 0).takeIf { it > 0 }
        val templateName = data.getStringExtra("selectedTemplateName")
        viewModel.selectedRandomizerTemplateId = templateId
        viewModel.selectedRandomizerTemplateName = templateName
        if (!templateName.isNullOrBlank()) {
            bind.actvRandomizerTemplate.setText(templateName, false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from")

        if (from == "dash") {
            viewModel.showId = activity?.intent?.getStringExtra("showId")
            if (!viewModel.showId.isNullOrEmpty()) {
                viewModel.getShowDetails(viewModel.showId.toString())
            }
        }

        bind.header.onBackClick {
            finish()
        }

        bind.layout.setHapticClickListener {
            hideKeyboard(it)
        }

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            bind.scrollView.setPadding(
                bind.scrollView.paddingLeft,
                bind.scrollView.paddingTop,
                bind.scrollView.paddingRight,
                maxOf(system.bottom, ime.bottom) + bind.buttonLayout.height
            )
            insets
        }

        bind.showTitle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bind.scrollView.post {
                    bind.scrollView.smoothScrollTo(0, bind.showTitle.bottom + bind.buttonLayout.height)
                }
            }
        }

        titleAdapter = TitleAdapter(titleList)
        bind.recycler.adapter = titleAdapter

        exampleAdapter = ExampleAdapter(exampleList)
        bind.exampleRecycler.adapter = exampleAdapter

        bind.continueBtn.setHapticClickListener {

            when {

                bind.showTitle.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter the show title")
                    bind.showTitle.requestFocus()
                    showKeyboard(bind.showTitle)
                }

                else -> {
                    viewModel.showTitle = bind.showTitle.value()
                    hideKeyboard(it)

                    if (from == "tips" || from == "showTutorial") {
                        findNavController().navigate(ids.goToSelectCategoryFragment)
                    } else {
                        findNavController().navigate(ids.goToSelectShowTimeFragment)
                    }
                }
            }

        }

        bind.loader.isVisible = true

        viewModel.getAllTips("title".request())
        viewModel.getAllTipsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    titleList.clear()
                    exampleList.clear()

                    mData?.tips?.forEach { tip ->
                        titleList.add(tip)
                    }

                    mData?.example?.forEach { example ->
                        exampleList.add(example)
                    }

                    titleAdapter.notifyDataSetChanged()

                    exampleAdapter.notifyDataSetChanged()

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

        viewModel.getShowDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    viewModel.showTitle = mData?.title ?: ""
                    bind.showTitle.setText(viewModel.showTitle)

                    viewModel.time = Utils.getFormattedDateTime("HH:mm:ss", "HH:mm", mData?.time ?: "") ?: ""
                    viewModel.date = mData?.date ?: ""

                    viewModel.categoryId = mData?.categoryId.toString()
                    viewModel.productCategoryName=mData?.category?.name?:""

                    if(mData?.subCategoryId!=null) {
                        viewModel.subCategoryId = mData.subCategoryId.toString()
                        viewModel.productSubCategoryName = mData.subCategory?.name ?: ""
                    }

                    viewModel.auctionId = mData?.auctionTypeId.toString()

                    viewModel.repeatMode = if (mData?.isRepeat ?: false) "0" else "1"
                    viewModel.repeatType = mData?.repeatValue?.asCapital() ?: ""

                    viewModel.explicitContent = if (mData?.isExplicit ?: false) "1" else "0"
                    // Basecamp #9933883175 (2026-05-27): hydrate verified-only state from server.
                    viewModel.verifiedOnly = if (mData?.isVerifiedOnly ?: false) "1" else "0"
                    viewModel.primaryLanguage = mData?.language ?: ""

                    viewModel.discoverability = mData?.showDiscoverability ?: ""

                    viewModel.thumbnail=mData?.thumbnail?.first()?:""

                    // Basecamp #9991372302: prefill per-product stream quantities from
                    // the saved show. The server returns product_stream_quantities as a
                    // JSON-encoded STRING like {"1016":2} (verified on live API), so
                    // parse it defensively; absent/invalid -> empty map -> adapter
                    // initialises to full stock.
                    val streamQtyMap: Map<String, Int> = try {
                        val raw = mData?.productStreamQuantities
                        if (raw.isNullOrBlank()) emptyMap() else {
                            val obj = org.json.JSONObject(raw)
                            obj.keys().asSequence().associateWith { k -> obj.optInt(k, 0) }
                        }
                    } catch (_: Exception) { emptyMap() }
                    mData?.products?.forEach { data ->
                        if(data!=null) {
                            if (!viewModel.currentProducts.any { existing -> existing.id == data.id }) {
                                val savedQty = streamQtyMap[data.id?.toString()]
                                if (savedQty != null && savedQty >= 1) {
                                    data.streamQuantity = savedQty
                                }
                                viewModel.currentProducts.add(data)
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

        // ── Randomizer template dropdown ─────────────────────────────────────
        loadRandomizerTemplates()

    }

    private fun loadRandomizerTemplates() {
        val vm2 = androidx.lifecycle.ViewModelProvider(requireActivity())[io.bidswipe.app.ui.randomizer.RandomizerViewModel::class.java]
        vm2.listResponse.observe(viewLifecycleOwner) { res ->
            if (res is io.bidswipe.app.network.Resource.Success) {
                val templates = res.value?.data ?: return@observe
                val labels = mutableListOf("None", "Create new randomizer template")
                labels.addAll(templates.map { "${it.name} (${it.typeLabel()})" })
                val adapter = android.widget.ArrayAdapter(
                    mCtx,
                    android.R.layout.simple_dropdown_item_1line,
                    labels
                )
                bind.actvRandomizerTemplate.setAdapter(adapter)
                // Restore previously selected
                val prevId = viewModel.selectedRandomizerTemplateId
                if (prevId != null) {
                    val idx = templates.indexOfFirst { it.id == prevId }
                    if (idx >= 0) bind.actvRandomizerTemplate.setText(labels[idx + 2], false)
                } else {
                    bind.actvRandomizerTemplate.setText("None", false)
                }
                bind.actvRandomizerTemplate.setOnItemClickListener { _, _, position, _ ->
                    if (position == 0) {
                        viewModel.selectedRandomizerTemplateId = null
                        viewModel.selectedRandomizerTemplateName = null
                    } else if (position == 1) {
                        bind.actvRandomizerTemplate.setText(
                            viewModel.selectedRandomizerTemplateName ?: "None",
                            false
                        )
                        randomizerTemplateLauncher.launch(
                            Intent(requireContext(), RandomizerTemplatesActivity::class.java)
                                .putExtra("from", "show_creation_picker")
                                .putExtra("start_new_template", true)
                        )
                    } else {
                        val tpl = templates[position - 2]
                        viewModel.selectedRandomizerTemplateId = tpl.id
                        viewModel.selectedRandomizerTemplateName = tpl.name
                    }
                }
            }
        }
        vm2.loadTemplates()
    }

}
