package io.bidswipe.app.ui.product

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.skydoves.powermenu.PowerMenuItem
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.controller.ProductImageAdapter
import io.bidswipe.app.databinding.FragmentProductDetailsBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.share.Seller
import io.bidswipe.app.utils.share.ShareHelper
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged", "InflateParams")
class ProductDetailsFragment : BaseFragment<ProductViewModel, FragmentProductDetailsBinding>() {

    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentProductDetailsBinding.inflate(inflater, view, false)

    private var productId = ""
    private var offerList = mutableListOf<OfferModel>()
    private var actionList = mutableListOf<PowerMenuItem>()
    private var images = mutableListOf<String?>()

    private var productSaved = false

    lateinit var mediaAdapter: ProductImageAdapter

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = activity?.intent?.getStringExtra("productId") ?: ""

        bind.backImage.setHapticClickListener {
            finish()
        }

        actionList.clear()
        actionList.add(PowerMenuItem(title = "Save Product"))

        mediaAdapter = ProductImageAdapter(images)
        bind.recyclerView.adapter = mediaAdapter

        bind.buyNow.setHapticClickListener {
            findNavController().navigate(ids.goToBuyNowFragment)
        }

        bind.makeOffer.setHapticClickListener {
            showOfferSheet()
        }

        // Basecamp #9933847997 (2026-05-27): pre-bid button.
        bind.preBid.setHapticClickListener {
            showPreBidDialog()
        }

        bind.save.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.saveSellerProduct(productId.request())
        }

        bind.loader.isVisible = true
        viewModel.getProductDetails(productId.request())

        viewModel.getProductDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
//                    bind.loader.isVisible = false

                    val mData = it.value.data

                    viewModel.product = mData

                    bind.userName.text = mData?.user?.name?.asCapital()

                    viewModel.getSellerInfo(sellerId = mData?.userId.toString())

