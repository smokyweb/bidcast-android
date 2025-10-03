package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
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
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard

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

		bind.recycler.adapter = homeAdapter

		bind.search.requestFocus()

		showKeyboard(bind.search)

		bind.loader.isVisible = true
		viewModel.getLiveShow(search = "".request())

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(
				p0: CharSequence?,
				p1: Int,
				p2: Int,
				p3: Int,
			) {
			}

			override fun onTextChanged(
				p0: CharSequence?,
				p1: Int,
				p2: Int,
				p3: Int,
			) {
			}

			override fun afterTextChanged(p0: Editable?) {

				bind.loader.isVisible = true
				viewModel.getLiveShow(search = p0.toString().request())


			}
		})

		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

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


	}

}