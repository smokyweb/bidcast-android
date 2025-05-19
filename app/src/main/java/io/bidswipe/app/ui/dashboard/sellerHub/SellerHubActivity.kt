package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivitySellerHubBinding
import io.bidswipe.app.utils.bind

class SellerHubActivity : BaseActivity() {

    private val bind by bind (ActivitySellerHubBinding::inflate)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)



    }
}