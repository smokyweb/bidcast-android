package io.bidswipe.app.ui.sellerProfile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.skydoves.powermenu.PowerMenuItem
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.ActivitySellerProfileBinding
import io.bidswipe.app.databinding.AppReportViewBinding
import io.bidswipe.app.databinding.NotificationSheetBinding
import io.bidswipe.app.databinding.SendTipSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetReportCategoriesResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.goToRateSeller
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.share.Seller
import io.bidswipe.app.utils.share.ShareHelper
import kotlin.math.abs

@SuppressLint("InflateParams", "SetTextI18n")
class SellerProfileActivity : BaseActivity() {

    private val bind by bind(ActivitySellerProfileBinding::inflate)
    private val viewModel by viewModels<SellerViewModel>()

    var sellerId = ""
    private var sellerName = ""
    private var sellerImage = ""

    private var actionList = mutableListOf<PowerMenuItem>()

    @SuppressLint("SetTextI18n")
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

        bind.pager.setPadding(0, 0, 0, navigationBarHeight)

        actionList.clear()
        actionList.add(PowerMenuItem(title = "Save Product"))

        val wrapper = ContextThemeWrapper(this, R.style.popupMenuStyle)
        val menu = PopupMenu(
            wrapper,
            bind.moreIcon
        )

        menu.menuInflater.inflate(R.menu.profile_action_menu, menu.menu)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                ids.rate -> {
                    startActivity(this.goToRateSeller(sellerId, sellerName, sellerImage))
                }

                ids.block -> {
                    showBlockConfirmation()
                }

                ids.reportUser -> {
                    bind.loader.isVisible = true
                    viewModel.getReportCategories()
                }

            }
            return@setOnMenuItemClickListener true
        }

        bind.shareIcon.setHapticClickListener {
            shareSellerProfile()
        }

        bind.moreIcon.setHapticClickListener {
            menu.show()
        }

        val menu1 = PopupMenu(
            wrapper,
            bind.moreIcon1
        )

        menu1.menuInflater.inflate(R.menu.profile_action_menu, menu1.menu)
        menu1.setOnMenuItemClickListener {
            when (it.itemId) {
                ids.rate -> {
                    startActivity(this.goToRateSeller(sellerId, sellerName, sellerImage))
                }

                ids.block -> {
                    showBlockConfirmation()
                }

                ids.reportUser -> {
                    bind.loader.isVisible = true
                    viewModel.getReportCategories()
                }
            }
            return@setOnMenuItemClickListener true
        }

        bind.moreIcon1.setHapticClickListener {
            menu1.show()
        }

        bind.share1.setHapticClickListener {
            shareSellerProfile()
        }

        bind.notificationIcon1.setHapticClickListener {
            showNotificationSheet()
        }

        bind.appBar.addOnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = bind.appBar.totalScrollRange
            if (abs(verticalOffset) >= totalScrollRange) {
                bind.toolbar.animate().alpha(1f).setDuration(200).withStartAction {
                    bind.toolbar.isVisible = true
                }.start()
            } else {
                bind.toolbar.animate().alpha(0f).setDuration(200).withEndAction {
                    bind.toolbar.isVisible = false
                }.start()
            }
        }

        sellerId = intent?.getStringExtra("sellerId") ?: ""

        bind.backBtnCard.setHapticClickListener {
            finish()
        }

        bind.toolbar.setNavigationOnClickListener {
            finish()
        }

        bind.messageSeller.setHapticClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("id", sellerId)
                putExtra("name", sellerName)
                putExtra("image", sellerImage)
            }
            startActivity(intent)
        }

        bind.sendTip.setHapticClickListener {
            sendTipSheet()
        }

        val adapter = ViewPagerAdapter(this, "Shop")
        bind.pager.adapter = adapter
        bind.pager.isUserInputEnabled = false

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

                    sellerName = mData?.name?.asCapital() ?: ""
                    sellerImage = mData?.profileImage ?: ""

                    bind.name.text = mData?.name?.asCapital()
                    bind.name1.text = mData?.name?.asCapital()
                    bind.userName.text = mData?.username ?: ""
                    bind.userName1.text = mData?.username ?: ""
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

                    bind.bio.isVisible = mData?.bio?.isEmpty() == true
                    bind.bio.text = mData?.bio ?: ""

                    if (mData?.isFollowing == true) {
                        bind.follow.setBackgroundColor(
                            ContextCompat.getColor(
                                this, R.color.outline
                            )
                        )
                        bind.follow.setTextColor(ContextCompat.getColor(this, R.color.onSurface))
                        bind.follow.text = "Unfollow"
                    } else {
                        bind.follow.setBackgroundColor(
                            ContextCompat.getColor(
                                this, R.color.primary
                            )
                        )
                        bind.follow.setTextColor(ContextCompat.getColor(this, R.color.background))
                        bind.follow.text = "Follow"
                    }

                    if (mData?.preferences?.directMessage == false)
                        bind.messageSeller.visibility = View.INVISIBLE

