package io.bidswipe.app.ui.cohost

// Basecamp #9934001770 (2026-05-27): co-host claim screen. Second device
// enters the 6-char pairing code generated on the primary device, claims
// via POST /api/product/co-host/claim, and on success goes to the live show
// as a co-host.

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.bidswipe.app.databinding.ActivityCoHostJoinBinding
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CoHostJoinActivity : AppCompatActivity() {
    private lateinit var bind: ActivityCoHostJoinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityCoHostJoinBinding.inflate(layoutInflater)
        setContentView(bind.root)

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
                        val show = json.optJSONObject("data")?.optJSONObject("show")
                        val showId = show?.optInt("id") ?: 0
                        val intent = Intent(this@CoHostJoinActivity, AgoraPublisherActivity::class.java)
                        intent.putExtra("show_id", showId.toString())
                        intent.putExtra("co_host", true)
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
}
