package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityProductDetailsBinding
import io.bidswipe.app.utils.bind

class ProductDetailsActivity : BaseActivity() {
	
	private val bind by bind(ActivityProductDetailsBinding::inflate)
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
	}
}