//                    if (mData?.user?.sellerVerification == true) {
//                        bind.sellerStatus.text = "Verified Seller"
//                    } else {
//                        bind.sellerStatus.text = "Unverified Seller"
//                    }

                    bind.productName.text = mData?.title?.asCapital()

                    bind.description.text = mData?.description?.asCapital()
                    bind.category.text = mData?.category?.name?.asCapital() ?: "Other"

                    bind.subCategoryLayout.isVisible = mData?.subCategory != null
                    if (mData?.subCategory != null) {
                        bind.subCategory.text = mData.subCategory.name ?: "Other"
                    }

                    bind.conditionLayout.isVisible =
                        mData?.productCondition.isNullOrEmpty() == false
                    if (!mData?.productCondition.isNullOrEmpty()) {
                        bind.condition.text = (mData.productCondition ?: "").replace("_", " ")
                    }

                    bind.quantity.text = buildSpannedString {
                        append(mData?.quantity.toString())
                        append(" Available")
                    }

                    // Basecamp #9933973683 (2026-05-27): flash sale active?
                    val flashActive = isFlashSaleActive(mData)
                    if (flashActive && mData?.flashSalePrice != null) {
                        bind.flashSaleBadgeRow.isVisible = true
                        startFlashCountdown(mData.flashSaleEndsAt)
                        bind.price.text = buildSpannedString {
                            val regular = mData.pricing.toString().asMoney()
                            color(ContextCompat.getColor(mCtx, R.color.outlineVariant)) {
                                // Strikethrough on regular price.
                                val start = length
                                append(regular)
                                // Apply strikethrough span manually below.
                            }
                            append(" ")
                            color(android.graphics.Color.parseColor("#DC2626")) {
                                bold { append(mData.flashSalePrice.toString().asMoney()) }
                            }
                            append(" + Shipping + Taxes")
                        }
                        // Add strikethrough span over the regular price.
                        val pricedText = bind.price.text
                        if (pricedText is android.text.Spannable) {
                            val end = mData.pricing.toString().asMoney().length
                            pricedText.setSpan(android.text.style.StrikethroughSpan(), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        bind.flashSaleBadgeRow.isVisible = false
                        stopFlashCountdown()
                        bind.price.text = buildSpannedString {
                            append("Starting at ")
                            color(ContextCompat.getColor(mCtx, R.color.onSurface)) {
                                append(mData?.pricing.toString().asMoney())
                            }
                            append(" + Shipping + Taxes")
                        }
                    }

                    val offer = mData?.offer
                    if (offer != null) {
//                        bind.offerLayout.isVisible = true

                        bind.offerHeading.text = buildSpannedString {
                            append("Offer ")
                            append(offer.status)
                        }

                        bind.offerPrice.text = offer.amount.toString().asMoney()

                        when (offer.status) {
                            "accepted" -> {
//                                bind.price.paintFlags =
//                                    bind.price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
//                                bind.price.setTextColor(
//                                    ContextCompat.getColor(
//                                        mCtx,
//                                        R.color.outlineVariant
//                                    )
//                                )
                                bind.makeOffer.isVisible = false
                            }

                            "rejected" -> {
                                bind.makeOffer.isVisible = mData.acceptOffers == true
                            }

                            else -> {
                                bind.makeOffer.isVisible = false
                            }
                        }

                    } else {
                        bind.offerLayout.isVisible = false
                        bind.makeOffer.isVisible = mData?.acceptOffers == true
                    }

                    bind.userImage.loadUrl(mCtx, mData?.user?.profileImage.toString())

                    bind.recyclerView.onFlingListener = null

                    images.clear()
                    images.addAll(mData?.videos ?: emptyList())
                    images.addAll(mData?.images ?: emptyList())
                    mediaAdapter.notifyDataSetChanged()

                    bind.indicatorv.attachTo(bind.recyclerView, true)

                    bind.posted.text = buildSpannedString {
                        bold {
                            append("Posted ")
                        }
                        append(Utils.getTimeAgo(mData?.createdAt ?: "", Const.DD_MM_YYYY_HH_MM_SS))
                    }

                    offerList.clear()

                    offerList.addAll(
                        listOf(
                            OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 20),
                                "20% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 15),
                                "15% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 10),
                                "10% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 5),
                                "5% off"
                            )
                        )
                    )

                    bind.share.setHapticClickListener {
                        bind.loader.isVisible = true
                        shareProduct(mData)

                    }

                    bind.chat.setHapticClickListener {
                        val intent = Intent(mCtx, ChatActivity::class.java).apply {
                            putExtra("id", mData?.userId.toString())
                            putExtra("name", mData?.user?.name ?: "")
                            putExtra("image", mData?.user?.profileImage ?: "")
                        }
                        startActivity(intent)
                    }

                    bind.buyLayout.isVisible = mData?.userId.toString() != userId

                    productSaved = mData?.productSaveStatus ?: false

                    bind.save.icon = ContextCompat.getDrawable(
                        mCtx,
                        if (productSaved) draw.ic_saved else draw.ic_save
                    )
                    bind.save.text = if (productSaved) "Saved" else "Save"

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

        viewModel.makeOfferRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    Alerts.success(mCtx, "Offer Sent")
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

        viewModel.saveSellerProductRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    productSaved = !productSaved

                    bind.save.icon = ContextCompat.getDrawable(
                        mCtx,
                        if (productSaved == true) draw.ic_saved else draw.ic_save
                    )
                    bind.save.text = if (productSaved == true) "Saved" else "Save"

