package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import io.bidcast.app.base.BaseActivity
import io.bidcast.app.databinding.ActivityDashBinding
import io.bidcast.app.utils.bind
import io.bidcast.app.utils.ids

class DashActivity : BaseActivity() , NavController.OnDestinationChangedListener {

    private val bind by bind(ActivityDashBinding::inflate)

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        navHostFragment =
            supportFragmentManager.findFragmentById(ids.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener(this)
        bind.bottomBar.setupWithNavController(navController)


    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {

    }
}