package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.gyf.immersionbar.ktx.immersionBar
import com.skydoves.powermenu.PowerMenuItem
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.ActivitySellerProfileBinding
import io.bidswipe.app.databinding.NotificationSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.goToRateSeller
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import kotlin.math.abs

class SellerProfileActivity : BaseActivity() {

    private val bind by bind(ActivitySellerProfileBinding::inflate)
    private val viewModel by viewModels<SellerViewModel>()

    private var sellerId = ""
    private var sellerName = ""
    private var sellerImage = ""

    private var actionList = mutableListOf<PowerMenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            navigationBarDarkIcon(true)
            navigationBarColor(clr.surface)
            supportActionBar(false)
            fitsSystemWindows(false)
            keyboardEnable(true)
        }

        actionList.clear()
        actionList.add(PowerMenuItem(title = "Save Product"))

        val menu = PopupMenu(this, bind.moreIcon)
        menu.menuInflater.inflate(R.menu.profile_action_menu, menu.menu)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                ids.rate -> {
                    startActivity(this.goToRateSeller(sellerId, sellerName, sellerImage))
                }

                ids.block -> {

                }

                ids.reportUser -> {

                }

            }
            return@setOnMenuItemClickListener true
        }

        bind.moreIcon.setOnClickListener {
            menu.show()
        }


        bind.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = bind.appBar.totalScrollRange
            if (abs(verticalOffset) >= totalScrollRange) {
                bind.toolbar.animate().alpha(1f).setDuration(200).withEndAction {
                    bind.toolbar.isVisible = true
                }.start()
            } else {
                bind.toolbar.animate().alpha(0f).setDuration(200).withEndAction {
                    bind.toolbar.visibility = View.GONE
                }.start()
            }
        })

        sellerId = intent?.getStringExtra("userId") ?: ""

        bind.backBtnCard.setOnClickListener {
            finish()
        }

        bind.backButton.setOnClickListener {
            finish()
        }

        val adapter = ViewPagerAdapter(this, "Shop")
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
        viewModel.getProfileByIdShowRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    sellerName = mData?.name ?: ""
                    sellerImage = mData?.profileImage ?: ""

                    bind.name.text = mData?.name
                    bind.name1.text = mData?.name
                    bind.userName.text = mData?.username ?: ""
                    bind.userImage.loadUrl(this, mData?.profileImage.toString())
                    bind.userImage2.loadUrl(this, mData?.profileImage.toString())
                    bind.followers.text = buildSpannedString {
                        bold { append(mData?.followerCount.toString()) }
                        append(" Follower")
                    }

                    bind.following.text = buildSpannedString {
                        bold { append(mData?.followingCount.toString()) }
                        append(" Following")
                    }

                    bind.bio.text = mData?.bio ?: ""

                    if (mData?.isFollowing == true) {
                        bind.follow.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.outline
                            )
                        )
                        bind.follow.setTextColor(ContextCompat.getColor(this, R.color.onSurface))
                        bind.follow.text = "Unfollow"
                    } else {
                        bind.follow.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.primary
                            )
                        )
                        bind.follow.setTextColor(ContextCompat.getColor(this, R.color.background))
                        bind.follow.text = "Follow"
                    }

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
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
            showNotificationSheet()
        }

        viewModel.followUserShowRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    it.value.data

                    viewModel.getProfileById(sellerId.request())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
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

        viewModel.notifyLiveUserRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false

                    it.value.data

                    Alerts.success(this, it.value.message.toString())

//                    viewModel.getProfileById(sellerId.request())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
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

    fun showNotificationSheet() {
        var notificationSheetBind = NotificationSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.notification_sheet,
                null,
                false
            )
        )
        var notificationSheet = Alerts.appBottomSheet(this, true, notificationSheetBind)

        notificationSheetBind.userImage.loadUrl(this, sellerImage)
        notificationSheetBind.userName.text = sellerName
        notificationSheetBind.text.text = "Would you like to notified when ${sellerName} goes live"

        notificationSheetBind.close.setOnClickListener {
            notificationSheet.dismiss()
        }

        notificationSheetBind.noBtn.setOnClickListener {
            notificationSheet.dismiss()
        }

        notificationSheetBind.submit.setOnClickListener {
            bind.loader.isVisible = true
            notificationSheet.dismiss()
            viewModel.notifyLiveUser(sellerId.request())
        }

        notificationSheet.show()
    }

}