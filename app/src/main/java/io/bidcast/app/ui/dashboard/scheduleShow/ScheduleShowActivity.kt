package io.bidcast.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidcast.app.R
import io.bidcast.app.base.BaseActivity
import io.bidcast.app.databinding.ActivityScheduleShowBinding
import io.bidcast.app.utils.bind
import io.bidcast.app.utils.ids

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