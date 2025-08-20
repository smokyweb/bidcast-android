package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

@SuppressLint("NotifyDataSetChanged")
class ExploreFragment : BaseFragment<DashViewModel, FragmentExploreBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentExploreBinding.inflate(inflater, view, false)

	private lateinit var exploreAdapter: ExploreAdapter
	private var exploreList = mutableListOf<GetCategoryResponse.Data?>()
	private var currentSelectedTab: TextView? = null
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

		bind.root.setOnClickListener {
			hideKeyboard(it)
		}

		exploreAdapter = ExploreAdapter(exploreList, mClick)
		bind.recycler.adapter = exploreAdapter

		bind.header.onMoreSecondaryClick {
			bind.searchExpandLayout.toggle()

			if (bind.searchExpandLayout.isExpanded) {
				bind.search.requestFocus()
			}
		}

		bind.header.onMorePrimaryClick {
			startActivity(
				Intent(mCtx, NotificationActivity::class.java).putExtra(
					"slug",
					"notification"
				)
			)
		}

		selectTab(bind.recommended)

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				if (!s.isNullOrEmpty()) {
					bind.loader.isVisible = true
					viewModel.getCategory(type = selectedTabText, search = s.toString())
				}
			}
		})

		bind.swipeRefreshLayout.setOnRefreshListener {
			viewModel.getCategory(type = selectedTabText)
		}

		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			viewModel.getCategory(type = selectedTabText)
		}

		bind.recommended.setOnClickListener { selectTab(it as TextView) }
		bind.popular.setOnClickListener { selectTab(it as TextView) }
		bind.all.setOnClickListener { selectTab(it as TextView) }

		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			bind.loader.isVisible = false
			bind.swipeRefreshLayout.isRefreshing = false
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.noInternet.isVisible = false

					exploreList.clear()

					if (it.value.data?.isNotEmpty() == true) {
						exploreList.addAll(it.value.data)
					}
					exploreAdapter.notifyDataSetChanged()

					bind.noData.isVisible = exploreList.isEmpty()

				}

				is Resource.Error -> {
					bind.noInternet.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
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

	override fun onPause() {
		super.onPause()
		bind.search.setText("")
	}

	private fun selectTab(selectedTab: TextView) {

		bind.search.setText("")

		listOf(bind.recommended, bind.popular, bind.all).forEach { tab ->
			tab.setTextAppearance(R.style.TitleMedium)
			tab.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
			tab.isSelected = (tab == selectedTab)
		}

		selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
		selectedTab.setTextAppearance(R.style.TitleLarge)

		currentSelectedTab = selectedTab

		bind.loader.isVisible = true

		when (selectedTab) {
			bind.recommended -> {
				selectedTabText = "recommended"
				viewModel.getCategory(type = "recommended")
			}

			bind.popular -> {
				selectedTabText = "popular"
				viewModel.getCategory(type = "popular")
			}

			bind.all -> {
				selectedTabText = "all"
				viewModel.getCategory(type = "all")
			}

		}

	}

}