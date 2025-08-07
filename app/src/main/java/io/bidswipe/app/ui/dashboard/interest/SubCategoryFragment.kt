package io.bidswipe.app.ui.dashboard.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SubCategoryAdapter
import io.bidswipe.app.databinding.FragmentSubcategoryBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.dashboard.DashViewModel

class SubCategoryFragment : BaseFragment<DashViewModel, FragmentSubcategoryBinding>() {

    override fun getModel() = DashViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSubcategoryBinding.inflate(inflater, view, false)

    private lateinit var subCategoryAdapter: SubCategoryAdapter
    private val subCategoryList = mutableListOf<GetCategoryResponse.Data?>()
    private val selectedCategories = mutableSetOf<GetCategoryResponse.Data>()

    private val categoryClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
//            subCategoryList.getOrNull(pos)?.let { category ->
//                if (selectedCategories.contains(category)) {
//                    selectedCategories.remove(category)
//                } else {
//                    selectedCategories.add(category)
//                }
//            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subCategoryAdapter = SubCategoryAdapter(subCategoryList, categoryClicks)
        bind.recyclerView.adapter = subCategoryAdapter

        viewModel.selectedCategories.forEachIndexed { index, data ->

            viewModel.getSubCategory(data.id.toString())
            viewModel.getSubCategoryRepo.observe(viewLifecycleOwner){
                when (it) {
                    is Resource.Success -> {
                        bind.loader.isVisible = false

                      if (index==0)  subCategoryList.clear()
                        subCategoryList.addAll(it.value.data ?: emptyList())
                        subCategoryAdapter.notifyDataSetChanged()
                    }
                    is Resource.Error -> {

                    }
                    else -> {}
                }
            }
        }

    }

}