//					if (mData?.preferences?.receiveGifts == false)
//						bind.sendTip.visibility = View.INVISIBLE

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

        bind.follow.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.followUser(sellerId.request())
        }

        bind.notificationIcon.setHapticClickListener {
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

        viewModel.notifyLiveUserRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false

                    it.value.data

                    Alerts.success(this, it.value.message.toString())

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

        viewModel.sendTipAmountRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    Alerts.success(this, "Tip sent successfully")
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

        viewModel.getReportCategoriesRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    val mData = it.value.data

                    if (mData != null) {
                        reportUserDialog(mData)
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

        viewModel.reportSellerRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false

                    Alerts.success(this, "Report sent successfully")

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

    fun showNotificationSheet() {
        val notificationSheetBind = NotificationSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.notification_sheet, null, false
            )
        )
        val notificationSheet = Alerts.appBottomSheet(this, true, notificationSheetBind)

        notificationSheetBind.userImage.loadUrl(this, sellerImage)
        notificationSheetBind.userName.text = sellerName.asCapital()
        notificationSheetBind.text.text = "Would you like to notified when $sellerName goes live?"

        notificationSheetBind.close.setHapticClickListener {
            notificationSheet.dismiss()
        }

        notificationSheetBind.noBtn.setHapticClickListener {
            notificationSheet.dismiss()
        }

        notificationSheetBind.submit.setHapticClickListener {
            bind.loader.isVisible = true
            notificationSheet.dismiss()
            viewModel.notifyLiveUser(sellerId.request())
        }

        notificationSheet.show()
    }

    private fun shareSellerProfile() {
        val shareText = buildString {
            append("Check out this seller: @${bind.userName.text}")
            append(" ⭐\nExplore their collection here:\n")
            append("${Const.BASE_URL}/seller/$sellerId")
        }

        val seller = Seller(
            sellerId,
            sellerImage,
            sellerName
        )
        ShareHelper.openShareSheet(
            this.supportFragmentManager,
            imageUrl = sellerImage,
            text = sellerName,
            sellerInfo = seller,
            shareText = shareText,
            type = "seller"
        )
    }

    private fun showBlockConfirmation() {
        AppBottomSheet(
            this,
            R.drawable.ic_block,
            "Block Seller",
            "Are you sure you want to block this seller?",
            primaryBtnText = "Block",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            iconPadding = 16,
            alertType = AlertType.WARNING,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    blockUser()
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }).show()

    }

    fun reportUserDialog(data: List<GetReportCategoriesResponse.Data?>) {
        val mBind = AppReportViewBinding.bind(
            layoutInflater.inflate(
                R.layout.app_report_view, null, false
            )
        )
        val sheet = Alerts.appBottomSheet(this, true, mBind)

        val reportCategoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            data.map { it?.name?.asCapital() }
        )

        mBind.reason.setAdapter(reportCategoryAdapter)
        val reportDrawable = ContextCompat.getDrawable(this, R.drawable.card_8)
        mBind.reason.setDropDownBackgroundDrawable(reportDrawable)

        mBind.reason.setOnItemClickListener { _, _, position, _ ->
            val selectedProcessingCategory = data[position]
            log("Selected processing category: $selectedProcessingCategory")
        }

        mBind.reason.setHapticClickListener {
            mBind.reason.showDropDown()
        }

        mBind.submitReport.setHapticClickListener {

            if (mBind.reason.text.toString().isEmpty()) {
                Alerts.error(this, "Please select a reason")
                return@setHapticClickListener
            }

            if (mBind.tellMore.text.toString().isEmpty()) {
                Alerts.error(this, "Please tell us more")
                return@setHapticClickListener
            }

            val reasonId = data.find { it?.name?.asCapital() == mBind.reason.text.toString() }?.id.toString()


            bind.loader.isVisible = true
            viewModel.reportSeller(sellerId.request(), reasonId.request(), mBind.tellMore.text.toString().request())

            sheet.dismiss()
        }

        sheet.show()
    }

    private fun blockUser() {
        bind.loader.isVisible = true
        viewModel.blockUnblockUser(sellerId.request())

        viewModel.blockUnblockUserRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    it.value.data
                    if (it.value.status == "success") {
                        Alerts.success(this, it.value.message ?: "User blocked successfully")
                        finish()
                    } else {
                        Alerts.error(this, it.value.message ?: "Failed to block user")
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
    }

    fun sendTipSheet() {
        val sendTipSheetBind = SendTipSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.send_tip_sheet,
                null,
                false
            )
        )

        val sendTipSheet = Alerts.appBottomSheet(this, true, sendTipSheetBind)

        sendTipSheetBind.root.setOnClickListener {
            hideKeyboard()
        }
        sendTipSheetBind.close.setHapticClickListener {
            sendTipSheet.dismiss()
        }

        sendTipSheetBind.btnTip5.setHapticClickListener {
            sendTipSheetBind.customOffer.setText("5")
        }

        sendTipSheetBind.btnTip10.setHapticClickListener {
            sendTipSheetBind.customOffer.setText("10")
        }

        sendTipSheetBind.btnTip25.setHapticClickListener {
            sendTipSheetBind.customOffer.setText("25")
        }

        sendTipSheetBind.btnTip50.setHapticClickListener {
            sendTipSheetBind.customOffer.setText("50")
        }

        sendTipSheetBind.paymentWallet.text = buildString {
            append("Wallet - ")
            append(App.profileResponse.value?.walletAmount ?: 0)
        }

        sendTipSheetBind.walletRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendTipSheetBind.cardRadio.isChecked = false
            }
        }

        sendTipSheetBind.cardRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendTipSheetBind.walletRadio.isChecked = false
            }
        }

        if (App.profileResponse.value?.defaultCard != null) {
            sendTipSheetBind.paymentCard.text = buildString {
                append("XXXX XXXX XXXX ")
                append(App.profileResponse.value?.defaultCard?.last4 ?: 0)
            }
        } else {
            sendTipSheetBind.cardRadio.isVisible = false
            sendTipSheetBind.paymentCard.text = buildString {
                append("Payment Method Not Added")
            }
        }

        sendTipSheetBind.btnSendTip.setHapticClickListener {

            with(sendTipSheetBind) {

                if (!walletRadio.isChecked && !cardRadio.isChecked) {
                    Alerts.error(this@SellerProfileActivity, "Please select a payment method")
                    return@setHapticClickListener
                }


                if (customOffer.text.toString().isEmpty()) {
                    Alerts.error(this@SellerProfileActivity, "Please enter an amount")
                    return@setHapticClickListener
                }

                if (walletRadio.isChecked && customOffer.text.toString().toDouble() > ((App.profileResponse.value?.walletAmount ?: "0.0").toString()
                        .toDouble())
                ) {
                    Alerts.error(this@SellerProfileActivity, "Insufficient balance")
                    return@setHapticClickListener
                }


            }

            sendTipSheet.dismiss()
            bind.loader.isVisible = true
            viewModel.sendTipAmount(
                sellerId.request(),
                sendTipSheetBind.customOffer.text.toString().request(),
                null
            )

        }

        sendTipSheet.show()
    }


}