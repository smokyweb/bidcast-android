package io.bidswipe.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.controller.SellerToolsAdapter
import io.bidswipe.app.databinding.ActivityDashBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.databinding.SellBottomSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.model.SellerToolModel
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.ui.sellerHub.SellerVerificationActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toTutorials

class DashActivity : BaseActivity(), NavController.OnDestinationChangedListener {

    private val bind by bind(ActivityDashBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private lateinit var imageSheet: BottomSheetBehavior<LinearLayout>
    private lateinit var sellerToolsAdapter: SellerToolsAdapter
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var mSellSheet: BottomSheetDialog
    private lateinit var navController: NavController

    private var gridList = mutableListOf<SellerToolModel>()
    private var sellList = mutableListOf<SellModel>()

    private val gridClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            if (status?.isEmpty() == false) {
                when (val slug = gridList[pos].list[status.toInt()].slug) {
                    "sellerVerification" -> {
                        startActivity(
                            Intent(this@DashActivity, SellerVerificationActivity::class.java).putExtra(
                                "slug",
                                slug
                            )
                        )
                    }

                    "training" -> {
                        startActivity(this@DashActivity.toTutorials())
                    }

                    "notifications" -> {
                        startActivity(
                            Intent(this@DashActivity, NotificationActivity::class.java).putExtra(
                                "slug",
                                slug
                            )
                        )
                    }

                    else -> {
                        startActivity(
                            Intent(this@DashActivity, SellerHubActivity::class.java).putExtra(
                                "slug",
                                slug
                            ).putExtra("url", "")
                        )
                    }
                }
            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.contentDash.root.setPadding(0, system.top, 0, system.bottom)
            bind.sideMenu.setPadding(0, system.top, 0, system.bottom)
            CONSUMED
        }

        checkIntent(intent)

        navHostFragment = supportFragmentManager.findFragmentById(ids.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener(this)

        bind.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        bind.drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {

            }

            override fun onDrawerClosed(drawerView: View) {
                viewModel.isDrawerOpened.value = false

            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })


        bind.header.onBackClick { bind.drawer.closeDrawer(GravityCompat.END) }

        gridList.clear()

        gridList.add(
            SellerToolModel(
                "Seller",
                mutableListOf(
                    MoreModel(R.drawable.ic_training_outline, "Seller Training", "training"),
                    MoreModel(R.drawable.ic_seller_verification, "Seller Verification", "sellerVerification"),
                    MoreModel(R.drawable.ic_identity_verification, "Identity Verification", "identityVerification"),
                    MoreModel(R.drawable.ic_box, "Inventory", "inventory"),
                    MoreModel(R.drawable.ic_clip_new, "Shows", "shows"),
                    MoreModel(R.drawable.ic_order, "My Orders", "order"),
                    MoreModel(R.drawable.ic_wallet, "Wallet", "wallet"),
                    MoreModel(R.drawable.ic_tag_outline, "Offers", "offers"),
                    MoreModel(R.drawable.ic_gift, "Tips", "tips"),
                    MoreModel(R.drawable.notification, "Notifications", "notifications"),
                )
            )
        )

        gridList.add(
            SellerToolModel(
                "Promotion",
                mutableListOf(
                    MoreModel(R.drawable.ic_people, "Affiliate Program", "program"),
                    MoreModel(R.drawable.ic_sound, "Promote Tools", "promote")
                )
            )
        )

        gridList.add(
            SellerToolModel(
                "Performance",
                mutableListOf(
                    MoreModel(R.drawable.ic_shop, "Premier Shop", "shop"),
                    MoreModel(R.drawable.ic_graph, "Seller Analytics", "sellerAnalytics"),
                )
            )
        )
        gridList.add(
            SellerToolModel(
                "Settings",
                mutableListOf(
                    MoreModel(R.drawable.ic_shipping, "Shipping", "shipping"),
                    MoreModel(R.drawable.ic_graph, "Seller Status", "sellerStatus")
                )
            )
        )

        sellerToolsAdapter = SellerToolsAdapter(
            gridList, gridClick
        )
        bind.menuRecycler.adapter = sellerToolsAdapter

        viewModel.isDrawerOpened.observe(this) {

            if (it) bind.drawer.openDrawer(GravityCompat.END)

        }

        bind.contentDash.bottomBar.setupWithNavController(navController)
        sellSheet()

        log("USER NAME : ${userName.replace(" ", ".")}  $userId   $userImage")

        bind.contentDash.bottomBar.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId != ids.sellFragment) viewModel.lastIndex.value = menuItem.itemId
            when (menuItem.itemId) {
                ids.sellFragment -> {
//					imageSheet.state = BottomSheetBehavior.STATE_EXPANDED
                    mSellSheet.show()
                    return@setOnItemSelectedListener true
                }

                else -> {
//					imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    mSellSheet.dismiss()
                    try {
                        navController.let { ctrl ->
                            NavigationUI.onNavDestinationSelected(menuItem, ctrl)
                            ctrl.popBackStack(menuItem.itemId, false)
                        }
                        return@setOnItemSelectedListener true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@setOnItemSelectedListener false
                    }
                }
            }
        }

        getDeviceToken(this) {
            Log.d(TAG, "onCreate: $it")
            viewModel.storeDeviceDetails(it.request())
        }

        requestPerms(Const.PERMISSIONS) {}

        viewModel.storeDeviceDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    log(mData.toString())

                }

                is Resource.Error -> {

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

        App.getProfile()

        App.getCategories()

    }

    fun hideBottomNav() {
        bind.contentDash.bottomBar.isVisible = false
    }

    fun showBottomNav() {
        bind.contentDash.bottomBar.isVisible = true
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        when (destination.id) {
            R.id.exploreTypeFragment -> hideBottomNav()
            else -> showBottomNav()
        }
    }

    private fun sellSheet() {
        val sheetView = SellBottomSheetBinding.bind(layoutInflater.inflate(R.layout.sell_bottom_sheet, null, false))

        mSellSheet = Alerts.appBottomSheet(this, true, sheetView)

        mSellSheet.setOnDismissListener {
            bind.contentDash.bottomBar.selectedItemId = viewModel.lastIndex.value ?: 0
        }

        sellList.clear()
        sellList.addAll(
            listOf(
                SellModel(
                    R.drawable.ic_tag_outline,
                    R.color.primaryContainer,
                    "List a Product",
                    "Create listing for your item"
                ),
                SellModel(
                    R.drawable.ic_video,
                    R.color.primaryContainer,
                    "Schedule a Show",
                    "Go live and sell to your audience"
                ),
                SellModel(
                    R.drawable.ic_shop,
                    R.color.primaryContainer,
                    "Seller Hub",
                    "Manage your store and listings"
                )
            )
        )

        val exploreAdapter = SellAdapter(sellList, "explore", object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

                when (pos) {
                    2 -> {
                        bind.contentDash.bottomBar.selectedItemId = ids.accountFragment
                        return
                    }
                }

                val profile = App.profileResponse.value

                if (profile?.sellerIdentityStatus != "verified") {
                    verificationDialog()
                    return
                }

                if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                    showPaymentAndAddressSheet()
                    return
                }

                when (pos) {
                    0 -> {
                        startActivity(this@DashActivity.toListProduct())
                        mSellSheet.dismiss()
                    }

                    1 -> {
                        val isFirstShow = profile.isFirstShowCreated == true

                        val intent = if (isFirstShow) {
                            this@DashActivity.toScheduleShow(from = "dash")
                        } else {
                            this@DashActivity.toTutorials()
                        }

                        startActivity(intent)
                        mSellSheet.dismiss()
                    }
                }
            }

        })
        sheetView.recycler.adapter = exploreAdapter

        sheetView.root.setHapticClickListener {
            mSellSheet.dismiss()
        }

        sheetView.close.setHapticClickListener {
            mSellSheet.dismiss()
        }
    }

    private val mSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    /*val params = CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.MATCH_PARENT,
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 0)
                    bind.coOrdinate.setLayoutParams(params)*/

                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                }

                BottomSheetBehavior.STATE_DRAGGING -> {
                }

                BottomSheetBehavior.STATE_HALF_EXPANDED -> {

                }

                BottomSheetBehavior.STATE_SETTLING -> {

                }

                BottomSheetBehavior.STATE_COLLAPSED -> {
                    bind.contentDash.bottomBar.selectedItemId = viewModel.lastIndex.value ?: 0
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset > 0) {
                try {
//						bind.commentSheet.sheetRoot.itemClick.alpha = slideOffset
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    fun getDeviceToken(context: Context, token: (token: String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (!it.isSuccessful) {
                Alerts.log(
                    javaClass.simpleName,
                    "Fetching FCM registration token failed ${it.exception}"
                )
                return@addOnCompleteListener
            }
            val deviceToken = it.result.toString()
            if (Prefs(context).fcmToken() != deviceToken) {
                Prefs(context).putString(Prefs.PUSH_TOKEN, deviceToken)
                Alerts.log(javaClass.simpleName, "device token $deviceToken")
            } else {
                Alerts.log(javaClass.simpleName, "device token not refresh  $token")
            }
            token(deviceToken)
        }
    }

    fun showPaymentAndAddressSheet() {

        val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.payment_and_address_sheet,
                null,
                false
            )
        )

        val makeOfferSheet = Alerts.appBottomSheet(this, true, paymentAddressBind)

        with(paymentAddressBind.addressItem) {
            val hasAddress = App.profileResponse.value?.hasShippingAddress == true
            moreIcon.setImageDrawable(ContextCompat.getDrawable(this@DashActivity, draw.ic_pencil))
            moreIcon.rotation = 0f

            name.isVisible = hasAddress
            address.isVisible = hasAddress

            if (hasAddress) {
                val addressData = App.profileResponse.value?.defaultShippingAddress
                address.text = addressData?.streetAddress
                name.text = addressData?.name
                type.text = addressData?.type
                defaultAddress.isVisible = addressData?.isDefault == true
            } else {
                type.text = buildString {
                    append("Address Not Added")
                }
                defaultAddress.isVisible = false
            }

            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@DashActivity, MoreActivity::class.java).putExtra(
                        "slug",
                        "paymentShipping"
                    )
                )
            }
        }

        with(paymentAddressBind.paymentCard) {
            val hasCard = App.profileResponse.value?.hasCardAdded == true
            iconCard.isVisible = hasCard
            expiryDate.isVisible = hasCard
            moreIcon.setImageDrawable(ContextCompat.getDrawable(this@DashActivity, draw.ic_pencil))
            moreIcon.rotation = 0f

            if (hasCard) {
                cardNumber.text = buildString {
                    append("•••• •••• •••• ")
                    append(App.profileResponse.value?.defaultCard?.last4)
                }

                expiryDate.text = buildString {
                    append(App.profileResponse.value?.defaultCard?.expDate)
                }

            } else {
                cardNumber.text = buildString {
                    append("Payment Cards Not Added")
                }
            }

            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@DashActivity, MoreActivity::class.java).putExtra(
                        "slug",
                        "paymentShipping"
                    )
                )
            }

        }

        paymentAddressBind.close.setHapticClickListener {
            makeOfferSheet.dismiss()
        }

        imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        makeOfferSheet.show()

    }

    private fun verificationDialog() {
        AppBottomSheet(
            this,
            R.drawable.ic_info,
            title = when (App.profileResponse.value?.sellerIdentityStatus) {
                "null" -> {
                    "Become a Verified Seller!"
                }

                "pending" -> {
                    "Verification Pending!"
                }

                "rejected" -> {
                    "Verification Rejected!"
                }

                else -> {
                    "Become a Verified Seller!"
                }
            },
            message = when (App.profileResponse.value?.sellerIdentityStatus) {
                "null" -> {
                    "Your seller verification request has been rejected, You need to reapply for the verification."
                }

                "pending" -> {
                    "Your seller verification request is currently pending. You will be able to access this functionality once it is approved by the admin."
                }

                "rejected" -> {
                    "Your seller verification request was not approved. Please reapply to complete the verification process."
                }

                else -> {
                    "Before you interact with lives shows, You need to become a Verified Seller."
                }
            },
            primaryBtnText = "Okay",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = false,
            iconPadding = 16,
            alertType = AlertType.INFO,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()

                    if (App.profileResponse.value?.sellerIdentityStatus == "pending") {
                        return
                    }

                    startActivity(Intent(this@DashActivity, SellerVerificationActivity::class.java))
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }
        ).show()

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntent(intent)
    }

    fun checkIntent(intent: Intent) {
        log("ON NEW INTENT ${intent.data}")

        if (intent.action == ACTION_VIEW) {
            val item = intent.data?.toString() ?: ""

            val sellerId = item.split("seller/")
            if (sellerId.size > 1) {
                val sellerIdValue = sellerId.lastOrNull() ?: ""
                if (sellerIdValue.isNotEmpty()) {
                    startActivity(
                        Intent(this@DashActivity, SellerProfileActivity::class.java).putExtra(
                            "sellerId", sellerIdValue
                        )
                    )
                }
            }

            val productId = item.split("products/")
            if (productId.size > 1) {
                val productIdValue = productId.lastOrNull() ?: ""
                if (productIdValue.isNotEmpty()) {
                    startActivity(
                        Intent(this@DashActivity, ProductDetailsActivity::class.java).putExtra(
                            "productId", productIdValue
                        )
                    )
                }
            }

            val roomID = item.split("roomId=")
            if (roomID.size > 1) {
                val roomIdValue = roomID.lastOrNull() ?: ""
                val splitted = roomIdValue.split("_room_")
                if (splitted.size > 1) {
                    val userRoom = splitted.lastOrNull()?.split("_")
                    if (userRoom?.size == 2) {
                        val userId = userRoom.firstOrNull() ?: ""
                        val roomId = userRoom.lastOrNull() ?: ""

                        if (userId.isNotEmpty() && roomId.isNotEmpty()) {
                            startActivity(
                                Intent(this@DashActivity, ViewLiveShowActivity::class.java)
                                    .putExtra("roomId", roomIdValue)
                                    .putExtra("userId", userId)
                                    .putExtra("roomIdsList", roomIdValue)
                                    .putParcelableArrayListExtra(
                                        "streamList",
                                        ArrayList(listOf(
                                            StreamModel(
                                                roomIdValue.toString(),
                                                "",
                                                thumbnail = ""
                                            )
                                        ))
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
