package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductAdapter
import io.bidswipe.app.databinding.FragmentAddProductBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import okhttp3.MultipartBody
import java.io.File

@SuppressLint("NotifyDataSetChanged")
class AddProductFragment : BaseFragment<ScheduleShowViewModel,FragmentAddProductBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentAddProductBinding.inflate(inflater,view,false)

    private lateinit var productAdapter :ProductAdapter
    private var productList = mutableListOf<GetProductsResponse.Data?>()
    private var imagePartList = mutableListOf<MultipartBody.Part?>()

    private var from = ""

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            productList[pos]?.selected = true

            productAdapter.notifyItemChanged(pos)

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        from = activity?.intent?.getStringExtra("from") ?:""

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        productAdapter = ProductAdapter(productList,mClick)

        bind.recycler.adapter = productAdapter

        bind.finishBtn.setOnClickListener {

            bind.loader.isVisible = true
            var imagePartList = mutableListOf<MultipartBody.Part?>()
            var productIdList = mutableListOf<Int>()

            productList.forEach {

                if (it?.selected == true){

                    productIdList.add(it.id?.toInt() ?:0 )
                }

            }

            if (productIdList.isEmpty()){

                Alerts.error(mCtx,"Please select product")

            }

            imagePartList.add(Utils.imagePart(
                "thumbnail[]",
                viewModel.thumbnail,
                File(viewModel.thumbnail)
            ))

            if (from == "showTutorial"){

                val data = Intent()
                data.putExtra("title" , TutorialShowModel(viewModel.showTitle,viewModel.categoryId,viewModel.auctionId,viewModel.thumbnail,productIdList.joinToString(",")))
//                data.putExtra("categoryId" , )
//                data.putExtra("auctionTypeId" , )
//                data.putExtra("thumbnails" , )
//                data.putExtra("productIds" , )
                activity?.setResult(Activity.RESULT_OK, data)
                finish()



            }else{
                viewModel.storeScheduleShow(
                    title = viewModel.showTitle.request(),
                    date = viewModel.date.request(),
                    time = viewModel.time.request(),
                    categoryId = viewModel.categoryId.request(),
                    auctionTypeId = viewModel.auctionId.request(),
                    thumbnails = imagePartList,
                    productIds = productIdList
                )
            }






        }

        bind.loader.isVisible = true

        viewModel.getUserProducts("".request())

        viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    productList.clear()

                    mData?.forEach {
                        productList.add(it)
                    }

                    productAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

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

        viewModel.storeScheduleShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                   finish()


                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

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