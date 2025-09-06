package io.bidswipe.app.ui.dashboard.sell

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityListAproductBinding
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard

class ListAProductActivity : BaseActivity() {
	private val bind by bind(ActivityListAproductBinding::inflate)
	private lateinit var navHostFragment : NavHostFragment
	private lateinit var navController : NavController

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		bind.main.setOnClickListener {
			hideKeyboard()
		}
		navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
		navController = navHostFragment.navController


	}
}