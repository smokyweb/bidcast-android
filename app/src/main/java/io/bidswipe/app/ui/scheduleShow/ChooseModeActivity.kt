package io.bidswipe.app.ui.scheduleShow

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.bidswipe.app.R
import io.bidswipe.app.databinding.ActivityChooseModeBinding
import io.bidswipe.app.utils.bind
import kotlin.getValue

class ChooseModeActivity : AppCompatActivity() {

    private val bind by bind(ActivityChooseModeBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bind.publish.setOnClickListener {
            startActivity(Intent(this@ChooseModeActivity, DolbyStreamActivity::class.java))
        }

        bind.sub.setOnClickListener {
            startActivity(Intent(this@ChooseModeActivity, DolbySubscribeActivity::class.java))
        }
    }
}