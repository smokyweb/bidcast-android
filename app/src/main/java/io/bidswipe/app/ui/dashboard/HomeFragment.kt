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
import androidx.recyclerview.widget.GridLayoutManager
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
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import java.util.ArrayList

@SuppressLint("NotifyDataSetChanged")
class HomeFragment : BaseFragment<DashViewModel , FragmentHomeBinding>() {

	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentHomeBinding.inflate(inflater , view , false)

	private lateinit var homeAdapter : HomeAdapter
	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var categoriesList = mutableListOf<String?>()
	private var romIdsList = mutableListOf<String>()
	private var streamList = mutableListOf< StreamModel>()
	private var page = 1
	private var isLoading = false

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

					val roomId = showList[pos]?.roomId.toString()

					startActivity(
						Intent(
							mCtx ,
							ViewLiveShowActivity::class.java
						).putExtra("roomId" , roomId)
							.putExtra("userId" , showList[pos]?.userId.toString())
							.putExtra(
								"roomIdsList" ,
								romIdsList.joinToString(",")
							)
							.putParcelableArrayListExtra(
								"streamList" ,
								ArrayList(streamList)
							)
					)

				/*	if (showList[pos]?.isLive == true) {
						val roomId = showList[pos]?.roomId.toString()
						print("ROOM $romIdsList")
						if (App.PIPMode) {
							Alerts.error(mCtx , "You are already in Live show")
						} else {
							startActivity(
								Intent(
									mCtx ,
									ViewLiveShowActivity::class.java
								).putExtra("roomId" , roomId)
									.putExtra("userId" , showList[pos]?.userId.toString())
									.putExtra(
										"roomIdsList" ,
										romIdsList.joinToString(",")
									)
							)
						}
					}*/
				}
			}
		}
	}

	private var selectedTabText = "live"

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.searchLayout.setEndIconOnClickListener {
			bind.search.setText("")
			hideKeyboard(it)
		}

		bind.main.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.recycler.setHapticClickListener {
			hideKeyboard(it)
		}

		homeAdapter = HomeAdapter(showList , mClick)

		bind.recycler.adapter = homeAdapter

		bind.header.onMorePrimaryClick {
//            mCtx.startActivity(Intent(mCtx, SpoofSocketActivity::class.java))
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

//		FireRef.LIVE_SESSIONS.addChildEventListener(eventListener)

		bind.recycler.setOnScrollChangeListener { _ , _ , _ , _ , _ ->
			val layoutManager = bind.recycler.layoutManager as GridLayoutManager
			val lastItemPosition = layoutManager.findLastVisibleItemPosition()

			val listSize = showList.size

			if (lastItemPosition == listSize - 1 && ! isLoading) {
				isLoading = true
				page ++
				viewModel.getLiveShow(
					selectedTabText.request() ,
					selectedCategory.request() ,
					bind.search.value().ifEmpty { null }?.request() ,
					page.toString().request()
				)
			}
		}

		bind.searchLayout.isEndIconVisible = false

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s : CharSequence? , start : Int , count : Int , after : Int) {}
			override fun onTextChanged(s : CharSequence? , start : Int , before : Int , count : Int) {}
			override fun afterTextChanged(s : Editable?) {
				val query = s?.toString()?.trim() ?: ""
				bind.searchLayout.isEndIconVisible = query.isNotEmpty()

				if (! s.isNullOrEmpty()) {
					bind.loader.isVisible = true
					page = 1
					viewModel.getLiveShow(
						selectedTabText.request() ,
						selectedCategory.request() ,
						s.toString().request() ,
						page.toString().request()
					)
				}
			}
		})
		bind.searchLayout.setEndIconOnClickListener {
			bind.search.setText("")
			bind.searchLayout.isEndIconVisible = false
			hideKeyboard(it)
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			page = 1
			viewModel.getLiveShow(selectedTabText.request() , selectedCategory.request() , page = page.toString().request())
			viewModel.getCategory()
		}

		bind.noInternet.onClick {
			bind.loader.isVisible = false
			bind.noInternet.isVisible = false
			viewModel.getLiveShow(selectedTabText.request() , selectedCategory.request() , page = page.toString().request())
			viewModel.getCategory()
		}

		selectTab(bind.live , true)

		bind.live.setHapticClickListener { selectTab(it as TextView , false) }
		bind.popular.setHapticClickListener { selectTab(it as TextView , false) }
		bind.comingSoon.setHapticClickListener { selectTab(it as TextView , false) }

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
				viewModel.getLiveShow(
					selectedTabText.request() ,
					selectedCategory.request() ,
					page = page.toString().request()
				)
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
								selected = false,
								closeIconVisible = false
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

//					romIdsList.clear()
					streamList.clear()

					mData?.forEach {
						streamList.add(StreamModel(it?.roomId.toString() , it?.rtcToken ?:""))
						romIdsList.add(it?.roomId.toString())
					}

					if (page == 1) {
						showList.clear()
					}

					mData?.forEach {
						if (it?.user != null) {
							showList.add(it)
						}
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

					isLoading = page >= (it.value.totalPage ?: 0)

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

		page = 1

		when (selectedTab) {
			bind.live -> {
				selectedTabText = "live"
				if (! isFirst) {
					viewModel.getLiveShow("live".request() , selectedCategory.request() , page = page.toString().request())
				}
			}

			bind.popular -> {
				selectedTabText = "popular"
				viewModel.getLiveShow("popular".request() , selectedCategory.request() , page = page.toString().request())
			}

			bind.comingSoon -> {
				selectedTabText = "upcoming"
				viewModel.getLiveShow("upcoming".request() , selectedCategory.request() , page = page.toString().request())
			}
		}
	}

//	private var eventListener = object : ChildEventListener {
//		override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//			if (selectedTabText == "live"){
//				viewModel.getLiveShow(selectedTabText.request(), selectedCategory.request(), page = page.toString().request())
//			}
//		}
//
//		override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//			if (selectedTabText == "live") {
//				viewModel.getLiveShow(selectedTabText.request(), selectedCategory.request(), page = page.toString().request())
//			}
//		}
//
//		override fun onChildRemoved(snapshot: DataSnapshot) {
//
//		}
//
//		override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//
//		}
//
//		override fun onCancelled(error: DatabaseError) {
//		}
//
//
//	}

}