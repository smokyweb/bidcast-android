package io.bidswipe.app.ui.product

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserAction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.skydoves.powermenu.PowerMenuItem
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.controller.ProductImageAdapter
import io.bidswipe.app.databinding.FragmentProductDetailsBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("NotifyDataSetChanged", "InflateParams")
class ProductDetailsFragment : BaseFragment<ProductViewModel, FragmentProductDetailsBinding>() {

    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentProductDetailsBinding.inflate(inflater, view, false)

    private var productId = ""
    private var offerList = mutableListOf<OfferModel>()
    private var actionList = mutableListOf<PowerMenuItem>()
    private var images = mutableListOf<String?>()

    lateinit var mediaAdapter: ProductImageAdapter

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = activity?.intent?.getStringExtra("productId") ?: ""

        bind.header.onBackClick {
            finish()
        }

        bind.backImage.setHapticClickListener {
            finish()
        }

        actionList.clear()
        actionList.add(PowerMenuItem(title = "Save Product"))

        mediaAdapter = ProductImageAdapter(images)
        bind.recyclerView.adapter = mediaAdapter

        val menu = PopupMenu(mCtx, bind.header.findViewById<AppCompatImageView>(R.id.primaryIcon))
        menu.menuInflater.inflate(R.menu.action_menu, menu.menu)

        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                ids.save_product -> {

                    bind.loader.isVisible = true

                    viewModel.saveSellerProduct(productId.request())

                }

            }
            return@setOnMenuItemClickListener true
        }

        bind.header.onMorePrimaryClick {
            menu.show()
        }

        bind.buyNow.setHapticClickListener {
            findNavController().navigate(ids.goToBuyNowFragment)
        }

        bind.makeOffer.setHapticClickListener {
            showOfferSheet()
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
                    bind.quantity.text = buildSpannedString {
                        append(mData?.quantity.toString())
                        append(" Available")
                    }

                    bind.price.text = buildSpannedString {
                        append("Starting at ")
                        color(ContextCompat.getColor(mCtx, R.color.onSurface)) {
                            append(mData?.pricing.toString().asMoney())
                        }
                        append(" + Shipping + Taxes")
                    }

                    val offer = mData?.offer
                    if (offer != null) {

                        bind.offerLayout.isVisible = true

                        bind.offerHeading.text = buildSpannedString {
                            append("Offer ")
                            append(offer.status)
                        }

                        bind.offerPrice.text = offer.amount.toString().asMoney()

                        when (offer.status) {
                            "accepted" -> {
                                bind.price.paintFlags =
                                    bind.price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                                bind.price.setTextColor(
                                    ContextCompat.getColor(
                                        mCtx,
                                        R.color.outlineVariant
                                    )
                                )
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
                    images.addAll(mData?.images ?: emptyList())
                    mediaAdapter.notifyDataSetChanged()

                    bind.indicatorv.attachTo(bind.recyclerView, true)

                    bind.posted.text =
                        Utils.getTimeAgo(mData?.createdAt ?: "", Const.DD_MM_YYYY_HH_MM_SS)

                    bind.address.text = mData?.shippingAdress?.streetAddress ?: "--"

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
                        if (mData?.images?.isNotEmpty() == true) {
                            saveImageFromUrlToCache(mData)
                        } else {
                            shareProduct(mData)
                        }
                    }

                    bind.chat.setHapticClickListener {
                        val intent = Intent(mCtx, ChatActivity::class.java).apply {
                            putExtra("id", mData?.userId.toString())
                            putExtra("name", mData?.user?.name ?: "")
                            putExtra("image", mData?.user?.profileImage ?: "")
                        }
                        startActivity(intent)
                    }
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

                    it.value.data

                    Alerts.success(mCtx, it.value.message.toString())

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
        FireRef.CHAT_LIST.child(userId).orderByChild("timestamp")
            .addValueEventListener(mValueEventListener)
    }

    private val chatList = mutableListOf<ChatModel>()
    private var mValueEventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snap: DataSnapshot) {

            chatList.clear()

            snap.children.forEach {
                val chat = ChatModel().fromMap(it)
                chatList.add(chat)
            }
            chatList.reverse()
        }

        override fun onCancelled(error: DatabaseError) {
            error.toException().printStackTrace()
        }
    }

    fun saveImageFromUrlToCache(mData: GetProductDetailsResponse.Data?) {
        bind.loader.isVisible = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(mData?.images?.first() ?: "")
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream: InputStream = connection.inputStream
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                val cacheDir: File = mCtx.cacheDir

                val imageFile = File(cacheDir, "cached_image_${System.currentTimeMillis()}.jpg")

                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                val uri = FileProvider.getUriForFile(
                    mCtx,
                    "${mCtx.packageName}.provider",
                    imageFile
                )

                withContext(Dispatchers.Main) {
                    bind.loader.isVisible = false

                    shareProduct(mData, uri)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun shareProduct(mData: GetProductDetailsResponse.Data?, uri: Uri? = null) {
        // Use a plain String so all apps (WhatsApp, Instagram, etc.) can consume it safely
        val shareText = buildString {
            append("Check out ")
            append(mData?.title?.asCapital() ?: "")
            append(" from Bidswipe by ")
            append("@${bind.userName.text}\n${Const.BASE_URL}/products/$productId")
        }

        // Base share intent
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                data=uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        // List of custom actions (like 'Search' and chat contacts)
        val customActions = mutableListOf(
            ChooserAction.Builder(
                Icon.createWithResource(context, R.drawable.search),
                "Search",
                PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(Intent.ACTION_VIEW),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        // Add chat contact actions
        chatList.forEach { item ->
            val recId = if (item.users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
                item.users?.receiverId ?: ""
            } else {
                item.users?.senderId ?: ""
            }

            val (receiverName, receiverImage) = if (item.users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
                item.users?.receiverName?.asCapital() to (item.users?.receiverImage ?: "")
            } else {
                item.users?.senderName?.asCapital() to (item.users?.senderImage ?: "")
            }

            customActions.add(
                ChooserAction.Builder(
                    Icon.createWithResource(mCtx, R.drawable.user_image),
                    receiverName ?: "",
                    PendingIntent.getActivity(
                        context,
                        1,
                        Intent(mCtx, ChatActivity::class.java).apply {
                            putExtra("id", recId)
                            putExtra("name", receiverName)
                            putExtra("image", receiverImage)
                            putExtra("productImage", uri)
                            putExtra("shareText", shareText)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    )
                ).build()
            )
        }

        // Query apps that can handle exactly this share intent
        val resolvedActivities =
            mCtx.packageManager.queryIntentActivities(sendIntent, PackageManager.MATCH_ALL)

        val priorityList = listOf(
            "com.android.mms",
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.whatsapp"
        )

        val sortedResolveInfoList = resolvedActivities.sortedBy { resolveInfo ->
            when (resolveInfo.activityInfo.packageName) {
                in priorityList -> 0
                else -> 1
            }
        }

        val initialIntents = sortedResolveInfoList.map { resolveInfo ->
            Intent(Intent.ACTION_SEND).apply {
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/*"
                    data=uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                setPackage(resolveInfo.activityInfo.packageName)
            }
        }.toTypedArray()

        if (uri != null) {
            sortedResolveInfoList.forEach { resolveInfo ->
                try {
                    mCtx.grantUriPermission(
                        resolveInfo.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
            }
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share Product")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents)
        chooserIntent.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, customActions.toTypedArray())

        mCtx.startActivity(chooserIntent)
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
    }
}


