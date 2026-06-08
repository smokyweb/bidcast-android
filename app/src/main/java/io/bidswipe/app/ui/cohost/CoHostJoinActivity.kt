package io.bidswipe.app.ui.cohost

// Basecamp #9934001770 (2026-05-27): co-host claim screen. Second device
// enters the 6-char pairing code generated on the primary device, claims
// via POST /api/product/co-host/claim, and on success goes to the live show
// as a co-host.

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.bidswipe.app.databinding.ActivityCoHostJoinBinding
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CoHostJoinActivity : AppCompatActivity() {
    private lateinit var bind: ActivityCoHostJoinBinding
    private data class InviteProduct(val id: Int, val title: String, val subtitle: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityCoHostJoinBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val inviteId = intent.getStringExtra("cohost_invite_id").orEmpty()
        if (inviteId.isNotBlank()) {
            setupInviteFlow(inviteId)
            return
        }

        bind.coHostCodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val cur = s?.toString() ?: return
                val cleaned = cur.uppercase().filter { it.isLetterOrDigit() }.take(6)
                if (cleaned != cur) {
                    bind.coHostCodeInput.setText(cleaned)
                    bind.coHostCodeInput.setSelection(cleaned.length)
                }
            }
        })

        bind.coHostCancelBtn.setOnClickListener { finish() }

        bind.coHostJoinBtn.setOnClickListener {
            val code = bind.coHostCodeInput.text?.toString()?.trim()?.uppercase() ?: ""
            if (code.length < 4) {
                showStatus("Enter the 6-character code from your primary device.", Color.parseColor("#DC2626"))
                return@setOnClickListener
            }
            bind.coHostJoinBtn.isEnabled = false
            bind.coHostJoinBtn.text = "Joining…"
            showStatus("", Color.parseColor("#666666"))
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val token = Prefs(this@CoHostJoinActivity).token()
                        val body = JSONObject().put("pairing_code", code).toString()
                        val url = URL("${Const.BASE_URL}/api/product/co-host/claim")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.doOutput = true
                        conn.outputStream.use { it.write(body.toByteArray()) }
                        val rc = conn.responseCode
                        val stream = if (rc in 200..299) conn.inputStream else conn.errorStream
                        val text = stream.bufferedReader().use { it.readText() }
                        conn.disconnect()
                        Pair(rc, text)
                    } catch (e: Exception) {
                        Pair(-1, e.message ?: "error")
                    }
                }
                bind.coHostJoinBtn.isEnabled = true
                bind.coHostJoinBtn.text = "Join Show"
                try {
                    val json = JSONObject(result.second)
                    val status = json.optString("status")
                    if (status == "success") {
                        showStatus("Paired! Opening show…", Color.parseColor("#16A34A"))
                        // Basecamp #9934001770 (2026-05-29): extract show and
                        // pairing data so the co-host device can build the
                        // correct Agora channel name (live_room_{host_uid}_{show_id})
                        // and revoke the pairing on leave.
                        val data = json.optJSONObject("data")
                        val show = data?.optJSONObject("show")
                        val showId = show?.optInt("id") ?: 0
                        // show.user_id = the primary host's user ID — required to
                        // build the Agora channel name matching the host's channel.
                        val hostUserId = show?.optString("user_id")
                            ?: show?.optInt("user_id")?.takeIf { it != 0 }?.toString() ?: ""
                        val pairingId = data?.optInt("id") ?: 0
                        val intent = Intent(this@CoHostJoinActivity, AgoraPublisherActivity::class.java)
                        intent.putExtra("show_id", showId.toString())
                        intent.putExtra("co_host", true)
                        intent.putExtra("host_user_id", hostUserId)
                        intent.putExtra("pairing_id", pairingId)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        showStatus(json.optString("message", "Could not claim pairing."), Color.parseColor("#DC2626"))
                    }
                } catch (e: Exception) {
                    showStatus("Network error.", Color.parseColor("#DC2626"))
                }
            }
        }
    }

    private fun showStatus(msg: String, color: Int) {
        bind.coHostStatusTv.text = msg
        bind.coHostStatusTv.setTextColor(color)
    }

    private fun setupInviteFlow(inviteId: String) {
        renderInviteLoading("Loading invite...")
        lifecycleScope.launch {
            val result = apiRequest("GET", "${Const.BASE_URL}/api/product/co-host/invite/$inviteId")
            if (result.first !in 200..299) {
                renderInviteError("Could not load co-host invite.")
                return@launch
            }

            val data = JSONObject(result.second).optJSONObject("data")
            val host = data?.optJSONObject("host")
            val show = data?.optJSONObject("show")
            val hostName = host?.optString("name")?.takeIf { it.isNotBlank() }
                ?: host?.optString("username")?.takeIf { it.isNotBlank() }
                ?: "The seller"
            val showTitle = show?.optString("title")?.takeIf { it.isNotBlank() }
                ?: intent.getStringExtra("show_title").orEmpty().ifBlank { "this Live" }
            val hostUserId = data?.optInt("host_user_id")?.takeIf { it > 0 }?.toString()
                ?: intent.getStringExtra("host_user_id").orEmpty()
            val scheduleShowId = data?.optInt("schedule_show_id")?.takeIf { it > 0 }?.toString()
                ?: intent.getStringExtra("schedule_show_id").orEmpty()

            renderInviteDecision(inviteId, hostName, showTitle, hostUserId, scheduleShowId)
        }
    }

    private fun renderInviteDecision(
        inviteId: String,
        hostName: String,
        showTitle: String,
        hostUserId: String,
        scheduleShowId: String
    ) {
        resetInviteRoot("Co-host Invite")
        addText("$hostName has invited you to be a cohost for their Live: $showTitle. Would you like to accept?", 16, "#111111", true)
        addButton("Yes", "#2563EB") {
            loadEligibleProducts(inviteId, hostUserId, scheduleShowId)
        }
        addButton("No", "#6B7280") {
            lifecycleScope.launch {
                respondToInvite(inviteId, accept = false, productIds = emptyList())
                finish()
            }
        }
    }

    private fun loadEligibleProducts(inviteId: String, hostUserId: String, scheduleShowId: String) {
        renderInviteLoading("Loading products...")
        lifecycleScope.launch {
            val result = apiRequest("GET", "${Const.BASE_URL}/api/product/co-host/invite/$inviteId/eligible-products")
            if (result.first !in 200..299) {
                renderInviteError("Could not load eligible products.")
                return@launch
            }

            val products = mutableListOf<InviteProduct>()
            val arr = JSONObject(result.second).optJSONArray("data") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optInt("id")
                if (id <= 0) continue
                val title = obj.optString("title").ifBlank { obj.optString("name", "Product #$id") }
                val price = obj.optString("pricing").ifBlank { obj.optString("price") }
                products.add(InviteProduct(id, title, price))
            }
            renderProductPicker(inviteId, hostUserId, scheduleShowId, products)
        }
    }

    private fun renderProductPicker(inviteId: String, hostUserId: String, scheduleShowId: String, products: List<InviteProduct>) {
        resetInviteRoot("Add Products")
        addText("Would you like to add products to the show?", 18, "#111111", true)
        addText("Selecting items is optional.", 13, "#6B7280", false)

        val selected = linkedSetOf<Int>()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = dp(12) }
        }

        if (products.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No eligible products found."
                setTextColor(Color.parseColor("#6B7280"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
            })
        } else {
            products.forEach { product ->
                list.addView(CheckBox(this).apply {
                    text = if (product.subtitle.isBlank()) product.title else "${product.title} - ${product.subtitle}"
                    textSize = 15f
                    setTextColor(Color.parseColor("#111111"))
                    setPadding(0, dp(8), 0, dp(8))
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selected.add(product.id) else selected.remove(product.id)
                    }
                })
            }
        }
        bind.root.addView(list)

        addButton("Join as cohost", "#2563EB") {
            lifecycleScope.launch {
                val response = respondToInvite(inviteId, accept = true, productIds = selected.toList())
                if (response.first !in 200..299) {
                    renderInviteError("Could not join as cohost.")
                    return@launch
                }
                val data = JSONObject(response.second).optJSONObject("data")
                val coHostId = data?.optJSONObject("co_host")?.optInt("id") ?: 0
                val showId = data?.optString("schedule_show_id")?.takeIf { it.isNotBlank() } ?: scheduleShowId
                val intent = Intent(this@CoHostJoinActivity, AgoraPublisherActivity::class.java)
                intent.putExtra("show_id", showId)
                intent.putExtra("co_host", true)
                intent.putExtra("invited_cohost", true)
                intent.putExtra("host_user_id", hostUserId)
                intent.putExtra("pairing_id", coHostId)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private suspend fun respondToInvite(inviteId: String, accept: Boolean, productIds: List<Int>): Pair<Int, String> {
        val body = JSONObject().apply {
            put("accept", accept)
            put("product_ids", JSONArray(productIds))
        }.toString()
        return apiRequest("POST", "${Const.BASE_URL}/api/product/co-host/invite/$inviteId/respond", body)
    }

    private suspend fun apiRequest(method: String, urlString: String, body: String? = null): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            try {
                val token = Prefs(this@CoHostJoinActivity).token()
                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(body.toByteArray()) }
                    }
                }
                val rc = conn.responseCode
                val stream = if (rc in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                Pair(rc, text)
            } catch (e: Exception) {
                Pair(-1, e.message ?: "error")
            }
        }

    private fun renderInviteLoading(message: String) {
        resetInviteRoot("Co-host Invite")
        addText(message, 15, "#6B7280", false)
    }

    private fun renderInviteError(message: String) {
        resetInviteRoot("Co-host Invite")
        addText(message, 15, "#DC2626", true)
        addButton("Close", "#6B7280") { finish() }
    }

    private fun resetInviteRoot(title: String) {
        bind.root.removeAllViews()
        bind.root.gravity = Gravity.NO_GRAVITY
        bind.root.addView(TextView(this).apply {
            text = title
            setTextColor(Color.BLACK)
            textSize = 22f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(18) }
        })
    }

    private fun addText(textValue: String, size: Int, color: String, bold: Boolean) {
        bind.root.addView(TextView(this).apply {
            text = textValue
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        })
    }

    private fun addButton(label: String, color: String, onClick: () -> Unit) {
        bind.root.addView(Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
