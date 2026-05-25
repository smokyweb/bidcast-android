package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentSearchShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.network.response.SearchData
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard

@SuppressLint("NotifyDataSetChanged")
class SearchShowFragment : BaseFragment<DashViewModel, FragmentSearchShowBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSearchShowBinding.inflate(inflater, view, false)

	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var romIdsList = mutableListOf<StreamModel>()
	private lateinit var homeAdapter: HomeAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			when (status) {

				"user" -> {
					startActivity(
						Intent(mCtx, SellerProfileActivity::class.java).putExtra(
							"userId",
							showList[pos]?.userId.toString()
						)
					)
				}

				"viewShow" -> {

					if (showList[pos]?.isLive == true) {
						startActivity(
							Intent(mCtx, ViewLiveShowActivity::class.java).putExtra("position", pos)
								.putParcelableArrayListExtra("roomIdsList", romIdsList as ArrayList)
						)
					}

				}
			}

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}

		homeAdapter = HomeAdapter(showList, mClick)
		(bind.recycler.layoutManager as GridLayoutManager).setSpanCount( if(resources.isTablet()) 3 else 2)
		bind.recycler.adapter = homeAdapter

		bind.search.requestFocus()

		showKeyboard(bind.search)

		bind.loader.isVisible = true
		viewModel.getLiveShow(search = "".request())

		bind.search.addTextChangedListener(object : TextWatcher {
			private var debounce: android.os.Handler? = null

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun afterTextChanged(p0: Editable?) {
				// Basecamp #9922137198: fire both show search + unified search.
				// Debounce 350 ms to avoid hammering the API on every keystroke.
				debounce?.removeCallbacksAndMessages(null)
				debounce = android.os.Handler(android.os.Looper.getMainLooper())
				debounce?.postDelayed({
					val q = p0.toString()
					bind.loader.isVisible = true
					viewModel.getLiveShow(search = q.request())
					if (q.isNotBlank()) viewModel.unifiedSearch(q)
				}, 350)
			}
		})

		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { resource ->
			when (resource) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = resource.value.data

					mData?.forEach {
						romIdsList.add(StreamModel(it?.roomId.toString(), ""))
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
					bind.loader.isVisible = false

					resource.parse(mCtx, TAG, object : AlertClicks {
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

		// Basecamp #9922137198 (Trey 2026-05-20): unified search observer.
		// Phase 1 — shows a count summary at the bottom so Trey can confirm
		// products + users are being returned. A future ticket will add a full
		// sectioned result view (Live Shows / Products / Users) with tappable rows.
		viewModel.unifiedSearchRepo.observe(viewLifecycleOwner) { resource ->
			when (resource) {
				is Resource.Success -> {
					val data: SearchData? = resource.value.data
					val productCount = data?.products?.size ?: 0
					val userCount = data?.users?.size ?: 0
					if (productCount > 0 || userCount > 0) {
						val parts = mutableListOf<String>()
						if (productCount > 0) parts.add("$productCount product${if (productCount == 1) "" else "s"}")
						if (userCount > 0) parts.add("$userCount user${if (userCount == 1) "" else "s"}")
						bind.unifiedSearchSummary.text = "Also found: ${parts.joinToString(" • ")}"
						bind.unifiedSearchSummary.isVisible = true
					} else {
						bind.unifiedSearchSummary.isVisible = false
					}
				}
				else -> bind.unifiedSearchSummary.isVisible = false
			}
		}
	}

}