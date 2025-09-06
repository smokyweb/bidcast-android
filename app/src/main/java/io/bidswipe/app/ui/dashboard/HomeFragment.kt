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
import androidx.core.view.get
import androidx.core.view.isVisible
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.dashboard.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

@SuppressLint("NotifyDataSetChanged")
class HomeFragment : BaseFragment<DashViewModel , FragmentHomeBinding>() {

	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentHomeBinding.inflate(inflater , view , false)

	private lateinit var homeAdapter : HomeAdapter
	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var categoriesList = mutableListOf<String?>()
	private var romIdsList = mutableListOf<StreamModel>()

	private var selectedCategory = ""

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

						val showId = showList[pos]?.id.toString()

						if (App.PIPMode) {
							Alerts.error(mCtx , "You are already in Live show")

						} else {
							startActivity(
								Intent(
									mCtx ,
									ViewLiveShowActivity::class.java
								).putExtra("showId" , showId)
									.putParcelableArrayListExtra(
										"roomIdsList" ,
										romIdsList as ArrayList
									)
							)
						}

					}

				}

			}

		}
	}

	private var selectedTabText = "live"

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.setOnClickListener {
			hideKeyboard(it)
		}

		bind.main.setOnClickListener {
			hideKeyboard(it)
		}

		bind.recycler.setOnClickListener {
			hideKeyboard(it)
		}

		homeAdapter = HomeAdapter(showList , mClick)

		bind.recycler.adapter = homeAdapter

		bind.header.onMorePrimaryClick {
			startActivity(
				Intent(mCtx , NotificationActivity::class.java).putExtra(
					"slug" ,
					"notification"
				)
			)
		}

		bind.header.onMoreSecondaryClick {
			bind.searchExpandLayout.toggle()
		}
		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s : CharSequence? , start : Int , count : Int , after : Int) {}
			override fun onTextChanged(s : CharSequence? , start : Int , before : Int , count : Int) {}
			override fun afterTextChanged(s : Editable?) {
				if (! s.isNullOrEmpty()) {
					bind.loader.isVisible = true
					viewModel.getLiveShow(
						selectedTabText.request() ,
						selectedCategory.request() ,
						s.toString().request()
					)
				}
			}
		})

		bind.swipeRefreshLayout.setOnRefreshListener {
			viewModel.getLiveShow(selectedTabText.request() , selectedCategory.request())
			viewModel.getCategory()
		}

		bind.noInternet.onClick {
			bind.loader.isVisible = false
			bind.noInternet.isVisible = false
			viewModel.getLiveShow(selectedTabText.request() , selectedCategory.request())
			viewModel.getCategory()
		}

		selectTab(bind.live , true)

		bind.live.setOnClickListener { selectTab(it as TextView , false) }
		bind.popular.setOnClickListener { selectTab(it as TextView , false) }
		bind.comingSoon.setOnClickListener { selectTab(it as TextView , false) }

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
				selectedCategory = if (categoriesList[index].toString() == "For You") {
					"for_you"
				} else {
					categoriesList[index].toString()
				}
				bind.search.setText("")
				bind.loader.isVisible = true
				viewModel.getLiveShow(selectedTabText.request() , selectedCategory.request())
			}
		}

		bind.loader.isVisible = false

		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.noInternet.isVisible = false
					bind.noData.isVisible = false
					viewModel.getCategoryRepo.value = null

					val mData = it.value.data
					bind.chipGroup.removeAllViews()
					categoriesList.clear()

					categoriesList.add("For You")
					categoriesList.addAll(mData?.filter { it?.isSelected == true }
						?.map { category -> category?.name } ?: emptyList())

					categoriesList.forEach {
						bind.chipGroup.addView(
							Utils.makeAChip(
								mCtx = mCtx ,
								text = it ?: "" ,
								selected = false
							)
						)
					}

					bind.chipGroup.check(bind.chipGroup[0].id)

				}

				is Resource.Error -> {
					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.noData.isVisible = false
						bind.recycler.isVisible = false

					} else {
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

		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { it ->
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					bind.noInternet.isVisible = false
					bind.noData.isVisible = false

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
						bind.noInternet.isVisible = false

					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
						bind.noInternet.isVisible = false
					}

					homeAdapter.notifyDataSetChanged()
				}

				is Resource.Error -> {
					bind.swipeRefreshLayout.isRefreshing = false
					bind.loader.isVisible = false


					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false
						bind.noData.isVisible = false
					} else {
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

	fun selectTab(selectedTab : TextView , isFirst : Boolean) {
		val tabs = listOf(bind.live , bind.popular , bind.comingSoon)
		tabs.forEach {
			it.setTextAppearance(R.style.TitleMedium)
			it.setTextColor(ContextCompat.getColor(mCtx , R.color.outlineVariant))
		}
		selectedTab.setTextColor(ContextCompat.getColor(mCtx , R.color.scrim))
		selectedTab.setTextAppearance(R.style.TitleLarge)

		bind.search.setText("")
		bind.loader.isVisible = true

		when (selectedTab) {
			bind.live -> {
				selectedTabText = "live"
				if (! isFirst) {
					viewModel.getLiveShow("live".request() , selectedCategory.request())
				}
			}

			bind.popular -> {
				selectedTabText = "popular"
				viewModel.getLiveShow("popular".request() , selectedCategory.request())
			}

			bind.comingSoon -> {
				selectedTabText = "upcoming"
				viewModel.getLiveShow("upcoming".request() , selectedCategory.request())
			}

		}
	}

}