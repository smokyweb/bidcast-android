package io.bidswipe.app.ui.dashboard.sellerProfile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShopAdapter
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentShopBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.product.ProductDetailsActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe

class ShopFragment : BaseFragment<SellerViewModel,FragmentShopBinding>() {
    override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentShopBinding.inflate(inflater,view,false)

    private var productList = mutableListOf<GetProductsResponse.Data?>()

    private lateinit var shopAdapter: ShopAdapter

    private val mClick = object : RecyclerClicks{
      
        override fun itemClick(pos: Int, status: String?) {
            startActivity(Intent(mCtx, ProductDetailsActivity::class.java).putExtra("productId",productList[pos]?.id.toString()))
            
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repeat(5){
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = "For You",
                    selected = false
                )
            )
        }



        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }

        shopAdapter = ShopAdapter(productList,mClick)

        bind.recycler.adapter = shopAdapter

        viewModel.getUserProducts()

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    productList.clear()

                    mData?.forEach {
                        productList.add(it)
                    }

                    shopAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
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