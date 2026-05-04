package io.bidswipe.app.ui.more

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAccountSecurityBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

private const val TAG = "AccountSecurityActivity"

@AndroidEntryPoint
class AccountSecurityActivity : BaseActivity() {

    private val bind by bind(ActivityAccountSecurityBinding::inflate)
    private val viewModel by viewModels<MoreViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.root.setPadding(0, system.top, 0, system.bottom)
            CONSUMED
        }

        bind.header.onBackClick { finishAfterTransition() }
        bind.root.setHapticClickListener { hideKeyboard() }

        bind.savePassword.setHapticClickListener {
            when {
                bind.currentPassword.value().isEmpty() -> {
                    Alerts.error(this, "Please enter your current password")
                    bind.currentPassword.requestFocus()
                    showKeyboard(bind.currentPassword)
                }
                bind.newPassword.value().isEmpty() -> {
                    Alerts.error(this, "Please enter a new password")
                    bind.newPassword.requestFocus()
                    showKeyboard(bind.newPassword)
                }
                bind.confirmPassword.value().isEmpty() -> {
                    Alerts.error(this, "Please confirm your new password")
                    bind.confirmPassword.requestFocus()
                    showKeyboard(bind.confirmPassword)
                }
                bind.newPassword.value() != bind.confirmPassword.value() -> {
                    Alerts.error(this, "Confirm password does not match")
                    bind.confirmPassword.requestFocus()
                    showKeyboard(bind.confirmPassword)
                }
                else -> {
                    bind.loader.isVisible = true
                    hideKeyboard(it)
                    viewModel.changePassword(
                        bind.currentPassword.value().request(),
                        bind.newPassword.value().request(),
                        bind.confirmPassword.value().request()
                    )
                }
            }
        }

        bind.deleteAccount.setHapticClickListener {
            AppBottomSheet(
                this,
                R.drawable.ic_logout_outline,
                "Delete account?",
                "This sends an account deletion request for your BidSwipe account. This action cannot be undone.",
                primaryBtnText = "Delete Account",
                secondaryBtnText = "Cancel",
                canCancel = true,
                iconPadding = 36,
                showSecondary = true,
                alertType = AlertType.WARNING,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        bind.loader.isVisible = true
                        viewModel.deleteAccountRequest(bind.deleteReason.value().request())
                    }
                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }
                }
            ).show()
        }

        viewModel.changePasswordRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.changePasswordRepo.value = null
                    Alerts.success(this, it.value.message ?: "Password changed")
                    bind.currentPassword.setText("")
                    bind.newPassword.setText("")
                    bind.confirmPassword.setText("")
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.changePasswordRepo.value = null
                    it.parse(this, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) = dialog.dismiss()
                        override fun secondaryClick(dialog: AppBottomSheet) = dialog.dismiss()
                    })
                }
                else -> {}
            }
        }

        viewModel.deleteAccountRequestRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.deleteAccountRequestRepo.value = null
                    Alerts.success(this, it.value.message ?: "Delete account request submitted")
                    finishAfterTransition()
                }
                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.deleteAccountRequestRepo.value = null
                    it.parse(this, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) = dialog.dismiss()
                        override fun secondaryClick(dialog: AppBottomSheet) = dialog.dismiss()
                    })
                }
                else -> {}
            }
        }
    }
}
