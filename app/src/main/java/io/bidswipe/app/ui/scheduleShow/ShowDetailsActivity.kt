package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ProductAdapter
import io.bidswipe.app.databinding.ActivityScheduleShowBinding
import io.bidswipe.app.databinding.ActivityScheduleShowBinding.inflate
import io.bidswipe.app.databinding.ActivityShowDetailsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.parse
import kotlin.getValue

class ShowDetailsActivity : BaseActivity() {

    private val bind by bind(ActivityShowDetailsBinding::inflate)
    private val viewModel by viewModels<ScheduleShowViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)


        viewModel.showId = intent?.getStringExtra("showId")
        if (!viewModel.showId.isNullOrEmpty()) {
            viewModel.getShowDetails(viewModel.showId.toString())
        }

        bind.header.onBackClick {
            finishAfterTransition()
        }


        viewModel.getShowDetailsRepo.observe(this) {
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

                    val productAdapter = ProductAdapter(viewModel.currentProducts, object : RecyclerClicks {
                        override fun itemClick(pos: Int, status: String?) {

                        }

                    },"show_details")
                    bind.recycler.adapter = productAdapter

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(this, TAG, object : AlertClicks {
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