package io.bidswipe.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.messaging.FirebaseMessaging
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.ActivityDashBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.sellerHub.SellerVerificationActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toTutorials
import io.bidswipe.app.utils.value

class DashActivity : BaseActivity(), NavController.OnDestinationChangedListener {

    private val bind by bind(ActivityDashBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private lateinit var imageSheet: BottomSheetBehavior<ConstraintLayout>
    private var sellList = mutableListOf<SellModel>()

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    private var isFirstShowCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        navHostFragment =
            supportFragmentManager.findFragmentById(ids.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener(this)
        bind.bottomBar.setupWithNavController(navController)
        setupImageSheet()

        log("USER NAME : ${userName.replace(" ", ".")}  $userId   $userImage")

        bind.bottomBar.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId != ids.sellFragment) viewModel.lastIndex.value = menuItem.itemId
            when (menuItem.itemId) {

                ids.sellFragment -> {
                    imageSheet.state = BottomSheetBehavior.STATE_EXPANDED
                    return@setOnItemSelectedListener true
                }

                else -> {
                    imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
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

        requestPerms(Const.PERMISSIONS) { per ->

        }

        viewModel.storeDeviceDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    log(mData.toString())

                }

                is Resource.Error -> {

                    if (it.isNetworkError) {

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

        App.getProfile()

    }

    fun hideBottomNav() {
        bind.bottomBar.isVisible = false
    }

    fun showBottomNav() {
        bind.bottomBar.isVisible = true
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

    private fun setupImageSheet() {
        BottomSheetBehavior.from(bind.sellSheet.root)

        imageSheet = BottomSheetBehavior.from(bind.sellSheet.root).also {
            it.peekHeight = 0
            it.isHideable = true
            it.isDraggable = false
            it.isFitToContents = false
        }

        imageSheet.addBottomSheetCallback(mSheetCallback)

        imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED

        sellList.clear()
        sellList.addAll(
            listOf(
                SellModel(
                    R.drawable.ic_tag,
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
                        bind.bottomBar.selectedItemId = ids.accountFragment
                        return
                    }
                }

                val profile = App.profileResponse.value

                if (profile?.sellerIdentityStatus != "verified") {
                    startActivity(Intent(this@DashActivity, SellerVerificationActivity::class.java))
                    return
                }

                if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                    showPaymentAndAddressSheet()
                    return
                }

                when (pos) {
                    0 -> startActivity(this@DashActivity.toListProduct())

                    1 -> {
                        val isFirstShow = profile.isFirstShowCreated == true
                        val intent = if (isFirstShow) {
                            this@DashActivity.toScheduleShow(from = "dash")
                        } else {
                            this@DashActivity.toTutorials()
                        }
                        startActivity(intent)
                    }
                }
            }

        })

        bind.sellSheet.recycler.adapter = exploreAdapter

        bind.sellSheet.root.setOnClickListener {
            imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bind.sellSheet.close.setOnClickListener {
            imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
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
                    bind.bottomBar.selectedItemId = viewModel.lastIndex.value ?: 0
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
        Log.d(TAG, "getDeviceToken: ")
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (!it.isSuccessful) {
                Alerts.log(
                    javaClass.simpleName,
                    "Fetching FCM registration token failed ${it.exception}"
                )
                return@addOnCompleteListener
            }
            val deviceToken = it.result.toString()
            Log.d(TAG, "getDeviceToken: ")

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
        var paymentAddressBind = PaymentAndAddressSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.payment_and_address_sheet,
                null,
                false
            )
        )
        var makeOfferSheet = Alerts.appBottomSheet(this, true, paymentAddressBind)

        paymentAddressBind.close.setOnClickListener {
            makeOfferSheet.dismiss()
        }


        makeOfferSheet.show()
    }

}