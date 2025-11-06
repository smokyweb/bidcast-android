package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTipSettingBinding
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind

class TipSettingActivity : BaseActivity() {

	private val bind by bind(ActivityTipSettingBinding::inflate)
	private val viewModel by viewModels<ScheduleShowViewModel>()

	private var defaultAmount = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
		
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0,system.top,0, system.bottom)
			CONSUMED
		}

		repeat(5) {
			bind.chipGroupDefaultTips.addView(
				Utils.makeAChip(
					mCtx = this,
					text = "$ 25",
					selected = false,
					closeIconVisible = true
				)
			)
		}

		repeat(4) {
			defaultAmount += 5
			bind.chipGroupQuickAddTips.addView(
				Utils.makeAChip(
					mCtx = this,
					text = "$ $defaultAmount",
					selected = false,
					closeIconVisible = false
				)
			)
		}

	}

}