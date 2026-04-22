package io.bidswipe.app.ui.tutorials

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.agora.rtc2.Constants
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.FirebaseProductAdapter
import io.bidswipe.app.databinding.ActivityAgoraPublisherBinding
import io.bidswipe.app.databinding.FragmentProductsForLiveShowBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import kotlin.random.Random

class LiveRehearsalActivity : BaseActivity() {

    private val bind by bind(ActivityAgoraPublisherBinding::inflate)
    private val handler = Handler(Looper.getMainLooper())
    private val commentList = mutableListOf<LiveChatModel?>()

    private lateinit var commentAdapter: CommentAdapter

    private var elapsedSeconds = 0
    private var isRehearsalStarted = false
    private var isAuctionStarted = false
    private var productIndex = 0
    private var bidValue = 14.0
    private val staticProducts = mutableListOf<Product?>()

    private val demoProducts = mutableListOf(
        DemoProduct(
            title = "Unnu",
            category = "Wildlife",
            quantity = "Quantity: 1",
            price = "$93.00 + Shipping + Taxes",
            image = "https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=800"
        ),
        DemoProduct(
            title = "Vintage Denim Jacket",
            category = "Fashion",
            quantity = "Quantity: 3",
            price = "$24.00 + Shipping + Taxes",
            image = "https://images.unsplash.com/photo-1543076447-215ad9ba6923?w=800"
        ),
        DemoProduct(
            title = "Retro Sneakers",
            category = "Footwear",
            quantity = "Quantity: 2",
            price = "$31.00 + Shipping + Taxes",
            image = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800"
        ),
        DemoProduct(
            title = "Leather Crossbody Bag",
            category = "Accessories",
            quantity = "Quantity: 1",
            price = "$42.00 + Shipping + Taxes",
            image = "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=800"
        )
    )

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isRehearsalStarted) return
            elapsedSeconds += 1
            bind.duration.text = "Rehearsal Time: ${formatElapsed(elapsedSeconds)}"

            if (elapsedSeconds % 4 == 0) {
                if (isAuctionStarted) {
                    bidValue += Random.nextInt(1, 4)
                    bind.bidPrice.text = bidValue.toString().asMoney()
                    bind.winningLayout.isVisible = true
                    bind.winning.text = "Sample Buyer is winning!"
                }
            }
            if (elapsedSeconds % 7 == 0) {
                val count = Random.nextInt(1, 12)
                bind.liveCount.text = count.toString()
            }
            if (elapsedSeconds % 5 == 0 && isAuctionStarted) {
                addSystemComment("Practice bid updated to ${bidValue.toString().asMoney()}")
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.controlsView.setPadding(0, system.top, 0, system.bottom)
            insets
        }

        setupBaseUi()
        setupListeners()
        setupCameraPreview()
    }

    private fun setupBaseUi() {
        seedStaticProducts()
        bind.hostName.text = userName.asCapital()
        bind.hostImage.loadUrl(this, userImage)
        bind.liveCount.text = "0"
        bind.duration.text = "Rehearsal Time: 00:00:00"
        bind.startBtn.text = "Start Rehearsal"
        bind.runNext.isVisible = false
        bind.product.isVisible = false
        bind.poll.isVisible = false
        bind.showNotes.isVisible = false
        bind.freebieLayout.isVisible = false
        bind.promote.isVisible = false
        bind.share.isVisible = false
        bind.clip.isVisible = false

        commentAdapter = CommentAdapter(commentList, userId, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) = Unit
        })
        bind.recycler.adapter = commentAdapter
    }

    private fun setupListeners() {
        bind.startBtn.setHapticClickListener {
            if (isRehearsalStarted) return@setHapticClickListener
            isRehearsalStarted = true
            bind.startBtn.isVisible = false
            bind.message.isVisible = true
            addSystemComment("Rehearsal started. Tap Shop to start your demo auction.")
            handler.post(timerRunnable)
        }

        bind.runNext.setHapticClickListener {
            if (!isRehearsalStarted) {
                Alerts.error(this, "Please start rehearsal first")
                return@setHapticClickListener
            }
            if (!isAuctionStarted) {
                Alerts.error(this, "Start an auction from Shop first")
                return@setHapticClickListener
            }
            productIndex = (productIndex + 1) % demoProducts.size
            startDemoAuction(productIndex)
            bidValue = demoProducts.getOrNull(productIndex)?.price
                ?.substringAfter("$")
                ?.substringBefore(" ")
                ?.toDoubleOrNull() ?: bidValue
            addSystemComment("Moved to next product simulation.")
        }

        bind.cameraSwitch.setHapticClickListener {
            io.bidswipe.app.App.manager.switchCamera {
            }
        }

        bind.more.setHapticClickListener {
            Alerts.success(this, "Rehearsal mode keeps networking static")
        }

        bind.shop.setHapticClickListener {
            if (!isRehearsalStarted) {
                Alerts.error(this, "Please start rehearsal first")
                return@setHapticClickListener
            }
            showDummyProductSheet()
        }

        bind.message.setEndIconOnClickListener {
            val message = bind.messageText.value().trim()
            if (message.isNotEmpty()) {
                commentList.add(LiveChatModel(userImage, userName, userId, message))
                commentAdapter.notifyItemInserted(commentList.lastIndex)
                bind.recycler.scrollToPosition(commentList.lastIndex)
                bind.messageText.setText("")
            }
        }

        bind.cutButton.setHapticClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun setDemoProduct(index: Int) {
        val item = demoProducts.getOrNull(index) ?: return
        bind.product.isVisible = true
        bind.productLayout.isVisible = true
        bind.winningLayout.isVisible = false
        bind.status.isVisible = false
        bind.bidTime.isVisible = false
        bind.itemsLeftProgress.isVisible = false
        bind.productName.text = item.title
        bind.productCategory.text = item.category
        bind.quantity.text = item.quantity
        bind.price.text = item.price
        bind.bidPrice.text = "$0.00"
        bind.productImage.loadUrl(this, item.image)
        bind.productImageShop.loadUrl(this, item.image)
        bind.countBadge.text = demoProducts.size.toString()
    }

    private fun startDemoAuction(index: Int) {
        isAuctionStarted = true
        bind.runNext.isVisible = true
        setDemoProduct(index)
        bidValue = demoProducts.getOrNull(index)?.price
            ?.substringAfter("$")
            ?.substringBefore(" ")
            ?.toDoubleOrNull() ?: bidValue
        bind.bidPrice.text = bidValue.toString().asMoney()
        addSystemComment("Auction started for ${demoProducts[index].title}")
    }

    private fun showDummyProductSheet() {
        val sheetBind = FragmentProductsForLiveShowBinding.bind(
            layoutInflater.inflate(R.layout.fragment_products_for_live_show, null, false)
        )
        val sheet = Alerts.appBottomSheet(this, true, sheetBind)

        // Ensure sheet opens large/expanded so recycler area is visible.
        val targetHeight = (resources.displayMetrics.heightPixels * 0.82f).toInt()
        sheetBind.root.layoutParams = (sheetBind.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            targetHeight
        )).apply {
            height = targetHeight
        }

        val allProducts = staticProducts
        val filteredProducts = allProducts.toMutableList()
        var selectedFilter = "Auction"

        sheetBind.switcher.displayedChild = 0
        sheetBind.loader.isVisible = false
        sheetBind.bottomLoader.isVisible = false
        sheetBind.surpriseBottomLoader.isVisible = false
        sheetBind.surpriseRecycler.isVisible = false
        sheetBind.surpriseNoDataView.isVisible = false
        sheetBind.addBtn.text = "Add Product"

        val adapter = FirebaseProductAdapter(from = "live_show", mList = filteredProducts, mClicks = object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                if (status == "start_auction" || status == "select") {
                    val selected = filteredProducts.getOrNull(pos) ?: return
                    val originalIndex = allProducts.indexOfFirst { it?.id == selected?.id }
                    if (originalIndex >= 0 && originalIndex < demoProducts.size) {
                        productIndex = originalIndex
                        startDemoAuction(originalIndex)
                        sheet.dismiss()
                    } else {
                        val selectedDemo = DemoProduct(
                            title = selected?.title.orEmpty(),
                            category = selected?.category?.name.orEmpty(),
                            quantity = "Quantity: ${selected?.quantity ?: "1"}",
                            price = "$${selected?.pricing ?: "0.00"} + Shipping + Taxes",
                            image = selected?.images?.firstOrNull().orEmpty()
                        )
                        demoProducts.add(0, selectedDemo)
                        productIndex = 0
                        startDemoAuction(productIndex)
                        sheet.dismiss()
                    }
                }
            }
        })
        sheetBind.recycler.layoutManager = LinearLayoutManager(this)
        sheetBind.recycler.adapter = adapter

        fun applyFilters() {
            val query = sheetBind.search.value().trim().lowercase()
            filteredProducts.clear()
            val filterItems = allProducts.filter { product ->
                val byType = when (selectedFilter) {
                    "Auction" -> product?.auction == true
                    "Buy Now" -> product?.type == "buy_now"
                    "Sold" -> product?.status == "inactive"
                    "Offers" -> product?.acceptOffers == true
                    else -> true
                }
                val byQuery = query.isEmpty() || (product?.title?.lowercase()?.contains(query) == true)
                byType && byQuery
            }
            filteredProducts.addAll(filterItems)
            adapter.notifyDataSetChanged()
            sheetBind.noDataView.isVisible = filteredProducts.isEmpty()
            sheetBind.recycler.isVisible = filteredProducts.isNotEmpty()
        }

        val chipTitles = listOf("Auction", "Buy Now", "Sold", "Offers")
        sheetBind.chipGroup.removeAllViews()
        chipTitles.forEachIndexed { index, title ->
            val chip = Utils.makeAChip(this, text = title, selected = index == 0, closeIconVisible = false)
            chip.setOnClickListener {
                selectedFilter = title
                chipTitles.forEachIndexed { i, _ ->
                    val existing = sheetBind.chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip
                    existing?.isChecked = i == index
                }
                applyFilters()
            }
            sheetBind.chipGroup.addView(chip)
        }

        sheetBind.search.setOnItemClickListener { _, _, _, _ ->
            applyFilters()
        }
        sheetBind.search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilters()
            }
        })
        sheetBind.search.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, allProducts.mapNotNull { it?.title }))

        sheetBind.addBtn.setHapticClickListener {
            val newId = (allProducts.maxOfOrNull { it?.id ?: 1000 } ?: 1000) + 1
            val newProduct = Product(
                acceptOffers = false,
                auction = true,
                bidCount = 0,
                category = Product.Category(null, 99, null, "Rehearsal", null),
                createdAt = null,
                description = null,
                flashSale = false,
                hazardousMaterial = false,
                height = null,
                id = newId,
                images = listOf("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800"),
                length = null,
                mailClass = null,
                pricing = "19.00",
                processingCategory = null,
                productCondition = null,
                productShow = null,
                purchasedQuantity = null,
                quantity = "1",
                reserveForLive = false,
                shippingProfileId = null,
                sku = null,
                status = "active",
                subCategoryId = null,
                thumbnail = null,
                title = "Practice Product $newId",
                type = "auction",
                user = null,
                userId = null,
                variant = null,
                videos = null,
                weight = null,
                width = null
            )
            allProducts.add(0, newProduct)
            demoProducts.add(
                0,
                DemoProduct(
                    title = newProduct.title.orEmpty(),
                    category = newProduct.category?.name.orEmpty(),
                    quantity = "Quantity: ${newProduct.quantity ?: "1"}",
                    price = "$${newProduct.pricing ?: "0.00"} + Shipping + Taxes",
                    image = newProduct.images?.firstOrNull().orEmpty()
                )
            )
            sheetBind.search.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, allProducts.mapNotNull { it?.title }))
            Alerts.success(this, "Dummy product added")
            applyFilters()
        }

        sheetBind.close.setHapticClickListener {
            sheet.dismiss()
        }
        applyFilters()

        sheet.setOnShowListener { dialog ->
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = targetHeight
            }
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.peekHeight = targetHeight
        }
        sheet.show()
    }

    private fun seedStaticProducts() {
        if (staticProducts.isNotEmpty()) return
        staticProducts.add(
            Product(
                acceptOffers = false,
                auction = true,
                bidCount = 0,
                category = Product.Category(null, 1, null, "Wildlife", null),
                createdAt = null,
                description = null,
                flashSale = false,
                hazardousMaterial = false,
                height = null,
                id = 1001,
                images = listOf("https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=800"),
                length = null,
                mailClass = null,
                pricing = "93.00",
                processingCategory = null,
                productCondition = null,
                productShow = null,
                purchasedQuantity = null,
                quantity = "1",
                reserveForLive = false,
                shippingProfileId = null,
                sku = null,
                status = "active",
                subCategoryId = null,
                thumbnail = null,
                title = "Unnu",
                type = "auction",
                user = null,
                userId = null,
                variant = null,
                videos = null,
                weight = null,
                width = null
            )
        )
        staticProducts.add(
            Product(
                acceptOffers = false,
                auction = true,
                bidCount = 0,
                category = Product.Category(null, 2, null, "Fashion", null),
                createdAt = null,
                description = null,
                flashSale = false,
                hazardousMaterial = false,
                height = null,
                id = 1002,
                images = listOf("https://images.unsplash.com/photo-1543076447-215ad9ba6923?w=800"),
                length = null,
                mailClass = null,
                pricing = "24.00",
                processingCategory = null,
                productCondition = null,
                productShow = null,
                purchasedQuantity = null,
                quantity = "3",
                reserveForLive = false,
                shippingProfileId = null,
                sku = null,
                status = "active",
                subCategoryId = null,
                thumbnail = null,
                title = "Vintage Denim Jacket",
                type = "auction",
                user = null,
                userId = null,
                variant = null,
                videos = null,
                weight = null,
                width = null
            )
        )
        staticProducts.add(
            Product(
                acceptOffers = false,
                auction = true,
                bidCount = 0,
                category = Product.Category(null, 3, null, "Accessories", null),
                createdAt = null,
                description = null,
                flashSale = false,
                hazardousMaterial = false,
                height = null,
                id = 1003,
                images = listOf("https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=800"),
                length = null,
                mailClass = null,
                pricing = "42.00",
                processingCategory = null,
                productCondition = null,
                productShow = null,
                purchasedQuantity = null,
                quantity = "1",
                reserveForLive = false,
                shippingProfileId = null,
                sku = null,
                status = "active",
                subCategoryId = null,
                thumbnail = null,
                title = "Leather Crossbody Bag",
                type = "auction",
                user = null,
                userId = null,
                variant = null,
                videos = null,
                weight = null,
                width = null
            )
        )
    }

    private fun setupCameraPreview() {
        requestPerms(Const.PERMISSIONS) { granted ->
            if (!granted) {
                Alerts.error(this, "Camera and mic permissions are required for rehearsal")
                return@requestPerms
            }
            io.bidswipe.app.App.manager = AgoraManager(this, Const.APP_ID_AGORA)
            io.bidswipe.app.App.manager.initializeAgoraSDK(Constants.CLIENT_ROLE_BROADCASTER)
            bind.publisherView.removeAllViews()
            io.bidswipe.app.App.manager.setupPublisherView(bind.publisherView)
        }
    }

    private fun addSystemComment(text: String) {
        commentList.add(LiveChatModel(userImage, "Coach", "coach", text))
        commentAdapter.notifyItemInserted(commentList.lastIndex)
        bind.recycler.scrollToPosition(commentList.lastIndex)
    }

    private fun formatElapsed(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        io.bidswipe.app.App.manager.destroyEngine()
        super.onDestroy()
    }

    data class DemoProduct(
        val title: String,
        val category: String,
        val quantity: String,
        val price: String,
        val image: String
    )
}
