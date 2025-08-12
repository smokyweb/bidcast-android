package io.bidswipe.app.ui.dashboard.interest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SubCategoryRecyclerAdapter
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

    private lateinit var subCategoryRecyclerAdapter: SubCategoryRecyclerAdapter
    private val subCategoryList = mutableListOf<GetSubCategoriesResponse.Data?>()
    private val selectedSubCategories = mutableListOf<GetSubCategoriesResponse.Data.Subcategory>()


    private val categoryClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            if (status == null) return
            val item = subCategoryList[pos]?.subcategories?.get(status.toInt())
            Log.d(TAG, "itemClick: ")
            item?.let {
                if (selectedSubCategories.contains(it)) {
                    selectedSubCategories.remove(it)
                } else {
                    selectedSubCategories.add(it)
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
                            Toast.makeText(mCtx, "Saved successfully", Toast.LENGTH_SHORT).show()
                            startActivity(mCtx.toDash())
                            requireActivity().finish()
                        }
                        is Resource.Error -> {
                            Toast.makeText(mCtx, "Failed to save favorites", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            } else {
                Toast.makeText(
                    mCtx,
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

                        val filteredList = it.value.data?.filter { category ->
                            !category?.subcategories.isNullOrEmpty()
                        } ?: emptyList()

                        subCategoryList.addAll(filteredList)
                        subCategoryRecyclerAdapter.notifyDataSetChanged()
                    }
                    is Resource.Error -> {
                        bind.loader.isVisible = false
                        Toast.makeText(mCtx, "Failed to load subcategories", Toast.LENGTH_SHORT).show()

                    }
                    else -> {}
                }
            }
        }

    }

}