package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged")
class ExploreFragment : BaseFragment<DashViewModel, FragmentExploreBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentExploreBinding.inflate(inflater, view, false)

    private lateinit var exploreAdapter: ExploreAdapter
    private var exploreList = mutableListOf<GetCategoryResponse.Data?>()
    private var selectedTabText = "all"
    private var loadingSubcategoriesForPosition: Int? = null
    private var selectedCategory: GetCategoryResponse.Data? = null
    private var oldText = ""

    private val mClick = object : RecyclerClicks {

        override fun itemClick(pos: Int, status: String?) {

            selectedCategory = exploreList.getOrNull(pos)

            if (selectedCategory == null) return

            // ---------------- SUBCATEGORY CLICK ----------------
            if (!status.isNullOrEmpty()) {
                val subcategoryPos = status.toIntOrNull() ?: return
                val subcategory = exploreAdapter.getSubcategoryAt(subcategoryPos) ?: return

                val sendSub = if (subcategory.name == "All ${selectedCategory?.name}") {
                    null
                } else {
                    subcategory.name
                }

                findNavController().navigate(
                    ids.goTopExploreType,
                    bundleOf(
                        "category" to selectedCategory?.name,
                        "subcategory" to sendSub,
//                        "sub_list" to exploreAdapter.getAllSubCategories()
                    )
                )
                return
            }

            // ---------------- CATEGORY CLICK ----------------

            val selectedPos = exploreAdapter.getSelectedPosition()
            val categoryId = selectedCategory?.id

            // Toggle OFF (already selected)
            if (selectedPos == pos && selectedPos != -1) {
                exploreAdapter.clearSelection()

                (bind.recycler.layoutManager as? GridLayoutManager)?.apply {
                    spanSizeLookup.invalidateSpanIndexCache()
                    bind.recycler.post {
                        requestLayout()
                    }
                }

                return
            }

            // Toggle ON (fetch subcategories)
            if (categoryId != null) {
                loadingSubcategoriesForPosition = pos
                viewModel.getSubCategories(listOf(categoryId))
            } else {
                // No category ID → navigate directly
                findNavController().navigate(
                    ids.goTopExploreType,
                    bundleOf("category" to selectedCategory?.name)
                )
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {

            val query = s?.toString()?.trim() ?: ""

            if (query == oldText) return
            oldText = query

            bind.searchLayout.isEndIconVisible = query.isNotEmpty()
            bind.loader.isVisible = true
            bind.recycler.isVisible = false
            bind.noData.isVisible = false

            log("Query : $selectedTabText ")

            viewModel.getCategory(type = selectedTabText, search = query.ifEmpty { null }, getCount = "true")

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log("OnViewCreated")

        bind.root.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }

        exploreAdapter = ExploreAdapter(exploreList, mClick)
        val gridLayoutManager = GridLayoutManager(mCtx, if (resources.isTablet()) 4 else 3).apply {

            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

                override fun getSpanSize(position: Int): Int {
                    // Prevent crashes during layout recalculations
                    if (position == RecyclerView.NO_POSITION ||
                        position >= exploreAdapter.itemCount
                    ) {
                        return 1
                    }

                    val viewType = exploreAdapter.getItemViewType(position)

                    // Subcategory row always takes full width on a new line
                    if (viewType == 1) {
                        return spanCount
                    }

                    // Category items always span 1 column
                    return 1
                }
            }

            spanSizeLookup.isSpanIndexCacheEnabled = false // Disable cache to recalculate on data changes
        }

        bind.recycler.apply {
            setHasFixedSize(false)
            layoutManager = gridLayoutManager
            adapter = exploreAdapter
        }

        bind.notification.setHapticClickListener {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }

        bind.searchLayout.isEndIconVisible = false

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            bind.searchLayout.isEndIconVisible = false
            hideKeyboard(it)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            bind.loader.isVisible = true
            viewModel.getCategory(type = selectedTabText, search = bind.search.value().ifEmpty { null }, getCount = "true")
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            viewModel.getCategory(type = selectedTabText, search = bind.search.value().ifEmpty { null }, getCount = "true")
        }

        setUpChips()

        if (viewModel.getCategoryRepo.value == null) {
            bind.loader.isVisible = true
            viewModel.getCategory(type = selectedTabText, getCount = "true")
        }

        // Observe subcategories response
        viewModel.getSubCategoriesRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    viewModel.getSubCategoriesRepo.value = null

                    loadingSubcategoriesForPosition?.let { position ->
                        val subcategoriesData = it.value.data?.firstOrNull()

                        log("SubCategories : ${subcategoriesData?.subcategories}")

                        if (subcategoriesData?.subcategories?.isNotEmpty() == true) {
                            val subcategories = subcategoriesData.subcategories
                            val category = exploreList.getOrNull(position)

                            // Build subcategory list - add "All [Category Name]" as first item
                            val subcategoryList = mutableListOf<GetSubCategoriesResponse.Data.Subcategory?>()

                            // Add "All" option with parent category info (matching screenshot)
                            if (category != null) {
                                val allCategory = GetSubCategoriesResponse.Data.Subcategory(
                                    id = category.id,
                                    categoryId = category.id,
                                    name = "All ${category.name}",
                                    image = category.image,
                                    thumbnail = category.thumbnail,
                                    color = category.color,
                                    extraFields = null,
                                    isSelected = false
                                )
                                subcategoryList.add(allCategory)
                            }

                            // Add actual subcategories
                            subcategoryList.addAll(subcategories)

                            // Update adapter with selected position and subcategories
                            exploreAdapter.setSelectedPosition(position, subcategoryList)

                            // Invalidate span size cache and request layout recalculation
                            (bind.recycler.layoutManager as? GridLayoutManager)?.apply {
                                spanSizeLookup.invalidateSpanIndexCache()
                                // Request layout to ensure proper grid recalculation
                                bind.recycler.post {
                                    requestLayout()
                                }
                            }

                            loadingSubcategoriesForPosition = null
                        } else {
                            findNavController().navigate(
                                ids.goTopExploreType,
                                bundleOf(
                                    "category" to selectedCategory?.name,
                                    "subcategory" to selectedCategory?.name
                                )
                            )
                        }

                    }
                }

                is Resource.Error -> {
                    viewModel.getSubCategoriesRepo.value = null

                    loadingSubcategoriesForPosition?.let { position ->
                        // Clear selection on error
                        exploreAdapter.clearSelection()
                        // Still allow navigation to category
                        val category = exploreList[position]?.name
                        findNavController().navigate(
                            ids.goTopExploreType,
                            bundleOf("category" to category)
                        )
                        loadingSubcategoriesForPosition = null
                    }
                }

                else -> {}
            }
        }

        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    exploreList.clear()

                    if (it.value.data?.isNotEmpty() == true) {
                        exploreList.addAll(it.value.data)
                        bind.recycler.isVisible = true
                        bind.noData.isVisible = false
                    } else {
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = true
                    }

                    // Clear selection when categories refresh
                    exploreAdapter.clearSelection()

                    exploreAdapter.notifyDataSetChanged()

                    bind.noData.isVisible = exploreList.isEmpty()

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = false
                    } else {
                        bind.noInternet.isVisible = false
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

        bind.search.addTextChangedListener(textWatcher)

    }

    override fun onPause() {
        super.onPause()
        bind.search.removeTextChangedListener(textWatcher)
        bind.search.setText("")
    }

    private fun setUpChips() {
        bind.chipGroup.removeAllViews()

        listOf("All", "Recommended", "Popular").forEach {
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = it,
                    selected = false,
                    closeIconVisible = false,
                    chipPadding = 12,
                )
            )
        }

        bind.chipGroup.check(bind.chipGroup[0].id)

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
                bind.loader.isVisible = true

                when (index) {

                    0 -> {
                        selectedTabText = "all"
                        viewModel.getCategory(type = "all", getCount = "true")
                    }

                    1 -> {
                        selectedTabText = "recommended"
                        viewModel.getCategory(type = "recommended", getCount = "true")
                    }

                    2 -> {
                        selectedTabText = "popular"
                        viewModel.getCategory(type = "popular", getCount = "true")
                    }
                }

            }
        }

    }

}