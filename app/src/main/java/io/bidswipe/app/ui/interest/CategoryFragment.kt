package io.bidswipe.app.ui.interest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryAdapter
import io.bidswipe.app.databinding.FragmentCategoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toDash

class CategoryFragment : BaseFragment<DashViewModel, FragmentCategoryBinding>() {

	override fun getModel() = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentCategoryBinding.inflate(inflater, view, false)

	private lateinit var categoryAdapter: CategoryAdapter
	private val categoryList = mutableListOf<GetCategoryResponse.Data?>()

	/** True only while handling Continue → prefetch subcategories for the selected categories. */
	private var awaitingSubCategoriesForNext = false

	/** True only after calling [DashViewModel.userFavorite] from the no-subcategories shortcut. */
	private var awaitingUserFavoriteResult = false

	private val categoryClicks = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			categoryList[pos]?.isSelected = !(categoryList[pos]?.isSelected ?: false)
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

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		categoryAdapter = CategoryAdapter(categoryList, categoryClicks)
		bind.recyclerView.adapter = categoryAdapter

		val isFirstTimeLogin = activity?.intent?.getBooleanExtra("isFirstTimeLogin", false) ?: false

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
				awaitingSubCategoriesForNext = true
				bind.loader.visibility = View.VISIBLE
				viewModel.getSubCategories(viewModel.selectedCategories.mapNotNull { it.id })
			}

		}

		viewModel.getSubCategoriesRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					if (!awaitingSubCategoriesForNext) return@observe
					awaitingSubCategoriesForNext = false
					bind.loader.visibility = View.GONE
					viewModel.getSubCategoriesRepo.value = null

					val bundle = Bundle().apply {
						putBoolean(
							"fromAccount",
							requireActivity().intent.getBooleanExtra("fromAccount", false)
						)
					}
					if (hasAnySubcategories(it.value)) {
						findNavController().navigate(R.id.gotoSubcategory, bundle)
					} else {
						awaitingUserFavoriteResult = true
						viewModel.userFavorite(
							categoryIds = viewModel.selectedCategories.mapNotNull { c -> c.id },
							subcategoriesIds = emptyList()
						)
					}
				}

				is Resource.Error -> {
					if (!awaitingSubCategoriesForNext) return@observe
					awaitingSubCategoriesForNext = false
					bind.loader.visibility = View.GONE
					viewModel.getSubCategoriesRepo.value = null
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

		viewModel.userFavoriteRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					if (!awaitingUserFavoriteResult) return@observe
					awaitingUserFavoriteResult = false
					viewModel.userFavoriteRepo.value = null
					successToast("Saved successfully")
					App.getCategories()
					val fromAccount = requireActivity().intent.getBooleanExtra("fromAccount", false)
					if (fromAccount) {
						requireActivity().finish()
					} else {
						startActivity(mCtx.toDash())
						finish()
					}
				}

				is Resource.Error -> {
					if (!awaitingUserFavoriteResult) return@observe
					awaitingUserFavoriteResult = false
					viewModel.userFavoriteRepo.value = null
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

		loadCategories()
	}

	private fun hasAnySubcategories(response: GetSubCategoriesResponse): Boolean {
		return response.data.orEmpty().any { category ->
			category?.subcategories?.isNotEmpty() == true
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun loadCategories() {
		if (viewModel.isInterestCategoriesLoaded && viewModel.interestCategoriesCache.isNotEmpty()) {
			bind.loader.visibility = View.GONE
			categoryList.clear()
			categoryList.addAll(viewModel.interestCategoriesCache)
			applySelectedCategoriesOnList()
			categoryAdapter.notifyDataSetChanged()
			return
		}

		bind.loader.visibility = View.VISIBLE
		viewModel.getCategory()
		viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
			bind.loader.visibility = View.GONE
			when (it) {
				is Resource.Success -> {
					viewModel.getCategoryRepo.value = null
					viewModel.isInterestCategoriesLoaded = true
					viewModel.interestCategoriesCache.clear()
					viewModel.interestCategoriesCache.addAll(it.value.data ?: emptyList())

					categoryList.clear()
					categoryList.addAll(viewModel.interestCategoriesCache)
					if (viewModel.selectedCategories.isEmpty()) {
						categoryList.forEach { category ->
							if (category?.isSelected == true) {
								viewModel.selectedCategories.add(category)
							}
						}
					}
					applySelectedCategoriesOnList()

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

	private fun applySelectedCategoriesOnList() {
		val selectedIds = viewModel.selectedCategories.mapNotNull { it.id }.toSet()
		categoryList.forEach { category ->
			category?.isSelected = category?.id?.let { selectedIds.contains(it) } == true
		}
	}
}


