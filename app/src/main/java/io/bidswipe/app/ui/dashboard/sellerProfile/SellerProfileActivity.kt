package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivitySellerProfileBinding
import io.bidswipe.app.utils.bind

class SellerProfileActivity : BaseActivity() {

    private val bind by bind (ActivitySellerProfileBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)


    }
}