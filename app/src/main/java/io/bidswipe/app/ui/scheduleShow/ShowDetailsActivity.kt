package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityScheduleShowBinding
import io.bidswipe.app.databinding.ActivityScheduleShowBinding.inflate
import io.bidswipe.app.databinding.ActivityShowDetailsBinding
import io.bidswipe.app.utils.bind
import kotlin.getValue

class ShowDetailsActivity : BaseActivity() {

    private val bind by bind(ActivityShowDetailsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)



    }

}