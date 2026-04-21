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
				subCategoryList[pos]?.subcategories?.get(status.toInt())?.isSelected =
					!(subCategoryList[pos]?.subcategories?.get(status.toInt())?.isSelected ?: false)
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

						// QA-FIX (MC task cmo7iad4500bsfi158cbti9q1): filter out categories that
						// have NO subcategories — they should not appear as options here.
						val allData = it.value.data ?: emptyList()
						val categoriesWithSubcategories = allData.filter { category ->
							!category?.subcategories.isNullOrEmpty()
						}

						// QA-FIX (MC task cmo7iad9e00bufi15cq2d1qz1): if NONE of the selected
						// categories have subcategories, skip this screen entirely and go directly
						// to the dashboard (or back to account settings).
						if (categoriesWithSubcategories.isEmpty()) {
							// Submit with just the selected categories and no subcategories
							val selectedCategoryIdsFinal = viewModel.selectedCategories.mapNotNull { it.id }
							viewModel.userFavorite(
								categoryIds = selectedCategoryIdsFinal,
								subcategoriesIds = emptyList()
							)
							val fromAccountSkip = arguments?.getBoolean("fromAccount", false)
							viewModel.userFavoriteRepo.observe(viewLifecycleOwner) { res ->
								when (res) {
									is Resource.Success -> {
										successToast("Saved successfully")
										App.getCategories()
										if (fromAccountSkip == true) {
											requireActivity().finish()
										} else {
											startActivity(mCtx.toDash())
											requireActivity().finish()
										}
									}
									else -> {}
								}
							}
							return@observe
						}

						val sortedList = categoriesWithSubcategories
							.sortedByDescending { category ->
								!category?.subcategories.isNullOrEmpty()
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