//					Alerts.success(mCtx, it.value.message.toString())

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

        viewModel.getSellerInfoRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = it.value.data

                    bind.rating.text = (mData?.ratingAvg ?: 0).toString()
                    bind.review.text = (mData?.review ?: 0).toString()
                    bind.sold.text = (mData?.soldCount ?: 0).toString()
                    bind.shipping.text = (mData?.avgShip ?: 0).toString()
                    bind.userImage.loadUrl(mCtx, mData?.sellerDetails?.profileImage ?: "")

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

    fun shareProduct(mData: GetProductDetailsResponse.Data?, uri: Uri? = null) {
        val shareText = buildString {
            append("Check out this product ")
            append(mData?.title?.asCapital() ?: "")
            append(" by @${bind.userName.text}")
            append(" 🛍️✨\nGrab it here:\n")
            append("${Const.BASE_URL}/products/$productId")
        }

        val seller = Seller(
            mData?.userId.toString(),
            mData?.user?.profileImage,
            mData?.user?.name
        )

        ShareHelper.openShareSheet(
            parentFragmentManager,
            imageUrl = mData?.images?.first().orEmpty(),
            text = mData?.title?.asCapital().orEmpty(),
            sellerInfo = seller,
            shareText = shareText,
            type = "product"
        )

//        if (mData?.images?.isNotEmpty() == true) {
//            Utils.saveImageFromUrlToCache(mCtx, mData.images.first().orEmpty()) { imageUri ->
//                bind.loader.isVisible = false
//                shareProductDetails(imageUri)
//            }
//        } else {
//            bind.loader.isVisible = false
//            shareProductDetails(null)
//        }
    }

    fun getDiscountAmount(originalAmount: Double, percentOff: Int): String {
        val discountedAmount = originalAmount - (originalAmount * percentOff) / 100
        return discountedAmount.toString()
    }

    fun showOfferSheet() {
        val makeOfferSheetBind = MakeOfferSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.make_offer_sheet,
                null,
                false
            )
        )
        offerList.find { it.selected == true }?.selected = false
        val makeOfferSheet = Alerts.appBottomSheet(mCtx, true, makeOfferSheetBind)

        makeOfferSheetBind.listedPrice.text = viewModel.product?.pricing.toString().asMoney()

        makeOfferSheetBind.offerRecycler.adapter =
            MakeOfferAdapter(offerList, object : RecyclerClicks {

                override fun itemClick(pos: Int, status: String?) {

                    makeOfferSheetBind.customOffer.setText(offerList[pos].amount)

                    offerList.forEachIndexed { index, item ->
                        item.selected = index == pos
                    }

                    makeOfferSheetBind.offerRecycler.adapter?.notifyDataSetChanged()
                }
            })

        makeOfferSheetBind.close.setHapticClickListener {
            makeOfferSheet.dismiss()
        }

        makeOfferSheetBind.select.setHapticClickListener {

            if (makeOfferSheetBind.customOffer.value().isEmpty()) {
                Alerts.error(mCtx, "Please Enter Offer Amount")
            } else {
                hideKeyboard(it)
                makeOfferSheet.dismiss()
                bind.loader.isVisible = true
                viewModel.makeOffer(
                    makeOfferSheetBind.customOffer.value().request(),
                    productId.request()
                )
            }

        }
        makeOfferSheet.show()
    }

    private fun shareSellerProfile(productName: String, productImage: String) {
        val shareText = buildString {
            append("Check out $productName")
            append("Username: @${bind.userName.text}\n")
            append(productImage.takeIf { it.isNotEmpty() }?.let { "Profile image: $it" } ?: "")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share via")

        if (shareIntent.resolveActivity(mCtx.packageManager) != null) {
            startActivity(chooserIntent)
        } else {
            errorToast("No sharing apps available")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaAdapter.onDestroy()
        stopFlashCountdown()
    }

    // Basecamp #9933973683 (2026-05-27): flash sale helpers.
    private var flashCountdownRunnable: Runnable? = null
    private val flashHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    private fun isFlashSaleActive(d: io.bidswipe.app.network.response.GetProductDetailsResponse.Data?): Boolean {
        if (d == null) return false
        if (d.flashSale != true) return false
        if ((d.flashSalePrice ?: 0.0) <= 0.0) return false
        val ends = parseSqlOrIso(d.flashSaleEndsAt) ?: return false
        if (ends.before(java.util.Date())) return false
        val starts = parseSqlOrIso(d.flashSaleStartsAt)
        if (starts != null && starts.after(java.util.Date())) return false
        return true
    }

    private fun parseSqlOrIso(s: String?): java.util.Date? {
        if (s.isNullOrBlank()) return null
        val raw = s.trim()
        // Basecamp #9933973683 (2026-05-29): ROOT-CAUSE FIX for the flash-sale
        // price not showing. The Bidcast API's Product model overrides
        // serializeDate() to emit ALL datetimes as "d-m-Y H:i:s"
        // (e.g. "29-05-2026 10:47:00"). The old parser only tried
        // "yyyy-MM-dd HH:mm:ss" / ISO, so flashSaleEndsAt never parsed,
        // isFlashSaleActive was always false, and the price/badge stayed hidden.
        // Try the REAL backend format first, then the legacy fallbacks.
        val patterns = listOf(
            "dd-MM-yyyy HH:mm:ss",   // actual Product serializeDate() output
            "yyyy-MM-dd HH:mm:ss",   // legacy assumption
            "yyyy-MM-dd'T'HH:mm:ss"  // ISO without zone
        )
        for (p in patterns) {
            val fmt = java.text.SimpleDateFormat(p, java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fmt.isLenient = false
            try { return fmt.parse(raw.replace("Z", "").replace(".000000", "")) } catch (_: Exception) {}
        }
        return null
    }

    private fun startFlashCountdown(endsAt: String?) {
        val ends = parseSqlOrIso(endsAt) ?: return
        stopFlashCountdown()
        flashCountdownRunnable = object : Runnable {
            override fun run() {
                val diff = ends.time - System.currentTimeMillis()
                if (diff <= 0) {
                    bind.flashSaleCountdown.text = "Sale ended"
                    bind.flashSaleBadgeRow.isVisible = false
                    return
                }
                val total = diff / 1000
                val h = total / 3600
                val m = (total % 3600) / 60
                val sec = total % 60
                bind.flashSaleCountdown.text = if (h > 0) "${h}h ${m}m ${sec}s left" else "${m}m ${sec}s left"
                flashHandler.postDelayed(this, 1000)
            }
        }
        flashHandler.post(flashCountdownRunnable!!)
    }

    private fun stopFlashCountdown() {
        flashCountdownRunnable?.let { flashHandler.removeCallbacks(it) }
        flashCountdownRunnable = null
    }

    // Basecamp #9933847997 (2026-05-27): pre-bid dialog.
    private fun showPreBidDialog() {
        val ctx = requireContext()
        val input = android.widget.EditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Amount in USD"
            setPadding(40, 30, 40, 30)
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Place Pre-Bid")
            .setMessage("Lock in your bid before the auction starts. Applied automatically as the opening bid when the seller starts the auction.")
            .setView(input)
            .setPositiveButton("Place") { d, _ ->
                d.dismiss()
                val raw = input.text?.toString()?.trim() ?: ""
                val amount = raw.toDoubleOrNull() ?: 0.0
                if (amount < 1.0) {
                    io.bidswipe.app.utils.Alerts.error(mCtx, "Please enter $1 or more.")
                    return@setPositiveButton
                }
                postPreBid(amount)
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun postPreBid(amount: Double) {
        bind.loader.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val code: Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val token = io.bidswipe.app.utils.Prefs(mCtx).token()
                    val body = org.json.JSONObject().apply {
                        put("product_id", productId.toIntOrNull() ?: 0)
                        put("amount", amount)
                    }.toString()
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/pre-bid")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val rc = conn.responseCode
                    conn.disconnect()
                    rc
                } catch (e: Exception) { e.printStackTrace(); -1 }
            }
            bind.loader.isVisible = false
            when (code) {
                200, 201 -> io.bidswipe.app.utils.Alerts.success(mCtx, "Pre-bid placed.")
                403 -> io.bidswipe.app.utils.Alerts.error(mCtx, "Not allowed.")
                404 -> io.bidswipe.app.utils.Alerts.error(mCtx, "Product not found.")
                -1 -> io.bidswipe.app.utils.Alerts.error(mCtx, "Network error.")
                else -> io.bidswipe.app.utils.Alerts.error(mCtx, "Could not place pre-bid (code $code).")
            }
        }
    }
}


