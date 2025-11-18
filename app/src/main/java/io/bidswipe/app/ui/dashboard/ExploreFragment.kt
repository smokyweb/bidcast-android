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
import com.google.android.material.chip.Chip
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

@SuppressLint("NotifyDataSetChanged")
class ExploreFragment : BaseFragment<DashViewModel, FragmentExploreBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentExploreBinding.inflate(inflater, view, false)

    private lateinit var exploreAdapter: ExploreAdapter
    private var exploreList = mutableListOf<GetCategoryResponse.Data?>()
    private var currentSelectedTab: Chip? = null
    private var selectedTabText = "recommended"

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            val category = exploreList[pos]?.name
            findNavController().navigate(
                ids.goTopExploreType,
                bundleOf("category" to category)
            )

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.root.setHapticClickListener {
            hideKeyboard(it)
        }
        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }
        exploreAdapter = ExploreAdapter(exploreList, mClick)
        bind.recycler.adapter = exploreAdapter

        bind.notification.setHapticClickListener {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }


        bind.searchLayout.isEndIconVisible = false


        bind.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                bind.searchLayout.isEndIconVisible = query.isNotEmpty()

                bind.loader.isVisible = true
                bind.recycler.isVisible = false
                bind.noData.isVisible = false

                if (query.isNotEmpty()) {
                    viewModel.getCategory(type = selectedTabText, search = query, getCount = "true")
                } else {
                    viewModel.getCategory(type = selectedTabText, getCount = "true")
                }
            }
        })

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            bind.searchLayout.isEndIconVisible = false
            viewModel.getCategory(type = selectedTabText, getCount = "true")
            hideKeyboard(it)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            bind.search.setText("")
            viewModel.getCategory(type = selectedTabText, getCount = "true")
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            viewModel.getCategory(type = selectedTabText, getCount = "true")
        }

        setUpChips()


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

    }

    override fun onPause() {
        super.onPause()
        bind.search.setText("")
    }

    private fun setUpChips() {
        bind.search.setText("")
        bind.chipGroup.removeAllViews()

        listOf("Recommended", "Popular", "All").forEach {
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
                        selectedTabText = "recommended"
                        viewModel.getCategory(type = "recommended", getCount = "true")
                    }

                    1 -> {
                        selectedTabText = "popular"
                        viewModel.getCategory(type = "popular", getCount = "true")
                    }

                    2 -> {
                        selectedTabText = "all"
                        viewModel.getCategory(type = "all", getCount = "true")
                    }

                }
            }
        }

    }

}