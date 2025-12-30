package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.Token
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import io.bidswipe.app.App
import io.bidswipe.app.BuildConfig
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAddPaymentCardBinding
import io.bidswipe.app.databinding.DatePickerLayoutBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.PaymentCardModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.layout
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class AddPaymentCardActivity : BaseActivity() {

	private val bind by bind(ActivityAddPaymentCardBinding::inflate)

	private val viewModel by viewModels<DashViewModel>()

	private var mSheet: BottomSheetDialog? = null
	private var isShowing = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0, system.top, 0, system.bottom)
			CONSUMED
		}
		log("GET PAYMENT CARD")

		bind.header.onBackClick { finish() }

		bind.expiryDate.setHapticClickListener {
			showDatePicker {
				bind.expiryDate.setText(it)
			}
		}

		bind.rootView.setHapticClickListener {
			hideKeyboard()
		}

		bind.addCard.setHapticClickListener {

			when {

				bind.name.value().validator().nonEmpty().check().not() -> {
					Alerts.error(this, "Enter Card Holder Name")
					bind.name.requestFocus()
					showKeyboard(bind.name)
				}

				bind.cardNumber.value().validator().nonEmpty().check().not() -> {
					Alerts.error(this, "Enter Card Number")
					bind.cardNumber.requestFocus()
					showKeyboard(bind.cardNumber)
				}

				bind.cardNumber.value().validator().minLength(16).check().not() -> {
					Alerts.error(this, "Card Digit should be 12")
					bind.cardNumber.requestFocus()
					showKeyboard(bind.cardNumber)
				}

				bind.expiryDate.value().validator().nonEmpty().check().not() -> {
					Alerts.error(this, "Enter Expiry Date")
					bind.expiryDate.requestFocus()
					showKeyboard(bind.expiryDate)
				}

				bind.csv.value().validator().nonEmpty().check().not() -> {
					Alerts.error(this, "Enter CVV")
					bind.csv.requestFocus()
					showKeyboard(bind.csv)
				}

				bind.csv.value().validator().minLength(3).check().not() -> {
					Alerts.error(this, "Enter Valid CVV")
					bind.csv.requestFocus()
					showKeyboard(bind.csv)
				}

				else -> {

					hideKeyboard()

					hideKeyboard()

					val mCard = CardParams(
						number = bind.cardNumber.value(),
						expMonth = (bind.expiryDate.text ?: "").split("-")[1].toInt(),
						expYear = (bind.expiryDate.text ?: "").split("-")[0].toInt(),
						cvc = bind.csv.value(),
						name = bind.name.text.toString()
					)

					bind.loader.isVisible = true
					val strip = Stripe(this, BuildConfig.STRIPE_PK)
					strip.createCardToken(mCard, null, null, object : ApiResultCallback<Token> {
						override fun onSuccess(result: Token) {
							Log.d(TAG, "onSuccess: $result.")
							viewModel.addPaymentCard(result.id.request())
						}

						override fun onError(e: Exception) {

						}
					})

				}

			}
			/*
						if (bind.name.value().isEmpty()) {
						successSheet(true)
					} else {
						successSheet()
					}*/

		}


		viewModel.addPaymentCardRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						bind.loader.isVisible = false

						Alerts.success(this, "Payment card Added")

						App.getProfile()

						this.setResult(RESULT_OK)

						finish()

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


	private fun showDatePicker(call: (String) -> Unit) {
		val alBind = DatePickerLayoutBinding.bind(LayoutInflater.from(this).inflate(layout.date_picker_layout, null))

		mSheet = Alerts.appBottomSheet(this, false, alBind)

		isShowing = if (mSheet?.isShowing == true) {
			mSheet?.dismiss()
			false
		} else {
			mSheet?.show()
			true
		}

		alBind.cancel.setHapticClickListener {
			mSheet?.dismiss()
			isShowing = false
		}

		alBind.select.setHapticClickListener {
			val date = alBind.timePicker.date
			mSheet?.dismiss()
			isShowing = false
			call(Utils.getSimpleDate("YYYY-MM").format(date))
		}

	}

}