package io.bidswipe.app.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.GridAdapter
import io.bidswipe.app.controller.MoreAdapter
import io.bidswipe.app.databinding.FragmentAccountBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.SellerHubResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.ui.scheduleShow.ShowDetailsActivity
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.sellerHub.SellerVerificationActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toAuth
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.toRandomizerTemplates
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toTutorials

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentAccountBinding.inflate(inflater, view, false)

    private var moreList = mutableListOf<MoreModel>()

    private var accountGridList = mutableListOf<MoreModel>()

    private lateinit var moreAdapter: MoreAdapter
    private lateinit var gridAdapter: GridAdapter
    private lateinit var accountGridAdapter: GridAdapter
    private var upcomingShow: SellerHubResponse.Data.UpcomingShow? = null

    private val scheduleShowLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                App.getProfile()
                viewModel.getSellerHubInfo()
            }
        }

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let {
                bind.switcher.displayedChild = it.position
                it.view.findViewById<TextView>(android.R.id.text1)?.apply {
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            tab?.let {
                it.view.findViewById<TextView>(android.R.id.text1)?.apply {
                    setTypeface(typeface, Typeface.NORMAL)
                }
            }
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            tab?.let {
                bind.switcher.displayedChild = it.position
            }
        }
    }

    private val mClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            when (moreList[pos].slug) {
                "logout" -> logoutDialog()
                "aboutUs" -> handlePageUrl(DashViewModel.SLUG_ABOUT_US)
                "accountSecurity" -> startActivity(Intent(mCtx, io.bidswipe.app.ui.more.AccountSecurityActivity::class.java))
                "privacyPolicy" -> handlePageUrl(DashViewModel.SLUG_PRIVACY_POLICY)
                "faq" -> handlePageUrl(DashViewModel.SLUG_FAQ)
                "termsCondition" -> handlePageUrl(DashViewModel.SLUG_TERMS)
                else -> {
                    startActivity(
                        Intent(mCtx, MoreActivity::class.java).putExtra("slug", moreList[pos].slug).putExtra("title", moreList[pos].title)
                    )
                }

            }
        }

    }

    private fun handlePageUrl(slug: String) {
        val title = when (slug) {
            DashViewModel.SLUG_ABOUT_US -> "About Us"
            DashViewModel.SLUG_PRIVACY_POLICY -> "Privacy Policy"
            DashViewModel.SLUG_FAQ -> "FAQ"
            DashViewModel.SLUG_TERMS -> "Terms & Conditions"
            else -> "Content"
        }
        val intent = Intent(mCtx, MoreActivity::class.java).apply {
            putExtra("slug", slug)
            putExtra("title", title)
        }
        startActivity(intent)
    }

    private val accountGridClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when (accountGridList[pos].slug) {

                "notification" -> {
                    startActivity(
                        Intent(mCtx, NotificationActivity::class.java).putExtra(
                            "slug", accountGridList[pos].slug
                        )
                    )
                }

                "savedSearches" -> {
                    // Basecamp #9933801536 round 3: open the PWA management page in
                    // Chrome Custom Tabs. The PWA session cookie auto-auths the user.
                    // Native parity screen is a follow-up; this gives Trey the nav
                    // affordance + a working manage screen today.
                    try {
                        val url = "${io.bidswipe.app.utils.Const.BASE_URL}/app/saved-searches"
                        val customTabs = androidx.browser.customtabs.CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                        customTabs.launchUrl(mCtx, android.net.Uri.parse(url))
                    } catch (e: Exception) {
                        // Fallback: plain ACTION_VIEW if Custom Tabs not available.
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("${io.bidswipe.app.utils.Const.BASE_URL}/app/saved-searches")))
                    }
                }

                "buyer" -> {
                    startActivity(
                        Intent(mCtx, TrustedBuyerActivity::class.java).putExtra(
                            "slug", accountGridList[pos].slug
                        )
                    )
                }

                "favourite" -> {
                    startActivity(
                        Intent(mCtx, ChooseInterestActivity::class.java).putExtra(
                            "fromAccount", true
                        )
                    )
                }

                "clips" -> {
                    findNavController().animatedNav(ids.toClipsFragment, bundleOf("fromAccount" to true))
                }

                else -> {
                    startActivity(
                        Intent(mCtx, MoreActivity::class.java).putExtra(
                            "slug", accountGridList[pos].slug
                        )
                    )
                }
            }

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onMorePrimaryClick {
            viewModel.isDrawerOpened.value = viewModel.isDrawerOpened.value == false
        }

        App.getProfile()

        bind.swipeRefreshLayout.setOnRefreshListener {
            App.getProfile()
            viewModel.getSellerHubInfo()
        }

        App.profileResponse.observe(viewLifecycleOwner) {

            bind.userName.text = it?.name?.asCapital() ?: ""

            bind.sellerSince.isVisible = it?.username.isNullOrEmpty() == false

            bind.sellerSince.text = it?.username ?: "N/A"
            // Basecamp #9916951961 (Trey 2026-05-21) — use the colored bidswipe
            // dollar-circle logo as the avatar fallback instead of the generic
            // gray person silhouette (placeholder_user) when the user has no
            // profile image set. Matches the iOS account screen styling.
            bind.userProfile.loadUrl(mCtx, it?.profileImage.toString(), R.drawable.app_icon_dollar)

            bind.accountView.couponCount.text = (it?.couponCount ?: 0).toString()
        }

        bind.tabs.addOnTabSelectedListener(onTabSelectedListener)

        moreList.clear()
        // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitLab's i18n
        // string resources + Delete Account entry; merged in GitHub's
        // Account Security entry (cmordzx1s00cuf3hgkwnkkplg — iOS parity).
        // Account Security uses a hardcoded string because no R.string
        // resource exists yet; future i18n pass can add it.
        moreList.add(MoreModel(R.drawable.ic_about_us, getString(R.string.about_us), "aboutUs"))
        moreList.add(MoreModel(R.drawable.ic_outlined_message, getString(R.string.contact_us), "contactUs"))
        moreList.add(MoreModel(R.drawable.ic_document, getString(R.string.sales_tax_exemption), "salesTax"))
        moreList.add(MoreModel(R.drawable.ic_document, getString(R.string.terms_and_conditions), "terms-condition"))
        moreList.add(MoreModel(R.drawable.ic_privacy, getString(R.string.privacy_policy_plain), "privacy-policy"))
        moreList.add(MoreModel(R.drawable.ic_faq, getString(R.string.faq_label), "faq"))
        moreList.add(MoreModel(R.drawable.ic_people, getString(R.string.blocked_users), "blockedUsers"))
        moreList.add(MoreModel(R.drawable.ic_privacy, "Account Security", "accountSecurity"))
        moreList.add(MoreModel(R.drawable.ic_trash, getString(R.string.delete_account), "deleteAccount"))
        moreList.add(MoreModel(R.drawable.ic_logout_outline, getString(R.string.logout), "logout"))

        moreAdapter = MoreAdapter(moreList, mClicks)
        bind.accountView.moreRecycler.adapter = moreAdapter

        accountGridList.clear()
        accountGridList.add(MoreModel(R.drawable.ic_box, getString(R.string.payment_shipping), "paymentShipping"))
        accountGridList.add(MoreModel(R.drawable.ic_location, getString(R.string.addresses), "address"))
        accountGridList.add(
            MoreModel(
                R.drawable.ic_identity_verification, getString(R.string.trusted_buyer), "buyer"
            )
        )
        accountGridList.add(MoreModel(R.drawable.notification, getString(R.string.notifications), "notification"))
        // Basecamp #9933801536 round 3 (2026-05-28): Saved Searches nav tile for Android,
        // parity with iOS Account → Saved Searches. Native list/manage screen isn't
        // built yet, so this opens /app/saved-searches in Chrome Custom Tabs against
        // the user's existing PWA session (cookie-authenticated). Native screen is a
        // follow-up.
        accountGridList.add(MoreModel(R.drawable.notification, "Saved Searches", "savedSearches"))
        accountGridList.add(MoreModel(R.drawable.ic_tag_outline, getString(R.string.preferences), "preferences"))
        accountGridList.add(MoreModel(R.drawable.ic_heart, getString(R.string.favourite), "favourite"))
        accountGridList.add(MoreModel(R.drawable.ic_clip_new, getString(R.string.clips), "clips"))

        accountGridAdapter = GridAdapter(accountGridList, accountGridClick)
        bind.accountView.gridRecycler.adapter = accountGridAdapter

        // QA-FIX (MC tasks cmo7iaf7h00cgfi155p712op6 / cmo7iaffj00cifi153dmctomr / cmo7iaflb00ckfi15jxfhw8j2):
        // Items, Revenue, and Rating stat cards on the seller account screen should
        // navigate to the relevant screen when tapped.
        bind.sellerHub.itemsCard.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "inventory"
                )
            )
        }

        bind.sellerHub.revenueCard.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "wallet"
                )
            )
        }

        // MC cmpaj2fex0000w5hgq64jp9k4 merge cleanup (2026-05-24):
        // Earlier ratingCard listener removed — replaced by the cleaner one
        // below that opens SellerProfileActivity with the Reviews tab,
        // matching Basecamp #9922137161 (Trey 2026-05-20).

        bind.sellerHub.payoutCard.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "wallet"
                )
            )
        }

        bind.sellerHub.totalOrderCard.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "order"
                )
            )
        }

        // #30: new orders card click — same destination as total orders
        bind.sellerHub.newOrderCard.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "order"
                )
            )
        }

        // MC cmpaj2fex0000w5hgq64jp9k4 merge cleanup (2026-05-24):
        //   - Removed redundant second `revenueCard` listener (identical wiring
        //     to the one above).
        //   - Repointed `ratingCard` from SellerHubActivity(slug="sellerStatus")
        //     to SellerProfileActivity with selectedTab=2 (Reviews tab) so
        //     tapping Rating goes to ratings/reviews instead of seller status.
        //     This matches Basecamp #9922137161 (Trey 2026-05-20): tapping
        //     Reviews should show ratings/reviews (or empty state), not log
        //     the user out via the wrong destination.
        bind.sellerHub.ratingCard.setHapticClickListener {
            // Basecamp #9922137161 (Trey 2026-05-20): "tapping Reviews logs user
            // out and shows wrong background page". Root cause: the profile
            // wasn't loaded yet at click time, sellerId came through as empty,
            // backend returned UNAUTHORIZED/invalid_token, and the global
            // Resource.Error.parse() handler in Extensions.kt treats that as
            // an auth failure and clears Prefs + redirects to auth screen —
            // hence the logout. Guard here: don't navigate until profile is
            // loaded.
            val profile = App.profileResponse.value
            val sellerId = profile?.id?.toString().orEmpty()
            if (sellerId.isEmpty()) {
                Alerts.info(mCtx, "Loading your account\u2026 please try again in a moment.")
                App.getProfile()
                return@setHapticClickListener
            }
            startActivity(
                Intent(mCtx, SellerProfileActivity::class.java)
                    .putExtra("sellerId", sellerId)
                    .putExtra("selectedTab", 2)
            )
        }

        // #50 Account Health tiles → route to relevant Seller Hub destinations.
        // On-Time Scan Rate + Defect-Free Order Rate → orders status page (MyOrdersFragment).
        // Policy Standing → SellerStatusFragment (marketplace + live-sell vendor status).
        bind.sellerHub.onTimeTile.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "order"
                )
            )
        }

        bind.sellerHub.defectFreeTile.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "order"
                )
            )
        }

        bind.sellerHub.policyStandingTile.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "sellerStatus"
                )
            )
        }

        bind.editIcon.setHapticClickListener {
            startActivity(Intent(mCtx, UpdateAccountActivity::class.java))
        }

        viewModel.logoutRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    successToast(it.value.message.toString())
                    // Clear in-memory session caches so next login starts clean.
                    App.profileResponse.value = null
                    App.checkKycResponse.value = null
                    App.categoryList.clear()
                    App.socketManager?.disconnect()
                    App.socketManager = null
                    Prefs(mCtx).clear()
                    startActivity(mCtx.toAuth())
                    finish()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.logoutRepo.value = null

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

        bind.sellerHub.viewAll.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerHubActivity::class.java).putExtra(
                    "slug", "shows"
                )
            )

        }

        bind.sellerHub.createProduct.setHapticClickListener {

            val profile = App.profileResponse.value
            if (profile?.sellerIdentityStatus != "verified") {
                verificationDialog()
                return@setHapticClickListener
            }
            // Basecamp #9991372302: KYC gate removed for parity with iOS/PWA.
            if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                showPaymentAndAddressSheet()
                return@setHapticClickListener
            }

            startActivity(mCtx.toListProduct())

        }

        bind.sellerHub.createShow.setHapticClickListener {

            val profile = App.profileResponse.value
            if (profile?.sellerIdentityStatus != "verified") {
                verificationDialog()
                return@setHapticClickListener
            }
            // Basecamp #9991372302: KYC gate removed for parity with iOS/PWA.
            if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                showPaymentAndAddressSheet()
                return@setHapticClickListener
            }

            val intent = if (profile.isFirstShowCreated == true) {
                mCtx.toScheduleShow(from = "dash")
            } else {
                mCtx.toTutorials()
            }
            scheduleShowLauncher.launch(intent)

        }

        // ── Randomizer Templates entry point (2026-05-26) ─────────────────────────────
        bind.sellerHub.randomizerTemplatesBtn?.setHapticClickListener {
            val profile = App.profileResponse.value
            if (profile?.sellerIdentityStatus != "verified") {
                verificationDialog()
                return@setHapticClickListener
            }
            startActivity(mCtx.toRandomizerTemplates())
        }

        bind.sellerHub.upcomingShow.setHapticClickListener {
            upcomingShow
            startActivity(
                Intent(mCtx, ShowDetailsActivity::class.java).putExtra(
                    "showId", upcomingShow?.id.toString()
                )
            )
        }

        // #35: Wrap vacation mode toggle in a confirmation dialog
        bind.sellerHub.vacationMode.setOnCheckedChangeListener { compoundButton, isChecked ->
            // Temporarily block the change — we'll revert if user cancels
            compoundButton.setOnCheckedChangeListener(null)
            compoundButton.isChecked = !isChecked
            val titleRes = if (isChecked) "Enable Vacation Mode" else "Disable Vacation Mode"
            val msgRes = if (isChecked)
                "Enable vacation mode? Buyers will not be able to purchase your items while vacation mode is on."
            else
                "Disable vacation mode? Your listings will become available for purchase again."
            AppBottomSheet(
                requireActivity(),
                io.bidswipe.app.R.drawable.ic_info,
                titleRes,
                msgRes,
                primaryBtnText = "Confirm",
                secondaryBtnText = "Cancel",
                canCancel = true,
                showSecondary = true,
                alertType = AlertType.WARNING,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        compoundButton.isChecked = isChecked
                        viewModel.updateVacationModeStatus(isChecked.toString().request())
                        // Restore listener after state change
                        restoreVacationModeListener(compoundButton)
                    }
                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        // Revert: leave toggle at !isChecked (already set above)
                        restoreVacationModeListener(compoundButton)
                    }
                }).show()
        }

        bind.accountView.coupons.setOnClickListener { p0 ->
            startActivity(
                Intent(mCtx, MoreActivity::class.java).putExtra(
                    "slug", "coupons"
                )
            )
        }


        bind.loader.isVisible = true
        viewModel.getSellerHubInfo()
        viewModel.getSellerHubInfoRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    val mData = it.value.data

                    //UPCOMING SHOW
                    if (mData?.upcomingShow != null) {

                        upcomingShow = mData.upcomingShow

                        bind.sellerHub.noShows.isVisible = false
                        bind.sellerHub.upcomingShow.isVisible = true

                        val item = mData.upcomingShow
                        bind.sellerHub.name.text = item.title?.asCapital() ?: "N/A"
                        bind.sellerHub.category.text = item.category?.name?.asCapital() ?: "N/A"
                        bind.sellerHub.time.text = buildString {
                            append(
                                Utils.getFormattedDateTime(
                                    "yyyy-MM-dd", "MM-dd-yyyy", item.date ?: ""
                                )
                            )
                            append(" ")
                            append(Const.BULLET)
                            append(" ")
                            append(
                                Utils.getFormattedDateTime(
                                    "HH:mm:ss", "hh:mm a", item.time ?: ""
                                )
                            )
                        }

                        bind.sellerHub.image.loadUrl(mCtx, item.imgThumbnail?.first() ?: "")

                    }

                    //ACCOUNT HEALTH
                    bind.sellerHub.onTimePercent.text = mData?.accountHealth?.onTimeScanRate ?: "N/A"
                    bind.sellerHub.defectFreeOrderRate.text = mData?.accountHealth?.defectFreeOrderRate ?: "N/A"
                    bind.sellerHub.policyStanding.text = mData?.accountHealth?.policyStanding ?: "N/A"

                    // #29 fix: use == 1 for singular (was `< 1` which wrongly showed 0→"Item" and 1→"Items")
                    val total = (mData?.totalOrders ?: 0)
                    bind.sellerHub.totalOrders.text = buildString {
                        append(total.toString())
                        if (total == 1) append(" Item") else append(" Items")
                    }
                    // #30 fix: bind new_orders from seller-hub-info (field was missing from model)
                    val newOrders = mData?.newOrders ?: 0
                    bind.sellerHub.newOrders.text = buildString {
                        append(newOrders.toString())
                        if (newOrders == 1) append(" Item") else append(" Items")
                    }
                    bind.sellerHub.payoutAmount.text = (mData?.payouts ?: 0.0).toString().asMoney()

                    bind.sellerHub.vacationMode.isChecked = mData?.vacationMode ?: false

                    bind.sellerHub.revenue.text = (mData?.revenue ?: 0).toString().asMoney()
                    bind.sellerHub.itemCount.text = (mData?.items ?: 0).toString()
                    bind.sellerHub.rating.text = (mData?.rating ?: 0.0).toString()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.getSellerHubInfoRepo.value = null
                    bind.swipeRefreshLayout.isRefreshing = false

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

    override fun onDestroyView() {
        bind.tabs.removeOnTabSelectedListener(onTabSelectedListener)
        super.onDestroyView()
    }

    private fun logoutDialog() {
        AppBottomSheet(
            mCtx,
            R.drawable.ic_logout_outline,
            "Logout",
            "Are you sure you want to logout?",
            primaryBtnText = "Logout",
            secondaryBtnText = "Cancel",
            canCancel = true,
            iconPadding = 36,
            showSecondary = true,
            alertType = AlertType.WARNING,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    bind.loader.isVisible = true
                    viewModel.logout()
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }

        ).show()

    }

    fun showPaymentAndAddressSheet() {

        val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.payment_and_address_sheet,
                null,
                false
            )
        )

        val makeOfferSheet = Alerts.appBottomSheet(mCtx, true, paymentAddressBind)

        with(paymentAddressBind.addressItem) {
            val hasAddress = App.profileResponse.value?.hasShippingAddress == true
            moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
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
                    Intent(mCtx, MoreActivity::class.java).putExtra(
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
            moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
            moreIcon.rotation = 0f

            if (hasCard) {
                // #44: use orEmpty() / let to avoid printing "null" when fields are missing
                val last4 = App.profileResponse.value?.defaultCard?.last4.orEmpty()
                val expM = App.profileResponse.value?.defaultCard?.expMonth?.toString().orEmpty()
                val expY = App.profileResponse.value?.defaultCard?.expYear?.toString().orEmpty()
                cardNumber.text = if (last4.isNotEmpty()) "•••• •••• •••• $last4" else "•••• •••• •••• ••••"
                expiryDate.text = if (expM.isNotEmpty()) "$expM/$expY" else ""

            } else {
                cardNumber.text = buildString {
                    append("Payment Cards Not Added")
                }
            }

            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(mCtx, MoreActivity::class.java).putExtra(
                        "slug",
                        "paymentShipping"
                    )
                )
            }

        }

        paymentAddressBind.close.setHapticClickListener {
            makeOfferSheet.dismiss()
        }
        makeOfferSheet.show()

    }

    private fun verificationDialog() {
        AppBottomSheet(
            mCtx,
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
                    startActivity(Intent(mCtx, SellerVerificationActivity::class.java))
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }
            }
        ).show()

    }

    /** Restore the vacation mode listener after dialog dismissal (#35) */
    private fun restoreVacationModeListener(compoundButton: android.widget.CompoundButton) {
        compoundButton.setOnCheckedChangeListener { btn, isChecked ->
            compoundButton.setOnCheckedChangeListener(null)
            compoundButton.isChecked = !isChecked
            val titleRes = if (isChecked) "Enable Vacation Mode" else "Disable Vacation Mode"
            val msgRes = if (isChecked)
                "Enable vacation mode? Buyers will not be able to purchase your items while vacation mode is on."
            else
                "Disable vacation mode? Your listings will become available for purchase again."
            AppBottomSheet(
                requireActivity(),
                io.bidswipe.app.R.drawable.ic_info,
                titleRes,
                msgRes,
                primaryBtnText = "Confirm",
                secondaryBtnText = "Cancel",
                canCancel = true,
                showSecondary = true,
                alertType = AlertType.WARNING,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        btn.isChecked = isChecked
                        viewModel.updateVacationModeStatus(isChecked.toString().request())
                        restoreVacationModeListener(btn)
                    }
                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        restoreVacationModeListener(btn)
                    }
                }).show()
        }
    }
}