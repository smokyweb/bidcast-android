package io.bidswipe.app.ui.sell

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityListAproductBinding
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.setHapticClickListener

class ListAProductActivity : BaseActivity() {
	private val bind by bind(ActivityListAproductBinding::inflate)
	private lateinit var navHostFragment: NavHostFragment
	private lateinit var navController: NavController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0, system.top, 0, system.bottom)
			CONSUMED
		}

		bind.main.setHapticClickListener {
			hideKeyboard()
		}
		navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
		navController = navHostFragment.navController


	}
}