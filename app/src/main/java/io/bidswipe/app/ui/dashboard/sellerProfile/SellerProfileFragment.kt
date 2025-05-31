package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentSellerProfileBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SellerProfileFragment : BaseFragment<SellerViewModel,FragmentSellerProfileBinding>() {
    override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellerProfileBinding.inflate(inflater,view,false)

    private var sellerId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sellerId = activity?.intent?.getStringExtra("userId") ?:""

        bind.backBtnCard.setOnClickListener {
            finish()
        }

        val adapter = ViewPagerAdapter(requireActivity(),"Shop")
        bind.pager.adapter = adapter

        TabLayoutMediator(bind.tabLayout, bind.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Shop"
                1 -> "Shows"
                2 -> "Reviews"
                3 -> "Clips"
                else -> ""
            }
        }.attach()

        bind.loader.isVisible = true
        viewModel.getProfileById(sellerId.request())
        viewModel.getProfileByIdShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    bind.name.text = mData?.name
                    bind.userName.text = mData?.username ?:""
                    bind.userImage.loadUrl(mCtx,mData?.profileImage.toString())
                    bind.followers.text = buildSpannedString {
                        append(mData?.followerCount.toString())
                        append(" Follower")
                    }

                    bind.following.text = buildSpannedString {
                        append(mData?.followingCount.toString())
                        append(" Following")
                    }

                    bind.bio.text = mData?.bio ?:""

                    if (mData?.isFollowing == true){
                        bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx,R.color.outline))
                        bind.follow.setTextColor(ContextCompat.getColor(mCtx,R.color.onSurface))
                        bind.follow.text = "Unfollow"
                    }else{
                        bind.follow.setBackgroundColor(ContextCompat.getColor(mCtx,R.color.primary))
                        bind.follow.setTextColor(ContextCompat.getColor(mCtx,R.color.background))
                        bind.follow.text = "Follow"
                    }

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

        bind.follow.setOnClickListener {
            bind.loader.isVisible = true
            viewModel.followUser(sellerId.request())
        }

        bind.notificationIcon.setOnClickListener {

            bind.loader.isVisible = true

            viewModel.notifyLiveUser(sellerId.request())

        }

        viewModel.followUserShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    viewModel.getProfileById(sellerId.request())

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

        viewModel.notifyLiveUserRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false

                    val mData = it.value.data

                    Alerts.success(mCtx,it.value.message.toString())

//                    viewModel.getProfileById(sellerId.request())

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