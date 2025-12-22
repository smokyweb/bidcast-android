package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExampleAdapter
import io.bidswipe.app.controller.TitleAdapter
import io.bidswipe.app.databinding.FragmentShowTitleBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class ShowTitleFragment : BaseFragment<ScheduleShowViewModel, FragmentShowTitleBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentShowTitleBinding.inflate(inflater, view, false)

    private lateinit var titleAdapter: TitleAdapter
    private lateinit var exampleAdapter: ExampleAdapter

    private var titleList = mutableListOf<GetAllTipsResponse.Data.Tip?>()
    private var exampleList = mutableListOf<String?>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from")

        if (from == "dash") {
            viewModel.showId = activity?.intent?.getStringExtra("showId")
            if (!viewModel.showId.isNullOrEmpty()) {
                viewModel.getShowDetails(viewModel.showId.toString())
            }
        }

        bind.header.onBackClick {
            finish()
        }

        bind.layout.setHapticClickListener {
            hideKeyboard(it)
        }

        titleAdapter = TitleAdapter(titleList)
        bind.recycler.adapter = titleAdapter

        exampleAdapter = ExampleAdapter(exampleList)
        bind.exampleRecycler.adapter = exampleAdapter

        bind.continueBtn.setHapticClickListener {

            when {

                bind.showTitle.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter the show title")
                    bind.showTitle.requestFocus()
                    showKeyboard(bind.showTitle)
                }

                else -> {
                    viewModel.showTitle = bind.showTitle.value()
                    hideKeyboard(it)

                    if (from == "tips" || from == "showTutorial") {
                        findNavController().navigate(ids.goToSelectCategoryFragment)
                    } else {
                        findNavController().navigate(ids.goToSelectShowTimeFragment)
                    }
                }
            }

        }

        bind.loader.isVisible = true

        viewModel.getAllTips("title".request())
        viewModel.getAllTipsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    titleList.clear()
                    exampleList.clear()

                    mData?.tips?.forEach { tip ->
                        titleList.add(tip)
                    }

                    mData?.example?.forEach { example ->
                        exampleList.add(example)
                    }

                    titleAdapter.notifyDataSetChanged()

                    exampleAdapter.notifyDataSetChanged()

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

        viewModel.getShowDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    viewModel.showTitle = mData?.title ?: ""
                    bind.showTitle.setText(viewModel.showTitle)

                    viewModel.time = Utils.getFormattedDateTime("HH:mm:ss", "HH:mm", mData?.time ?: "") ?: ""
                    viewModel.date = mData?.date ?: ""

                    viewModel.categoryId = mData?.categoryId.toString()
                    viewModel.auctionId = mData?.auctionTypeId.toString()

                    viewModel.repeatMode = if (mData?.isRepeat ?: false) "0" else "1"
                    viewModel.repeatType = mData?.repeatValue ?: ""

                    viewModel.explicitContent = if (mData?.isExplicit ?: false) "1" else "0"
                    viewModel.primaryLanguage = mData?.language ?: ""

                    viewModel.discoverability = mData?.showDiscoverability ?: ""

                    viewModel.thumbnail=mData?.thumbnail?.first()?:""

                    mData?.products?.forEach { data ->
                        if(data!=null) {
                            val product =
                                LiveShowModel.Product(
                                    LiveShowModel.Category(
                                        data.category?.id,
                                        data.category?.image ?: "",
                                        data.category?.name ?: "",
                                        data.category?.thumbnail
                                    ),
                                    data.id.toString(),
                                    data.images?.get(0),
                                    data.status,
                                    data.title,
                                    data.pricing.toString(),
                                    data.quantity.toString(),
                                    selected = true
                                )

                            if (!viewModel.currentProducts.any { existing -> existing.id == product.id }) {
                                viewModel.currentProducts.add(product)
                            }
                        }
                    }

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