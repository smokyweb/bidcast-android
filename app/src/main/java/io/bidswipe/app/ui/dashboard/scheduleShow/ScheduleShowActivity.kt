package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityScheduleShowBinding
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.ids

class ScheduleShowActivity : BaseActivity() {

    private val bind by bind(ActivityScheduleShowBinding::inflate)

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        val from = intent?.getStringExtra("from").toString()

        navHostFragment = supportFragmentManager.findFragmentById(ids.fragmentContainer) as NavHostFragment

        navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.schedule_show_graph)

        if (from == "tutorial"){
            navGraph.setStartDestination(R.id.selectShowTimeFragment)
        }

        navController.graph = navGraph

    }
}