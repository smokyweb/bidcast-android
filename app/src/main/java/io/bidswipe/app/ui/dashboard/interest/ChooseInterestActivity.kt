package io.bidswipe.app.ui.dashboard.interest

import android.os.Bundle
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityChooseInterestBinding
import io.bidswipe.app.utils.bind

class ChooseInterestActivity : BaseActivity() {

	private val bind by bind(ActivityChooseInterestBinding::inflate)

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

	}

}