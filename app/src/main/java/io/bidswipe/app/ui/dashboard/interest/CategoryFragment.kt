package io.bidswipe.app.ui.dashboard.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CategoryAdapter
import io.bidswipe.app.databinding.FragmentCategoryBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.dashboard.DashViewModel

class CategoryFragment : BaseFragment<DashViewModel, FragmentCategoryBinding>() {

    override fun getModel() = DashViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentCategoryBinding.inflate(inflater, view, false)

    private lateinit var categoryAdapter: CategoryAdapter
    private val categoryList = mutableListOf<GetCategoryResponse.Data?>()


    private val categoryClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            categoryList.getOrNull(pos)?.let { category ->
                if (viewModel.selectedCategories.contains(category)) {
                    viewModel.selectedCategories.remove(category)
                } else {
                    viewModel.selectedCategories.add(category)
                }

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryAdapter = CategoryAdapter(categoryList, categoryClicks)
        bind.recyclerView.adapter = categoryAdapter
        
        bind.header.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.nextButton.setOnClickListener {
            if (viewModel.selectedCategories.isEmpty()){
                Toast.makeText(requireContext(), "Please select at least one category", Toast.LENGTH_SHORT).show()
            }
            else{
                findNavController().navigate(R.id.gotoSubcategory)
            }

        }

        loadCategories()
    }

    private fun loadCategories() {
        viewModel.getCategory()
        viewModel.getCategoryRepo.observe(viewLifecycleOwner){
            when (it) {
                is Resource.Success -> {
                    categoryList.clear()
                    categoryList.addAll(it.value.data ?: emptyList())
                    categoryAdapter.notifyDataSetChanged()
                }
                is Resource.Error -> {
                }
                else -> {}
            }
        }
        categoryAdapter.notifyDataSetChanged()
    }
}


