package io.bidswipe.app.ui.dashboard.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SubCategoryAdapter
import io.bidswipe.app.databinding.FragmentSubcategoryBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.toDash

class SubCategoryFragment : BaseFragment<DashViewModel, FragmentSubcategoryBinding>() {

    override fun getModel() = DashViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSubcategoryBinding.inflate(inflater, view, false)

    private lateinit var subCategoryAdapter: SubCategoryAdapter
    private val subCategoryList = mutableListOf<GetSubCategoriesResponse.Data?>()
    private val selectedSubCategories = mutableListOf<GetSubCategoriesResponse.Data>()


    private val categoryClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            val item = subCategoryList[pos]
            item?.let {
                if (selectedSubCategories.contains(it)) {
                    selectedSubCategories.remove(it)
                } else {
                    selectedSubCategories.add(it)
                }
                subCategoryAdapter.notifyItemChanged(pos)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subCategoryAdapter = SubCategoryAdapter(subCategoryList, categoryClicks)
        bind.recyclerView.adapter = subCategoryAdapter

        bind.header.setOnClickListener {
            findNavController().popBackStack()
        }
        bind.firstButton.setOnClickListener {
            if (selectedSubCategories.isNotEmpty()) {
                val selectedCategoryIds = viewModel.selectedCategories.mapNotNull { it.id }
                val selectedSubCategoryIds = selectedSubCategories.mapNotNull { it.id }

                viewModel.userFavorite(
                    categoryIds = selectedCategoryIds,
                    subcategoriesIds = selectedSubCategoryIds
                )
                viewModel.userFavoriteRepo.observe(viewLifecycleOwner) {
                    when (it) {
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), "Saved successfully", Toast.LENGTH_SHORT).show()
                            startActivity(requireContext().toDash())
                            requireActivity().finish()
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), "Failed to save favorites", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one category",
                    Toast.LENGTH_SHORT
                ).show()
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
                        subCategoryList.addAll(it.value.data ?: emptyList())
                        subCategoryAdapter.notifyDataSetChanged()
                    }
                    is Resource.Error -> {
                        bind.loader.isVisible = false
                        Toast.makeText(requireContext(), "Failed to load subcategories", Toast.LENGTH_SHORT).show()

                    }
                    else -> {}
                }
            }
        }

    }

}