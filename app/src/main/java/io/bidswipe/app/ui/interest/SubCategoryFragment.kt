package io.bidswipe.app.ui.interest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SubCategoryRecyclerAdapter
import io.bidswipe.app.databinding.FragmentSubcategoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toDash

class SubCategoryFragment : BaseFragment<DashViewModel, FragmentSubcategoryBinding>() {

	override fun getModel() = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSubcategoryBinding.inflate(inflater, view, false)

	private lateinit var subCategoryRecyclerAdapter: SubCategoryRecyclerAdapter
	private val subCategoryList = mutableListOf<GetSubCategoriesResponse.Data?>()

	private val categoryClicks = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			if (status != null) {
				val subCategoryIndex = status.toIntOrNull() ?: return
				val subCategory = subCategoryList[pos]?.subcategories?.getOrNull(subCategoryIndex) ?: return
				val updatedSelection = !(subCategory.isSelected ?: false)
				subCategory.isSelected = updatedSelection
				subCategory.id?.let { id ->
					if (updatedSelection) {
						viewModel.selectedSubCategoryIds.add(id)
					} else {
						viewModel.selectedSubCategoryIds.remove(id)
					}
				}
				subCategoryRecyclerAdapter.notifyItemChanged(pos)
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		subCategoryRecyclerAdapter = SubCategoryRecyclerAdapter(subCategoryList, categoryClicks)
		bind.recyclerView.adapter = subCategoryRecyclerAdapter

		bind.header.setHapticClickListener {
			findNavController().popBackStack()
		}

		bind.confirmButton.setHapticClickListener {
			val selectedSubCategories = subCategoryList.filter { it?.subcategories?.filter { it1 -> it1?.isSelected == true }?.isNotEmpty() == true }.toList()

			val selectedCategoryIds = viewModel.selectedCategories.mapNotNull { it.id }
			val selectedSubCategoryIds = mutableListOf<Int>()
			selectedSubCategories.forEach {
				it?.subcategories?.filter { it1 -> it1?.isSelected == true }
					?.map { it?.id?.let { element -> selectedSubCategoryIds.add(element) } }
			}

			viewModel.userFavorite(
				categoryIds = selectedCategoryIds,
				subcategoriesIds = selectedSubCategoryIds
			)

			val fromAccount = arguments?.getBoolean("fromAccount", false)
			viewModel.userFavoriteRepo.observe(viewLifecycleOwner) {
				when (it) {
					is Resource.Success -> {
						successToast("Saved successfully")

						App.getCategories()
						if (fromAccount == true) {
							requireActivity().finish()
						} else {
							startActivity(mCtx.toDash())
							requireActivity().finish()
						}
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

		val selectedCategoryIds = viewModel.selectedCategories.mapNotNull { it.id }
		if (selectedCategoryIds.isNotEmpty()) {
			bind.loader.isVisible = true
			viewModel.getSubCategories(selectedCategoryIds)

			viewModel.getSubCategoriesRepo.observe(viewLifecycleOwner) {
				when (it) {
					is Resource.Success -> {
						bind.loader.isVisible = false
						subCategoryList.clear()

						val sortedList = (it.value.data ?: emptyList())
							.sortedByDescending { category ->
								!category?.subcategories.isNullOrEmpty()
							}

						if (viewModel.selectedSubCategoryIds.isEmpty()) {
							sortedList.forEach { category ->
								category?.subcategories?.forEach { subCategory ->
									if (subCategory?.isSelected == true) {
										subCategory.id?.let { selectedId ->
											viewModel.selectedSubCategoryIds.add(selectedId)
										}
									}
								}
							}
						}

						sortedList.forEach { category ->
							category?.subcategories?.forEach { subCategory ->
								subCategory?.id?.let { id ->
									subCategory.isSelected = viewModel.selectedSubCategoryIds.contains(id)
								}
							}
						}

						subCategoryList.addAll(sortedList)
						subCategoryRecyclerAdapter.notifyDataSetChanged()
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

}