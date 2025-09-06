package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
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
	}

}