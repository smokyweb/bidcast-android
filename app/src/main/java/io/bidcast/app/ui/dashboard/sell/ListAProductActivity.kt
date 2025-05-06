package io.bidcast.app.ui.dashboard.sell

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidcast.app.R
import io.bidcast.app.base.BaseActivity
import io.bidcast.app.databinding.ActivityListAproductBinding
import io.bidcast.app.utils.bind

class ListAProductActivity : BaseActivity() {

    private val bind by bind (ActivityListAproductBinding::inflate)

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)


        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController


    }
}