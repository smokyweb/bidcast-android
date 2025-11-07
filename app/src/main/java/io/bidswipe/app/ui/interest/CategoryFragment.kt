package io.bidswipe.app.ui.interest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryAdapter
import io.bidswipe.app.databinding.FragmentCategoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toDash

class CategoryFragment : BaseFragment<DashViewModel , FragmentCategoryBinding>() {

	override fun getModel() = DashViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentCategoryBinding.inflate(inflater , view , false)

	private lateinit var categoryAdapter : CategoryAdapter
	private val categoryList = mutableListOf<GetCategoryResponse.Data?>()

	private val categoryClicks = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
			categoryList[pos]?.isSelected = ! (categoryList[pos]?.isSelected ?: false)
			categoryList.getOrNull(pos)?.let { category ->
				if (viewModel.selectedCategories.contains(category)) {
					viewModel.selectedCategories.remove(category)
				} else {
					viewModel.selectedCategories.add(category)
				}

			}
			categoryAdapter.notifyItemChanged(pos)
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		categoryAdapter = CategoryAdapter(categoryList , categoryClicks)
		bind.recyclerView.adapter = categoryAdapter

		val isFirstTimeLogin = activity?.intent?.getBooleanExtra("isFirstTimeLogin" , false) ?: false

        bind.header.setHapticClickListener {
			when {
				isFirstTimeLogin -> {
					startActivity(mCtx.toDash())
					requireActivity().finish()
				}

				else -> {
					requireActivity().finish()
				}
			}
		}

        bind.nextButton.setHapticClickListener {
			if (viewModel.selectedCategories.isEmpty()) {
				errorToast("Please select at least one category")
			} else {
				val bundle = Bundle().apply {
					putBoolean(
						"fromAccount" ,
						requireActivity().intent.getBooleanExtra("fromAccount" , false)
					)
				}
				findNavController().navigate(R.id.gotoSubcategory , bundle)
			}

		}

		loadCategories()
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun loadCategories() {
		bind.loader.visibility = View.VISIBLE
		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			bind.loader.visibility = View.GONE
			when (it) {
				is Resource.Success -> {
					categoryList.clear()
					categoryList.addAll(it.value.data ?: emptyList())
					viewModel.selectedCategories.clear()
					val preSelectedIndexes = mutableListOf<Int>()
					categoryList.forEachIndexed { index , category ->
						if (category?.isSelected == true) {
							viewModel.selectedCategories.add(category)
							preSelectedIndexes.add(index)
						}
					}

					categoryAdapter.notifyDataSetChanged()
				}

				is Resource.Error -> {
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


