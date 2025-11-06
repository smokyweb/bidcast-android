package io.bidswipe.app.ui.product

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityProductDetailsBinding
import io.bidswipe.app.utils.bind

class ProductDetailsActivity : BaseActivity() {
	private val bind by bind(ActivityProductDetailsBinding::inflate)

	private lateinit var navHostFragment : NavHostFragment
	private lateinit var navController : NavController

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0,system.top,0, system.bottom)
			CONSUMED
		}
	}

}