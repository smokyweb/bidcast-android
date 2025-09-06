package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentExploreTypeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.dashboard.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class ExploreTypeFragment : BaseFragment<DashViewModel , FragmentExploreTypeBinding>() {
	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentExploreTypeBinding.inflate(inflater , view , false)

	private lateinit var homeAdapter : HomeAdapter
	private var categoriesList = mutableListOf<String>()
	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var romIdsList = mutableListOf<StreamModel>()
	private var category = ""

	private var selectedTabText = "live"

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {

			when (status) {
				"user" -> {
					startActivity(
						Intent(mCtx , SellerProfileActivity::class.java).putExtra(
							"userId" ,
							showList[pos]?.userId.toString()
						)
					)
				}

				"viewShow" -> {
					if (showList[pos]?.isLive == true) {
						startActivity(
							Intent(
								mCtx ,
								ViewLiveShowActivity::class.java
							).putExtra("position" , pos)
								.putParcelableArrayListExtra("roomIdsList" , romIdsList as ArrayList)
						)
					}
				}
			}

		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		category = arguments?.getString("category") ?: ""
		bind.header.setHeaderText(category.asCapital())

		bind.root.setOnClickListener {
			hideKeyboard(it)
		}
		bind.main.setOnClickListener {
			hideKeyboard(it)
		}
		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			viewModel.getLiveShow(selectedTabText.request() , category.request())
		}

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s : CharSequence? , start : Int , count : Int , after : Int) {}
			override fun onTextChanged(s : CharSequence? , start : Int , before : Int , count : Int) {}
			override fun afterTextChanged(s : Editable?) {
				if (! s.isNullOrEmpty()) {
					bind.loader.isVisible = true
					viewModel.getLiveShow(selectedTabText.request() , category.request() , s.toString().request())
				}
			}
		})

		bind.noInternet.setOnClickListener {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			viewModel.getLiveShow(selectedTabText.request() , category.request())
		}

		bind.header.onMorePrimaryClick {
			startActivity(
				Intent(mCtx , NotificationActivity::class.java).putExtra(
					"slug" ,
					"notification"
				)
			)
		}

		homeAdapter = HomeAdapter(showList , mClick)

		bind.recycler.adapter = homeAdapter

		selectTab(bind.live)

		bind.live.setOnClickListener { selectTab(it as TextView) }
		bind.popular.setOnClickListener { selectTab(it as TextView) }
		bind.comingSoon.setOnClickListener { selectTab(it as TextView) }

		categoriesList = mutableListOf(category)
		viewModel.getLiveShow(selectedTabText.request() , category = category.request())
		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.noInternet.isVisible = false

					val mData = it.value.data

					mData?.forEach {
						romIdsList.add(StreamModel(it?.roomId.toString() , ""))
					}

					showList.clear()
					mData?.forEach {
						showList.add(it)
					}

					if (showList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}
					homeAdapter.notifyDataSetChanged()
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
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
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

	fun selectTab(selectedTab : TextView) {

		bind.search.setText("")

		listOf(bind.live , bind.popular , bind.comingSoon).forEach { tab ->
			tab.setTextAppearance(R.style.TitleMedium)
			tab.setTextColor(ContextCompat.getColor(mCtx , R.color.outlineVariant))
			tab.isSelected = (tab == selectedTab)
		}

		selectedTab.setTextColor(ContextCompat.getColor(mCtx , R.color.scrim))
		selectedTab.setTextAppearance(R.style.TitleLarge)

		bind.loader.isVisible = true

		when (selectedTab) {
			bind.live -> {
				selectedTabText = "live"
				viewModel.getLiveShow("live".request() , category.request())
			}

			bind.popular -> {
				selectedTabText = "popular"
				viewModel.getLiveShow("popular".request() , category.request())
			}

			bind.comingSoon -> {
				selectedTabText = "upcoming"
				viewModel.getLiveShow("upcoming".request() , category.request())
			}

		}
	}

}