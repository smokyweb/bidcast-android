package io.bidswipe.app.ui.sell

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ManageProductItemAdapter
import io.bidswipe.app.databinding.FragmentManageSurpriseProductsBinding
import io.bidswipe.app.databinding.SurpriseAddProductSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class ManageSurpriseProductsFragment : BaseFragment<DashViewModel, FragmentManageSurpriseProductsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentManageSurpriseProductsBinding.inflate(inflater, view, false)

    private lateinit var productsAdapter: ManageProductItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }
        bind.root.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.priceLayout.isVisible = viewModel.surprise_set_type == "buy_it_now"

        productsAdapter = ManageProductItemAdapter(viewModel.surpriseSetList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                viewModel.surpriseSetList.removeAt(pos)
                productsAdapter.notifyDataSetChanged()
                bind.numberOfProducts.text = "Number Of Products (Max 500) : ${viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 }}"
            }
        })

        bind.products.adapter = productsAdapter

        bind.addNew.setHapticClickListener {
            if (viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 } < 500) {
                addProductSheet()
            } else {
                errorToast("You can not add product quantity more than 500")
            }
        }

        bind.confirm.setHapticClickListener {
            when {
                viewModel.surpriseSetList.isEmpty() -> {
                    Alerts.error(mCtx, "Please add at least one product")
                }

                viewModel.surprise_set_type == "buy_it_now" && bind.price.value().isEmpty() -> {
                    bind.price.requestFocus()
                    Alerts.error(mCtx, "Please enter surprise buy price")
                }

                else -> {
                    viewModel.surpriseBuyPrice = bind.price.value()
                    findNavController().popBackStack()
                }
            }
        }

        bind.numberOfProducts.text = "Number Of Products (Max 500) : ${viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 }}"

    }

    fun addProductSheet() {
        val surpriseAddProductSheetBind = SurpriseAddProductSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.surprise_add_product_sheet,
                null,
                false
            )
        )

        val surpriseAddProductSheet = Alerts.appBottomSheet(mCtx, true, surpriseAddProductSheetBind)

        surpriseAddProductSheetBind.root.setOnClickListener {
            hideKeyboard(it)
        }

        surpriseAddProductSheetBind.close.setHapticClickListener {
            surpriseAddProductSheet.dismiss()
        }

        surpriseAddProductSheetBind.confirm.setHapticClickListener {
            val addedQuantity = viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 }
            when {
                surpriseAddProductSheetBind.name.value().isEmpty() -> {
                    surpriseAddProductSheetBind.name.requestFocus()
                    Alerts.error(mCtx, "Please enter product name")
                }

                surpriseAddProductSheetBind.quantity.value().isEmpty() -> {
                    surpriseAddProductSheetBind.quantity.requestFocus()
                    Alerts.error(mCtx, "Please enter product quantity")
                }

                surpriseAddProductSheetBind.quantity.value().toInt() < 1 -> {
                    surpriseAddProductSheetBind.quantity.requestFocus()
                    Alerts.error(mCtx, "Please enter valid product quantity")
                }

                addedQuantity + surpriseAddProductSheetBind.quantity.value().toInt() > 500 -> {
                    surpriseAddProductSheetBind.quantity.requestFocus()
                    Alerts.error(mCtx, "You can not add product quantity more than 500")
                }

                else -> {
                    viewModel.surpriseSetList.add(
                        SurpriseProductModel(
                            surpriseAddProductSheetBind.name.value(),
                            surpriseAddProductSheetBind.desc.value(),
                            surpriseAddProductSheetBind.quantity.value().toInt(),
                        )
                    )
                    productsAdapter.notifyItemInserted(viewModel.surpriseSetList.size)
                    bind.numberOfProducts.text = "Number Of Products (Max 500): ${viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 }}"
                    if (viewModel.surpriseSetList.sumOf { it?.quantity ?: 0 } == 500) {
                        bind.addNew.isVisible = false
                    }
                    surpriseAddProductSheet.dismiss()
                }
            }
        }

        surpriseAddProductSheet.show()
    }
}