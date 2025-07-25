package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.Token
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAddPaymentCardBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value
import kotlin.getValue

class AddPaymentCardActivity : BaseActivity() {

    private val bind by bind(ActivityAddPaymentCardBinding::inflate)

    private val viewModel by viewModels<DashViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        bind.header.onBackClick { finish() }

        PaymentConfiguration.init(this, Const.STRIPE_KEY)

        bind.addCard.setOnClickListener {

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

                bind.expDate.value().validator().nonEmpty().check().not() -> {
                    Alerts.error(this, "Enter Expiry Date")
                    bind.expDate.requestFocus()
                    showKeyboard(bind.expDate)
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

                    bind.loader.isVisible = true

                    val mCard = CardParams(
                        number = bind.cardNumber.value(),
                        expMonth = bind.expDate.value().split("/")[0].toInt(),
                        expYear = bind.expDate.value().split("/")[1].toInt(),
                        cvc = bind.csv.value(),
                        name = bind.name.value(),
                        currency = "usd"
                    )

                    try {
                        val stripe = Stripe(this, Const.STRIPE_KEY)

                        stripe.createCardToken(mCard, null, null, object :
                            ApiResultCallback<Token> {

                            override fun onSuccess(result: Token) {

                                Alerts.log(TAG, "STRIPE TOKEN : $result")

                                viewModel.addPaymentCard(
                                    bind.cardNumber.value().replace(" ","").request(),
                                    bind.expDate.value().request(),
                                    bind.csv.value().request()
                                )
                            }

                            override fun onError(e: Exception) {
                                bind.loader.isVisible = false
                                Alerts.error(this@AddPaymentCardActivity, e.message.toString())
                            }

                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

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
}