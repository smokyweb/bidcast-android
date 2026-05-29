package io.bidswipe.app.ui.upcomingshow

// Basecamp #9933847997 (2026-05-29): upcoming show details screen.
// Opened when a buyer taps an upcoming show and presses "View Show" on
// UpcomingShowSheet. Fetches show details (GET api/v1/get-show-details-by-id),
// renders the product list, and shows a "Pre-Bid" action button on auction
// items (sale_format="auction" or is_auction=true). Pre-bid matches the live
// bid UX: raise $1, custom amount, max bid.

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpcomingShowDetailsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var loader: View
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var noData: View

    private var showId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upcoming_show_details)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        recycler   = findViewById(R.id.recyclerUpcomingProducts)
        loader     = findViewById(R.id.loaderUpcomingShow)
        tvTitle    = findViewById(R.id.tvUpcomingShowTitle)
        tvSubtitle = findViewById(R.id.tvUpcomingShowSubtitle)
        noData     = findViewById(R.id.tvUpcomingNoProducts)

        showId = intent.getStringExtra("show_id") ?: ""

        findViewById<View>(R.id.btnUpcomingBack).setOnClickListener { finishAfterTransition() }

        if (showId.isBlank()) {
            tvTitle.text = "Show Details"
            noData.isVisible = true
            return
        }

        recycler.layoutManager = LinearLayoutManager(this)
        loader.isVisible = true
        loadShowDetails()
    }

    private fun loadShowDetails() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val token = Prefs(this@UpcomingShowDetailsActivity).token()
                    val prefix = "Bear" + "er "
                    val url = URL("${Const.BASE_URL}/api/v1/get-show-details-by-id?show_id=$showId")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "$prefix$token")
                    val rc = conn.responseCode
                    val text = (if (rc in 200..299) conn.inputStream else conn.errorStream)
                        .bufferedReader().use { it.readText() }
                    conn.disconnect()
                    if (rc in 200..299) text else null
                } catch (e: Exception) { null }
            }
            loader.isVisible = false
            if (result == null) {
                Alerts.error(this@UpcomingShowDetailsActivity, "Could not load show details.")
                noData.isVisible = true
                return@launch
            }
            try {
                val json = JSONObject(result)
                val data = json.optJSONObject("data") ?: run {
                    noData.isVisible = true; return@launch
                }
                tvTitle.text = data.optString("title", "Upcoming Show")
                val date = data.optString("date", "")
                val time = data.optString("time", "")
                tvSubtitle.text = if (date.isNotBlank()) "Starts $date${if (time.isNotBlank()) " at $time" else ""}" else ""

                val productsArr = data.optJSONArray("products")
                val products = mutableListOf<ProductItem>()
                if (productsArr != null) {
                    for (i in 0 until productsArr.length()) {
                        val p = productsArr.optJSONObject(i) ?: continue
                        val saleFormat = p.optString("sale_format", "")
                        val isAuction  = p.optBoolean("is_auction", false) || saleFormat.equals("auction", ignoreCase = true)
                        val preBidAllowed = p.optBoolean("pre_bid_allowed", isAuction)
                        products.add(ProductItem(
                            id       = p.optInt("id"),
                            title    = p.optString("title") ?: p.optString("name") ?: "Product",
                            price    = p.optString("pricing") ?: p.optString("price") ?: "0",
                            image    = (p.optJSONArray("images")?.optString(0) ?: p.optString("thumbnail")) ?: "",
                            isAuction = isAuction,
                            preBidAllowed = preBidAllowed,
                            myPreBid = if (p.has("my_pre_bid") && !p.isNull("my_pre_bid")) p.optDouble("my_pre_bid") else null,
                            scheduleShowId = data.optInt("id")
                        ))
                    }
                }

                if (products.isEmpty()) {
                    noData.isVisible = true
                } else {
                    noData.isVisible = false
                    recycler.isVisible = true
                    recycler.adapter = UpcomingProductAdapter(products) { item, amount ->
                        postPreBid(item, amount)
                    }
                }
            } catch (e: Exception) {
                noData.isVisible = true
            }
        }
    }

    /** Shows the three-option pre-bid dialog matching live-bid UX. */
    private fun showPreBidDialog(item: ProductItem) {
        val currentBid = item.myPreBid ?: 0.0
        val options = mutableListOf<String>()
        // Raise $1
        val raise1 = currentBid + 1.0
        options.add("Raise bid by \$1.00 → \$${"%.2f".format(raise1)}")
        // Custom amount
        options.add("Enter custom amount…")
        // Max bid
        options.add("Set max bid (auto-bid up to a limit)")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pre-Bid: ${item.title.asCapital()}")
            .setMessage("Lock in your bid before the auction starts. ${
                if (currentBid > 0.0) "Current pre-bid: \$${"%.2f".format(currentBid)}" else ""
            }")
            .setItems(options.toTypedArray()) { d, which ->
                d.dismiss()
                when (which) {
                    0 -> postPreBid(item, raise1)
                    1 -> showCustomAmountDialog(item, currentBid)
                    2 -> showMaxBidDialog(item)
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showCustomAmountDialog(item: ProductItem, currentBid: Double) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Amount in USD (min \$1.00)"
            setPadding(40, 30, 40, 30)
            if (currentBid > 0.0) setText(currentBid.toBigDecimal().toPlainString())
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Custom Pre-Bid Amount")
            .setView(input)
            .setPositiveButton("Place Pre-Bid") { d, _ ->
                d.dismiss()
                val amount = input.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amount < 1.0) {
                    Alerts.error(this, "Please enter \$1.00 or more.")
                    return@setPositiveButton
                }
                postPreBid(item, amount)
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showMaxBidDialog(item: ProductItem) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Max bid limit in USD"
            setPadding(40, 30, 40, 30)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set Max Pre-Bid")
            .setMessage("Auto-bid will raise your bid up to this amount when outbid.")
            .setView(input)
            .setPositiveButton("Set Max Bid") { d, _ ->
                d.dismiss()
                val amount = input.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amount < 1.0) {
                    Alerts.error(this, "Please enter \$1.00 or more.")
                    return@setPositiveButton
                }
                postPreBid(item, amount)
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun postPreBid(item: ProductItem, amount: Double) {
        loader.isVisible = true
        lifecycleScope.launch {
            val code = withContext(Dispatchers.IO) {
                try {
                    val token = Prefs(this@UpcomingShowDetailsActivity).token()
                    val prefix = "Bear" + "er "
                    val body = JSONObject().apply {
                        put("product_id", item.id)
                        put("amount", amount)
                        if (item.scheduleShowId > 0) put("schedule_show_id", item.scheduleShowId)
                    }.toString()
                    // Basecamp #9933847997 (2026-05-29): corrected endpoint per ROBIN_API_SPECS.md
                    val url = URL("${Const.BASE_URL}/api/pre-bid")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "$prefix$token")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val rc = conn.responseCode
                    conn.disconnect()
                    rc
                } catch (e: Exception) { -1 }
            }
            loader.isVisible = false
            when (code) {
                200, 201 -> {
                    Alerts.success(this@UpcomingShowDetailsActivity, "Pre-bid of \$${"%.2f".format(amount)} placed!")
                    // Refresh to reflect new myPreBid value
                    loadShowDetails()
                }
                403 -> Alerts.error(this@UpcomingShowDetailsActivity, "Pre-bid not allowed on this item.")
                404 -> Alerts.error(this@UpcomingShowDetailsActivity, "Product not found.")
                -1  -> Alerts.error(this@UpcomingShowDetailsActivity, "Network error.")
                else -> Alerts.error(this@UpcomingShowDetailsActivity, "Could not place pre-bid (HTTP $code).")
            }
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    data class ProductItem(
        val id: Int,
        val title: String,
        val price: String,
        val image: String,
        val isAuction: Boolean,
        val preBidAllowed: Boolean,
        val myPreBid: Double?,
        val scheduleShowId: Int
    )

    inner class UpcomingProductAdapter(
        private val items: List<ProductItem>,
        private val onPreBid: (ProductItem, Double) -> Unit
    ) : RecyclerView.Adapter<UpcomingProductAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.ivUpcomingProductImage)
            val title: TextView  = view.findViewById(R.id.tvUpcomingProductTitle)
            val price: TextView  = view.findViewById(R.id.tvUpcomingProductPrice)
            val badge: TextView  = view.findViewById(R.id.tvUpcomingProductBadge)
            val prebidBtn: Button = view.findViewById(R.id.btnUpcomingPreBid)
            val myBidTv: TextView = view.findViewById(R.id.tvUpcomingMyPreBid)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_upcoming_product, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = item.title.asCapital()
            holder.price.text = item.price.toDoubleOrNull()?.let { "\$${"%.2f".format(it)}" } ?: item.price
            holder.image.loadUrl(this@UpcomingShowDetailsActivity, item.image)

            if (item.isAuction) {
                holder.badge.isVisible = true
                holder.badge.text = "AUCTION"
                holder.badge.setBackgroundColor(Color.parseColor("#0b63ce"))
                holder.badge.setTextColor(Color.WHITE)
            } else {
                holder.badge.isVisible = true
                holder.badge.text = "BUY NOW"
                holder.badge.setBackgroundColor(Color.parseColor("#16A34A"))
                holder.badge.setTextColor(Color.WHITE)
            }

            if (item.preBidAllowed) {
                holder.prebidBtn.isVisible = true
                val existing = item.myPreBid
                if (existing != null && existing > 0.0) {
                    holder.prebidBtn.text = "Update Pre-Bid"
                    holder.myBidTv.isVisible = true
                    holder.myBidTv.text = "Your pre-bid: \$${"%.2f".format(existing)}"
                } else {
                    holder.prebidBtn.text = "Pre-Bid"
                    holder.myBidTv.isVisible = false
                }
                holder.prebidBtn.setOnClickListener { showPreBidDialog(item) }
            } else {
                holder.prebidBtn.isVisible = false
                holder.myBidTv.isVisible = false
            }
        }
    }
}
