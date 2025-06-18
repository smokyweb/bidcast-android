package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityNotificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import kotlin.getValue

class NotificationActivity : BaseActivity() {

    private val bind by bind(ActivityNotificationBinding::inflate)
    private val viewModel by viewModels<MoreViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        bind.header.onBackClick {
            finish()
        }

        bind.loader.isVisible = true

       viewModel.getNotification()

        viewModel.getNotificationRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    runSafe {
                        bind.loader.isVisible = false

                        val mData = it.value.data

                        bind.noData.isVisible = mData?.isEmpty() == true
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(this, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }
                else -> {}
            }
        }
    }
}