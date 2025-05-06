package io.bidcast.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.bidcast.app.R
import io.bidcast.app.base.BaseActivity
import io.bidcast.app.databinding.ActivityTutorialsBinding
import io.bidcast.app.utils.bind

class TutorialsActivity : BaseActivity() {

    private val bind by bind(ActivityTutorialsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)



    }
}