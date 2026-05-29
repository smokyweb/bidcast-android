package io.bidswipe.app.ui.scheduleShow

// Basecamp #9940152629 (2026-05-29): pre-show tip settings activity.
// Receives schedule_show_id via intent, pre-loads saved tip_message +
// show_in_live_chat from GET /api/get-tip-setting, saves via
// POST /api/save-tip-setting on the "Save" button.
// This mirrors the inline tipSettingsSheet() in AgoraPublisherActivity but
// as a standalone screen accessible during show scheduling/editing.

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTipSettingBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class TipSettingActivity : BaseActivity() {

    private val bind by bind(ActivityTipSettingBinding::inflate)
    private val viewModel by viewModels<ScheduleShowViewModel>()

    /** schedule_shows.id passed via intent extra "schedule_show_id". */
    private var scheduleShowId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.root.setPadding(0, system.top, 0, system.bottom)
            CONSUMED
        }

        scheduleShowId = intent.getStringExtra("schedule_show_id")

        bind.header.onBackClick { finish() }

        // Pre-load saved settings if we have a show id
        if (!scheduleShowId.isNullOrBlank()) {
            bind.loader.isVisible = true
            viewModel.getTipSetting(scheduleShowId)
        }

        viewModel.getTipSettingRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.getTipSettingRepo.value = null
                    val tip = it.value.data
                    bind.etTipMessage.setText(tip?.tipMessage ?: "")
                    bind.switchShowInLiveChat.isChecked = tip?.showInLiveChat ?: true
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.getTipSettingRepo.value = null
                    // Silent fail — leave fields at defaults
                }
                else -> {}
            }
        }

        bind.btnSubmit.setHapticClickListener {
            val message = bind.etTipMessage.text?.toString()?.trim() ?: ""
            if (message.isEmpty()) {
                Alerts.error(this, "Please enter a tip message.")
                return@setHapticClickListener
            }
            bind.loader.isVisible = true
            viewModel.saveTipSetting(
                scheduleShowId = scheduleShowId,
                tipMessage = message,
                showInLiveChat = bind.switchShowInLiveChat.isChecked
            )
        }

        viewModel.saveTipSettingRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.saveTipSettingRepo.value = null
                    Alerts.success(this, it.value.message ?: "Tip settings saved!")
                    setResult(RESULT_OK)
                    finish()
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.saveTipSettingRepo.value = null
                    it.parse(this, "TipSettingActivity", object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }
                else -> {}
            }
        }
    }
}
