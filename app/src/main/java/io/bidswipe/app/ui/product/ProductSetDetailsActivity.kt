package io.bidswipe.app.ui.product

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.AvailableItemAdapter
import io.bidswipe.app.controller.UnsoldItemsAdapter
import io.bidswipe.app.databinding.ActivityProductSetDetailsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class ProductSetDetailsActivity : BaseActivity() {

    private val bind by bind(ActivityProductSetDetailsBinding::inflate)
    private val viewModel by viewModels<ProductViewModel>()
    private var availableList = mutableListOf<GetSurpriseProductsResponse.Data.Item?>()
    private var soldList = mutableListOf<GetSurpriseProductsResponse.Data.Item.Unit?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)

        val productSetId = intent.getStringExtra("productSetId")

        bind.header.onBackClick {
            finishAfterTransition()
        }

        bind.unsoldHeading.onCloseCLick {
            bind.unsoldExpandView.toggle()
        }

        bind.soldHeading.onCloseCLick {
            bind.soldExpandView.toggle()
        }

        bind.howItWorks.setHtmlFromString(ContextCompat.getString(this, R.string.how_it_works_info), false)
        bind.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab?) {
                bind.viewSwitcher.displayedChild = p0?.position ?: 0
            }


            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
                bind.viewSwitcher.displayedChild = p0?.position ?: 0
            }
        })


        bind.loader.isVisible = true
        viewModel.getSurpriseProductDetail(productSetId)

        viewModel.getSurpriseProductDetailRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val surpriseSet = it.value.data

                    viewModel.getSellerInfo(sellerId = surpriseSet?.userId.toString())

                    if (surpriseSet != null) {
                        bind.desc.text = surpriseSet?.description
                        bind.productName.text = surpriseSet?.name?.asCapital()
                        bind.price.isVisible = surpriseSet?.type == "buy_it_now"
                        bind.price.text = buildString {
                            append("Starting from ")
                            append(surpriseSet?.price?.toString()?.asMoney())
                        }

                        bind.posted.text = buildSpannedString {
                            bold {
                                append("Posted ")
                            }
                            append(Utils.getTimeAgo(surpriseSet?.createdAt ?: "", Const.SERVER_TIME_FORMAT))
                        }

                        val totalQuantity = surpriseSet.items?.sumOf { it?.quantity ?: 0 }
                        val soldQuantity = surpriseSet.items?.sumOf { it?.soldQuantity ?: 0 }
                        bind.stepProgress.max = totalQuantity ?: 0
                        bind.stepProgress.progress = (soldQuantity ?: 0)
                        bind.itemsLeftText.text = buildString {
                            append((totalQuantity ?: 0) - (soldQuantity ?: 0))
                            append("/")
                            append(totalQuantity ?: 0)
                            append(" left")
                        }

                        surpriseSet?.items?.forEach {
                            soldList.addAll(it?.units?.filter { it1 -> it1?.status == "sold" } ?: emptyList())
                            availableList.add(it)
                        }
                        log("SOOLD LIST $soldList")
                        val soldAdapter = UnsoldItemsAdapter(
                            mList = soldList,
                            object : RecyclerClicks {
                                override fun itemClick(pos: Int, status: String?) {}
                            }, false
                        ) { position, price, desc -> }

                        bind.soldItems.adapter = soldAdapter
                        bind.soldHeading.title.text = "Sold (${soldList.size})"

                        val availableAdapter = AvailableItemAdapter(
                            mList = availableList,
                            object : RecyclerClicks {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun itemClick(pos: Int, status: String?) {
                                }
                            })

                        bind.unsoldItems.adapter = availableAdapter
                        bind.unsoldHeading.title.text =
                            "Items (${availableList.sumOf { it?.quantity ?: 0 } - availableList.sumOf { it?.soldQuantity ?: 0 }})"

                    }

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

        viewModel.getSellerInfoRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = it.value.data
                    bind.userName.text = (mData?.sellerDetails?.username ?: "")
                    bind.rating.text = (mData?.ratingAvg ?: 0).toString()
                    bind.review.text = (mData?.review ?: 0).toString()
                    bind.sold.text = (mData?.soldCount ?: 0).toString()
                    bind.shipping.text = (mData?.avgShip ?: 0).toString()
                    bind.userImage.loadUrl(this, mData?.sellerDetails?.profileImage ?: "")

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

private fun TabLayout.addOnTabSelectedListener(listener: Any) {}
