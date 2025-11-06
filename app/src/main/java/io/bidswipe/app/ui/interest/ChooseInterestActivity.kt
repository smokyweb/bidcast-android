package io.bidswipe.app.ui.interest

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityChooseInterestBinding
import io.bidswipe.app.utils.bind

class ChooseInterestActivity : BaseActivity() {

	private val bind by bind(ActivityChooseInterestBinding::inflate)

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