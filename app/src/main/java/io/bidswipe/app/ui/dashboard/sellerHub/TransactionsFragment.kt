package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.TransactionHistoryAdapter
import io.bidswipe.app.databinding.FragmentTransactionsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

class TransactionsFragment : BaseFragment<SellerHubViewModel , FragmentTransactionsBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentTransactionsBinding.inflate(inflater , view , false)

	private var categoriesList = mutableListOf<String>()

	private var transactionList = mutableListOf<GetTransactionsHistoryResponse.Data?>()

	private lateinit var transactionHistoryAdapter : TransactionHistoryAdapter
	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		transactionHistoryAdapter = TransactionHistoryAdapter(transactionList , mClick)

		bind.transaction.adapter = transactionHistoryAdapter

		categoriesList = mutableListOf("All" , "Processing" , "Completed" , "Withdrawal")
		categoriesList.forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx ,
					text = it ,
					selected = false
				)
			)
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}

		bind.transaction.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView : RecyclerView , dx : Int , dy : Int) {
				super.onScrolled(recyclerView , dx , dy)
				val layoutManager = bind.transaction.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (transactionList.size - 1)) {
					if (! isLoading) {
						isLoading = true
						page ++
						bind.bottomLoader.isVisible = true
						viewModel.getTransactionsHistory(page.toString().request())
					}
				}
			}
		})

		bind.loader.isVisible = true

		viewModel.getTransactionsHistory("1".request())

		viewModel.getTransactionsHistoryRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.bottomLoader.isVisible = false
					bind.loader.isVisible = false
					val mData = it.value.data

					log(mData.toString())

					if (page == 1) {
						transactionList.clear()
					}

					if (mData != null) {
						transactionList.addAll(mData)
					}

					log("Transactions : ${transactionList}")

					if (transactionList.isEmpty()) {
						bind.noData.isVisible = true
						bind.transaction.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.transaction.isVisible = true
					}

					isLoading = page >= (it.value.totalPage ?: 0)

					transactionHistoryAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
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

}