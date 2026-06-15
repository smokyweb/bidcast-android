package io.bidswipe.app.ui.scheduleShow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.bidswipe.app.App
import com.gyf.immersionbar.ktx.navigationBarHeight
import com.gyf.immersionbar.ktx.statusBarHeight
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.databinding.ActivityShowDetailsBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.GetShowDetailsResponse
import io.bidswipe.app.network.response.toLiveShowProduct
import io.bidswipe.app.ui.cohost.CoHostJoinActivity
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerHub.SellerVerificationActivity
import io.bidswipe.app.ui.tutorials.TutorialsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toSellerShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ShowDetailsActivity : BaseActivity() {

    private val bind by bind(ActivityShowDetailsBinding::inflate)
    private val viewModel by viewModels<ScheduleShowViewModel>()

    private var showData: GetShowDetailsResponse.Data?? = null
    private var promotePlans = mutableListOf<GetPromotePlansResponse.Data?>()

    private var editShowLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                bind.loader.isVisible = true
                viewModel.getShowDetails(viewModel.showId.toString())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.header.setHeaderPadding(
                resources.dpToPx(8),
                system.top,
                resources.dpToPx(8),
                resources.dpToPx(8)
            )
            bind.root.setPadding(0, 0, 0, system.bottom)
            CONSUMED
        }

        viewModel.showId = intent?.getStringExtra("showId")
        if (!viewModel.showId.isNullOrEmpty()) {
            bind.loader.isVisible = true
            viewModel.getShowDetails(viewModel.showId.toString())
        } else {
            errorToast("Something went wrong")
            finishAfterTransition()
        }

        bind.header.onBackClick {
            finishAfterTransition()
        }

        bind.editShow.setHapticClickListener {
            editShowLauncher.launch(toScheduleShow(from = "dash", showId = viewModel.showId))
        }

        // Basecamp #9940152629 (2026-05-29): tip settings for this show.
        bind.tipSettingsBtn.setHapticClickListener {
            val sid = viewModel.showId
            if (!sid.isNullOrBlank()) {
                startActivity(
                    android.content.Intent(this, TipSettingActivity::class.java)
                        .putExtra("schedule_show_id", sid)
                )
            }
        }

        // #49 — Promote button: fetch plans then show the promote bottom sheet.
        // Basecamp #9986427172: if launched with autoPromote=true (from PrepareYourShowFragment
        // step 4 in show-context mode) open the promote sheet automatically once plans load.
        val autoPromote = intent.getBooleanExtra("autoPromote", false)
        viewModel.getPromoteShowList()
        viewModel.getPromoteShowListRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getPromoteShowListRepo.value = null
                    promotePlans.clear()
                    promotePlans.addAll(it.value.data ?: mutableListOf())
                    if (autoPromote && promotePlans.isNotEmpty()) {
                        showPromoteSheet()
                    }
                }
                is Resource.Error -> {
                    viewModel.getPromoteShowListRepo.value = null
                    if (autoPromote) {
                        errorToast("Promote plans unavailable. You can promote from this screen.")
                    }
                }
                else -> {}
            }
        }

        viewModel.promoteShowRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.promoteShowRepo.value = null
                    AppBottomSheet(
                        this,
                        R.drawable.ic_success,
                        "Show Promoted",
                        it.value.message ?: "",
                        primaryBtnText = "Okay",
                        secondaryBtnText = "Cancel",
                        canCancel = true,
                        showSecondary = false,
                        iconPadding = 16,
                        alertType = AlertType.SUCCESS,
                        clicks = object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                                if (autoPromote) {
                                    setResult(Activity.RESULT_OK)
                                    finishAfterTransition()
                                }
                            }
                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }
                        }
                    ).show()
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.promoteShowRepo.value = null
                }
                else -> {}
            }
        }

        bind.promoteShow.setHapticClickListener {
            if (promotePlans.isNotEmpty()) {
                showPromoteSheet()
            } else {
                errorToast("Promote plans not available. Please try again.")
                viewModel.getPromoteShowList()
            }
        }

        bind.startShow.setHapticClickListener {
            val profile = App.profileResponse.value

            if (profile?.sellerIdentityStatus != "verified") {
                startActivity(Intent(this, SellerVerificationActivity::class.java))
                return@setHapticClickListener
            }

            if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                showPaymentAndAddressSheet()
                return@setHapticClickListener
            }

            val user = showData?.user
            val products = showData?.products?.map { product -> product?.toLiveShowProduct() }

            if (products?.isEmpty() == true) {
                errorToast("No products found for this Show")
                return@setHapticClickListener
            } else {
                products?.first()?.isCurrent = true
            }

            val showData = LiveShowModel(
                seller = LiveShowModel.Seller(
                    id = user?.id.toString(),
                    image = user?.profileImage ?: "",
                    name = user?.name,
                    rating = user?.rating ?: ""
                ),
                products = showData?.products?.map { product -> product?.toLiveShowProduct() }
                    ?: emptyList(),
                roomId = "live_room_${userId}_${showData?.id.toString()}",
                showDetail = showData?.title ?: "",
                thumbnail = showData?.thumbnail?.getOrNull(0) ?: "",
                viewerCount = "1",
                highestBid = LiveShowModel.HighestBid(
                    bidAmount = "",
                    userName = "",
                    userImage = "",
                    userId = "",
                    productId = ""
                ),
                isLive = true,
                time = showData?.time,
                showId = showData?.id.toString(),
                allowBidForAll = true,
                bidCountDown = "",
                showTimer = "",
                categoryId = showData?.category?.id.toString(),
                subCategoryId = showData?.subCategoryId.toString(),
                auctionTypeId = showData?.auctionTypeId,
            )

            if (App.PIPMode) {
                errorToast("You are already in Live show")
            } else {
                openSellerShowWithCohostPrompt(showData)
            }
        }

        // Basecamp #9991479337 — "Let's Prepare" entry for show owner.
        // Pass showId so PrepareYourShowFragment can prefill step completion state.
        bind.letsPrepareBtn.setHapticClickListener {
            startActivity(
                Intent(this, TutorialsActivity::class.java)
                    .putExtra("type", "letsPrep")
                    .putExtra("showId", viewModel.showId)
            )
        }

        // Basecamp #9991482788 — "Pair Second Device" for show owner.
        bind.pairSecondDeviceBtn.setHapticClickListener {
            showPairSecondDeviceDialog()
        }

        // Basecamp #9991482788 — "Join as Cohost" for non-owners (and visible to all).
        bind.joinAsCohostBtn.setHapticClickListener {
            startActivity(Intent(this, CoHostJoinActivity::class.java))
        }

        viewModel.getShowDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    showData = it.value.data

                    bind.showTitle.text = showData?.title ?: "Show Details"

                    // Basecamp #9991479337 / #9991482788 — show owner-only actions.
                    val isOwner = showData?.userId?.toString() == userId
                    bind.letsPrepareBtn.isVisible = isOwner
                    bind.pairSecondDeviceBtn.isVisible = isOwner
                    // Basecamp #9991482788: "Join as Cohost" must be visible to EVERYONE
                    // (owner and non-owner). The primary use case is the same seller's
                    // SECOND DEVICE — logged in as owner — entering a pairing code.
                    // Restricting to !isOwner broke that flow.
                    bind.joinAsCohostBtn.isVisible = true

                    bind.repeat.text = showData?.repeatValue?.asCapital() ?: "N/A"
                    bind.auctionType.text = showData?.auction?.name ?: "N/A"
                    bind.discoverability.text = (showData?.showDiscoverability ?: "").asCapital()

                    bind.explicitContent.text = if (showData?.isExplicit ?: false) "Yes" else "No"
                    bind.language.text = (showData?.language ?: "").asCapital()

                    if (showData?.category != null) {
                        bind.category.text = showData?.category?.name?.asCapital()
                    } else {
                        bind.category.isVisible = false
                    }

                    bind.time.text = buildString {
                        append(
                            Utils.getFormattedDateTime(
                                "yyyy-MM-dd",
                                "MM-dd-yyyy",
                                showData?.date.toString()
                            )
                        )
                        append(" ")
                        append(Const.BULLET)
                        append(" ")
                        append(
                            Utils.getFormattedDateTime(
                                "HH:mm:ss",
                                "hh:mm a",
                                showData?.time.toString()
                            )
                        )
                    }

                    bind.sales.text = buildString {
                        append((showData?.totalSalesAmount ?: 0).toString().asMoney())
                        append(" sales ")
                        append(Const.BULLET + " ")
                        append(showData?.totalOrders ?: 0)
                        append(" orders")
                    }

                    bind.image.loadUrl(this, showData?.thumbnail?.first() ?: "")
                    if (showData?.products?.isNotEmpty() == true) {
                        bind.addedProducts.isVisible = true
                        bind.recycler.isVisible = true

                        val products = showData?.products?.toMutableList() ?: mutableListOf()

                        val productAdapter =
                            InventoryAdapter(products, false, object : RecyclerClicks {
                                override fun itemClick(pos: Int, status: String?) {
	                                    startActivity(
	                                        Intent(
	                                            this@ShowDetailsActivity,
	                                            ProductDetailsActivity::class.java
	                                        ).putExtra(
	                                            "productId", products[pos]?.id.toString()
	                                        ).putExtra(
	                                            "isFromShowDetails", true
	                                        ).putExtra(
	                                            "showId", viewModel.showId.toString()
	                                        )
	                                    )
	                                }

                            }, "show_details")

                        bind.recycler.adapter = productAdapter
                        bind.exSpace.text = buildString {
                            append(products.size)
                            append("/100")
                        }

                    } else {
                        bind.addedProducts.isVisible = false
                        bind.recycler.isVisible = false
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

    private fun openSellerShowWithCohostPrompt(liveShow: LiveShowModel) {
        val scheduleShowId = liveShow.showId.orEmpty()
        val isOwnLiveShow = showData?.isLive == true &&
            (showData?.userId?.toString() == userId || liveShow.seller?.id == userId)

        fun startLive(takeOverVideo: Boolean, controlOnly: Boolean) {
            startActivity(
                toSellerShow(liveShow.time, liveShow).apply {
                    putExtra("same_account_second_device", takeOverVideo || controlOnly)
                    putExtra("take_over_video", takeOverVideo)
                    putExtra("control_only", controlOnly)
                    putExtra("host_user_id", userId)
                }
            )
        }

        if (!isOwnLiveShow || scheduleShowId.isBlank()) {
            startLive(takeOverVideo = false, controlOnly = false)
            return
        }

        bind.loader.isVisible = true
        lifecycleScope.launch {
            val shouldOffer = withContext(Dispatchers.IO) {
                try {
                    val token = Prefs(this@ShowDetailsActivity).token()
                    val url = URL("${Const.BASE_URL}/api/product/co-host/show/$scheduleShowId/presence")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 6_000
                    conn.readTimeout = 6_000
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    val rc = conn.responseCode
                    val body = (if (rc in 200..299) conn.inputStream else conn.errorStream)
                        .bufferedReader()
                        .use { it.readText() }
                    conn.disconnect()
                    if (rc in 200..299) {
                        JSONObject(body).optJSONObject("data")?.optBoolean("should_offer_takeover", false) == true
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
            bind.loader.isVisible = false

            if (!shouldOffer) {
                startLive(takeOverVideo = false, controlOnly = false)
                return@launch
            }

            androidx.appcompat.app.AlertDialog.Builder(this@ShowDetailsActivity)
                .setMessage("Would you like to enter and take over video?")
                .setPositiveButton("Yes") { _, _ -> startLive(takeOverVideo = true, controlOnly = false) }
                .setNegativeButton("No") { _, _ -> startLive(takeOverVideo = false, controlOnly = true) }
                .show()
        }
    }

    fun showPromoteSheet() {
        val promoteSheetBind = PromoteShowSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.promote_show_sheet,
                null,
                false
            )
        )

        val newHeight = window?.decorView?.measuredHeight
        val viewGroupLayoutParams = promoteSheetBind.root.layoutParams
            ?: android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        viewGroupLayoutParams.height = (newHeight ?: 0) - statusBarHeight
        promoteSheetBind.root.layoutParams = viewGroupLayoutParams

        promoteSheetBind.bottomText.setMargins(
            0,
            0,
            0,
            navigationBarHeight + resources.dpToPx(32)
        )

        val promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)

        promoteSheetBind.optionList.adapter =
            PromoteSheetAdapter(promotePlans, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {
                    val plan = promotePlans.getOrNull(pos) ?: return
                    promoteSheet.dismiss()
                    confirmPromotePurchase(plan)
                }
            })

        promoteSheetBind.close.setHapticClickListener {
            promoteSheet.dismiss()
        }

        promoteSheet.show()
    }

    private fun confirmPromotePurchase(plan: GetPromotePlansResponse.Data) {
        val defaultCard = App.profileResponse.value?.defaultCard
        if (App.profileResponse.value?.hasCardAdded != true || defaultCard?.cardId.isNullOrBlank()) {
            AppBottomSheet(
                this,
                R.drawable.ic_warning,
                "Payment Method Required",
                "Add a payment card before purchasing a show promotion.",
                primaryBtnText = "Okay",
                secondaryBtnText = "Cancel",
                canCancel = true,
                showSecondary = false,
                iconPadding = 16,
                alertType = AlertType.WARNING,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }

                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }
                }
            ).show()
            return
        }

        val message = buildString {
            append("Plan: ")
            append(plan.title ?: "Show Promotion")
            plan.subTitle?.takeIf { it.isNotBlank() }?.let {
                append("\n")
                append(it)
            }
            append("\n\nShow: ")
            append(showData?.title?.takeIf { it.isNotBlank() } ?: "Current Show")
            append("\n\nPayment method:\n")
            append("•••• •••• •••• ")
            append(defaultCard?.last4.orEmpty().ifEmpty { "----" })
            append("\nExpires ")
            append(defaultCard?.expMonth ?: "--")
            append("/")
            append(defaultCard?.expYear ?: "--")
            append("\n\nTotal: ")
            append(plan.price.asMoney())
        }

        AppBottomSheet(
            this,
            R.drawable.ic_payment_card,
            "Confirm Purchase",
            message,
            primaryBtnText = "Confirm Purchase",
            secondaryBtnText = "Cancel",
            canCancel = true,
            showSecondary = true,
            iconPadding = 16,
            alertType = AlertType.INFO,
            clicks = object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                    bind.loader.isVisible = true
                    viewModel.promoteShow(
                        viewModel.showId.toString().request(),
                        plan.id.toString().request(),
                        defaultCard?.cardId.orEmpty().request()
                    )
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

        val makeOfferSheet = Alerts.appBottomSheet(this, true, paymentAddressBind)

        with(paymentAddressBind.addressItem) {
            val hasAddress = App.profileResponse.value?.hasShippingAddress == true
            moreIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@ShowDetailsActivity,
                    draw.ic_pencil
                )
            )
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
                type.text = "Address Not Added"
                defaultAddress.isVisible = false
            }
            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@ShowDetailsActivity, MoreActivity::class.java).putExtra(
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
            moreIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@ShowDetailsActivity,
                    draw.ic_pencil
                )
            )
            moreIcon.rotation = 0f

            if (hasCard) {
                cardNumber.text = buildString {
                    append("•••• •••• •••• ")
                    append(App.profileResponse.value?.defaultCard?.last4)
                }

                expiryDate.text = buildString {
                    append(App.profileResponse.value?.defaultCard?.expMonth)
                    append("/")
                    append(App.profileResponse.value?.defaultCard?.expYear)
                }
            } else {
                cardNumber.text = "Cards Not Added"
            }
            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@ShowDetailsActivity, MoreActivity::class.java).putExtra(
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

    // Basecamp #9991482788 — co-host pairing dialog for the show owner.
    // Mirrors AgoraPublisherActivity.showCoHostPairingDialog() but operates from
    // the details screen (pre-show), using the scheduled show id from the ViewModel.
    private var coHostPairingId: Int? = null

    private fun showPairSecondDeviceDialog() {
        val scheduleShowId = viewModel.showId ?: ""
        if (scheduleShowId.isBlank()) {
            errorToast("Show ID not available")
            return
        }

        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_co_host_pairing, null, false)
        dialog.setContentView(view)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        val codeTv = view.findViewById<android.widget.TextView>(R.id.coHostCodeTv)
        val expiresTv = view.findViewById<android.widget.TextView>(R.id.coHostExpiresTv)
        val statusTv = view.findViewById<android.widget.TextView>(R.id.coHostStatusTv)
        val generateBtn = view.findViewById<android.widget.Button>(R.id.coHostGenerateBtn)
        val revokeBtn = view.findViewById<android.widget.Button>(R.id.coHostRevokeBtn)
        val closeBtn = view.findViewById<android.view.View>(R.id.coHostCloseBtn)

        closeBtn.setOnClickListener { dialog.dismiss() }
        coHostPairingId = null

        fun generate() {
            statusTv.text = "Generating code…"
            statusTv.setTextColor(android.graphics.Color.parseColor("#666666"))
            generateBtn.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val token = Prefs(this@ShowDetailsActivity).token()
                        val body = org.json.JSONObject().apply {
                            put("schedule_show_id", scheduleShowId.toIntOrNull() ?: 0)
                        }.toString()
                        val url = URL("${Const.BASE_URL}/api/product/co-host/pair")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.doOutput = true
                        conn.outputStream.use { os -> os.write(body.toByteArray()) }
                        val code = conn.responseCode
                        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                        val text = stream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        Pair(code, text)
                    } catch (e: Exception) {
                        Pair(-1, e.message ?: "error")
                    }
                }
                generateBtn.isEnabled = true
                try {
                    val json = org.json.JSONObject(result.second)
                    if (json.optString("status") == "success") {
                        val data = json.optJSONObject("data")
                        codeTv.text = data?.optString("pairing_code") ?: "——————"
                        val exp = data?.optString("expires_at") ?: ""
                        expiresTv.text = if (exp.isNotEmpty()) "Expires " + exp.take(16) else ""
                        coHostPairingId = data?.optInt("id")
                        revokeBtn.visibility = android.view.View.VISIBLE
                        statusTv.text = "Share this code with your second device."
                        statusTv.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                    } else {
                        statusTv.text = json.optString("message", "Could not generate code.")
                        statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                    }
                } catch (e: Exception) {
                    statusTv.text = "Could not generate code."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
            }
        }

        revokeBtn.setOnClickListener {
            val id = coHostPairingId ?: return@setOnClickListener
            statusTv.text = "Revoking…"
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val token = Prefs(this@ShowDetailsActivity).token()
                        val url = URL("${Const.BASE_URL}/api/product/co-host/$id")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "DELETE"
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        val rc = conn.responseCode
                        conn.disconnect()
                        rc in 200..299
                    } catch (e: Exception) { false }
                }
                if (ok) {
                    codeTv.text = "——————"
                    expiresTv.text = ""
                    revokeBtn.visibility = android.view.View.GONE
                    coHostPairingId = null
                    statusTv.text = "Pairing revoked."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#666666"))
                } else {
                    statusTv.text = "Could not revoke pairing."
                    statusTv.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                }
            }
        }

        generateBtn.setOnClickListener { generate() }
        dialog.show()
        generate()
    }

}
