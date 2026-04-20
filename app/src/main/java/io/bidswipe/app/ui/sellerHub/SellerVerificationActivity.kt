package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivitySellerVerificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.FetchSellerVerificationResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const

import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.goToAddCard
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value
import java.io.File
import java.util.Locale

@SuppressLint("NotifyDataSetChanged", "ResourceAsColor", "ClickableViewAccessibility")
class SellerVerificationActivity : BaseActivity() {

    private val bind by bind(ActivitySellerVerificationBinding::inflate)

    private val viewModel by viewModels<SellerHubViewModel>()

    var cardImage = ""
    var selfie = ""
    var phoneNumber = ""
    var isPhoneVerified = false
    var paymentCardId = ""

    private var sellerData: FetchSellerVerificationResponse.Data? = null

    private val idResult = registerForActivityResult(CustomCropImageContract()) { result ->
        if (result.isSuccessful) {
            val imagePath = result.getUriFilePath(this, true)
            if (imagePath != null) {
                bind.cardImage.isVisible = true
                bind.cardImage.loadUrl(this, imagePath)
                cardImage = imagePath
                if (cardImage.isNotEmpty() && selfie.isNotEmpty()) {
                    bind.verificationIcon.isVisible = true
                    bind.stepProgress.progress = 1
                    bind.stepCount.text = buildString {
                        append("1 of 3")
                    }
                }
                log("ImageUri = $imagePath ")
            }
        }
    }

    private val selfieResult = registerForActivityResult(CustomCropImageContract()) { result ->
        if (result.isSuccessful) {
            val imagePath = result.getUriFilePath(this, true)
            if (imagePath != null) {
                bind.selfie.isVisible = true
                bind.selfie.loadUrl(this, imagePath)
                selfie = imagePath
                if (cardImage.isNotEmpty() && selfie.isNotEmpty()) {
                    bind.verificationIcon.isVisible = true
                    bind.stepProgress.progress = 1
                    bind.stepCount.text = buildString {
                        append("1 of 3")
                    }
                }
                log("ImageUri = $imagePath")

            }
        }
    }

    private var addCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            bind.loader.isVisible = true
            viewModel.getPaymentCard()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.root.setPadding(0, system.top, 0, system.bottom)
            CONSUMED
        }
        bind.header.onBackClick {
            hideKeyboard()
            finish()
        }

        bind.root.setHapticClickListener {
            hideKeyboard()
        }

        bind.main.setOnTouchListener { _, _ ->
            hideKeyboard()
            return@setOnTouchListener false
        }

        bind.uploadId.setHapticClickListener {
            hideKeyboard()
            uploadUserId()
        }

        bind.uploadSelfie.setHapticClickListener {
            hideKeyboard()
            uploadUserSelfie()
        }

        bind.verifyId.setHapticClickListener {
            hideKeyboard()

            when {

                cardImage.isEmpty() -> {
                    Alerts.error(this, "Please select Id card")
                }

                selfie.isEmpty() -> {
                    Alerts.error(this, "Please select Selfie")
                }

                else -> {
                    bind.loader.isVisible = true

                    val idName = System.currentTimeMillis().toString() + "_id_card.jpeg"
                    val imagePart = Utils.imagePart("id_card", idName, File(cardImage))

                    val imageName = System.currentTimeMillis().toString() + "_selfie_image.jpeg"
                    val selfiePart = Utils.imagePart("image", imageName, File(selfie))

                    log("SELFIE PART : $selfiePart")

                    viewModel.storeSellerId(
                        imagePart,
                        selfiePart
                    )

                }

            }

        }

        bind.verifyPhone.setHapticClickListener {
            hideKeyboard()

            // QA-FIX (seller OTP): normalize to US E.164 (`+1XXXXXXXXXX`)
            // client-side before hitting the backend. The seller OTP backend
            // only supports 10-12 digit numbers today and there is no country
            // code picker in the UI; defaulting to US (+1) is the safe short
            // term fix while the product decision on international is pending.
            val digitsOnly = bind.phoneNumber.value().filter { it.isDigit() }

            when {

                bind.phoneNumber.value().isEmpty() -> {
                    Alerts.error(this, "Please Enter Phone Number")
                    showKeyboard(bind.phoneNumber)
                }

                digitsOnly.length != 10 -> {
                    Alerts.error(this, "Please enter a valid 10-digit US phone number")
                    showKeyboard(bind.phoneNumber)
                }

                else -> {
                    bind.loader.isVisible = true

                    val e164 = "+1$digitsOnly"
                    phoneNumber = e164
                    viewModel.storePhoneNumber(e164.request())
                }

            }

        }

        bind.verifyOtp.setHapticClickListener {
            hideKeyboard()

            when {

                bind.otp.value().isEmpty() -> {
                    Alerts.error(this, "Please Enter OTP")
                    showKeyboard(bind.otp)
                }

                else -> {
                    bind.loader.isVisible = true

                    viewModel.verifyNumberOtp(bind.otp.value().request())
                }

            }

        }

        bind.addCardBtn.setHapticClickListener {
            hideKeyboard()
            addCardLauncher.launch(this.goToAddCard("verification"))
        }

        bind.completeVerification.setHapticClickListener {
            hideKeyboard()
            when {

                cardImage.isEmpty() -> {

                    Alerts.error(this, "Please select Id card")
                }

                selfie.isEmpty() -> {
                    Alerts.error(this, "Please select Selfie")
                }

                !isPhoneVerified -> {
                    Alerts.error(this, "Please verify your phone number")
                }

                paymentCardId.isEmpty() -> {
                    Alerts.error(this, "Please select Payment Card")
                }

                else -> {
                    bind.loader.isVisible = true

                    val idName = System.currentTimeMillis().toString() + "_id_card.jpeg"
                    val imagePart = Utils.imagePart("id_card", idName, File(cardImage))

                    val imageName = System.currentTimeMillis().toString() + "_selfie_image.jpeg"
                    val selfiePart = Utils.imagePart("image", imageName, File(selfie))

                    viewModel.storeSellerVerification(imagePart, selfiePart, "1".request(), paymentCardId.request())
                }
            }
        }

        bind.editPhone.setHapticClickListener {
            hideKeyboard()
            bind.phoneNumberLayout.isVisible = true
            bind.verifyPhoneTitle.isVisible = true
            bind.verifyPhoneTitle.text = buildString {
                append("Enter phone number")
            }
            bind.otpLayout.isVisible = false
            bind.verifyOtp.isVisible = false
            bind.verifyPhone.isVisible = true
            bind.verificationPhoneIcon.isVisible = false
            bind.phoneNumber.setText("")
            bind.editPhone.isVisible = false
            bind.resend.isVisible = false
        }

        bind.resend.setHapticClickListener {
            hideKeyboard()

            // QA-FIX (seller OTP): phoneNumber is already normalized to E.164
            // when the user first submitted, so just resend that.
            bind.loader.isVisible = true
            viewModel.storePhoneNumber(phoneNumber.request())

        }

        bind.addKycBtn.setHapticClickListener {
            hideKeyboard()
            startActivity(
                Intent(this, SellerHubActivity::class.java).putExtra("slug", "identityVerification")
            )
        }

        viewModel.storeSellerVerificationRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    it.value.data
                    bind.stepProgress.progress = 2

                    bind.stepCount.text = buildString {
                        append("2 of 3")
                    }

                    bind.completeVerification.isVisible = false

                    Alerts.success(this, it.value.message.toString())

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

        bind.loader.isVisible = true

        viewModel.fetchSellerVerification()
        viewModel.fetchSellerVerificationRepo.observe(this) { it ->
            when (it) {
                is Resource.Success -> {

                    sellerData = it.value.data

                    bind.status.text = sellerData?.status?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                        else it.toString()
                    }

                    isPhoneVerified = sellerData?.numberOtpVerified == 1
                    phoneNumber = sellerData?.phoneNumber ?: ""

                    when (sellerData?.status) {

                        "pending" -> {
                            bind.verificationIcon.isVisible = true
                            bind.stepProgress.progress = 3
                            bind.stepCount.text = buildString {
                                append("2 of 3")
                            }
                            bind.addCardBtn.isVisible = false
                            bind.phoneNumberLayout.isVisible = false
                            bind.otpLayout.isVisible = false
                            bind.verifyOtp.isVisible = false
                            bind.verifyPhoneTitle.isVisible = false
                            bind.verifyPhone.isVisible = false
                            bind.verificationPhoneIcon.isVisible = true
                            bind.completeVerification.isVisible = false
                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.warning))
                            bind.uploadId.isClickable = false
                            bind.uploadSelfie.isClickable = false
                        }

                        "verified" -> {
                            bind.verifyPhone.isVisible = false
                            bind.verifyOtp.isVisible = true
                            bind.phoneNumberLayout.isVisible = false
                            bind.otpLayout.isVisible = false
                            bind.verificationIcon.isVisible = true
                            bind.verificationPhoneIcon.isVisible = true
                            bind.verifyOtp.isVisible = false
                            bind.addCardBtn.isVisible = false
                            bind.verifyPhoneTitle.isVisible = false
                            bind.stepProgress.progress = 3
                            bind.stepCount.text = buildString {
                                append("3 of 3")
                            }
                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.success))
                            bind.completeVerification.isVisible = false
                            bind.uploadId.isClickable = false
                            bind.uploadSelfie.isClickable = false
                        }

                        "rejected" -> {
                            bind.addCardBtn.isVisible = false
                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.error))
                            bind.statusDescription.text = sellerData?.reason.toString()
                        }

                        else -> {
                            bind.addCardBtn.isVisible = true
                        }
                    }

                    viewModel.getPaymentCard()
                    updateStepper()

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
        viewModel.storeSellerIdRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    sellerData = it.value.data
                    updateStepper()
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

        viewModel.storePhoneNumberRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    bind.verifyPhone.isVisible = false
                    bind.verifyOtp.isVisible = true
                    // QA-FIX (seller OTP): prefer the E.164 number we just
                    // submitted; fall back to whatever backend echoed. Mask
                    // everything except the last 4 digits so the banner works
                    // for both raw 10-digit and `+1XXXXXXXXXX` shapes.
                    val serverPhone = mData?.phoneNumer.orEmpty()
                    val resolvedPhone = if (serverPhone.isNotBlank()) serverPhone else phoneNumber
                    phoneNumber = resolvedPhone
                    val lastFour = resolvedPhone.filter { it.isDigit() }.takeLast(4)
                    bind.verifyPhoneTitle.text = buildString {
                        append("OTP has been sent to ******")
                        append(lastFour)
                    }

                    bind.editPhone.isVisible = true
                    bind.resend.isVisible = true
                    bind.phoneNumberLayout.isVisible = false
                    bind.otpLayout.isVisible = true

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

        viewModel.verifyNumberOtpRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    isPhoneVerified = true
                    if (phoneNumber.isNotEmpty()) {
                        bind.verifyNumberText.text = phoneNumber
                    }
                    bind.stepProgress.progress = 2
                    bind.stepCount.text = buildString {
                        append("2 of 4")
                    }

                    bind.phoneNumberLayout.isVisible = false
                    bind.verifyPhoneTitle.isVisible = false
                    bind.phoneNumberLayout.visibility = View.GONE
                    bind.verifyPhoneTitle.visibility = View.GONE
                    bind.resend.isVisible = false
                    bind.editPhone.isVisible = false

                    bind.otpLayout.isVisible = false
                    bind.verifyOtp.isVisible = false
                    bind.verifyPhone.isVisible = false
                    bind.verificationPhoneIcon.isVisible = true

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

        viewModel.getPaymentCardRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {
                        val selectedCard = mData.firstOrNull()
                        paymentCardId = selectedCard?.cardId.orEmpty()
                        bind.paymentCardIcon.isVisible = true

                        bind.addCardBtn.isVisible=false
                        bind.paymentCardNumber.isVisible = true
                        bind.paymentCardNumber.text = buildString {
                            append("**** **** **** ")
                            append(selectedCard?.last4.orEmpty())
                        }
                        bind.paymentCardExpiry.text = buildString {
                            append("Expires ")
                            append(selectedCard?.expMonth ?: "--")
                            append("/")
                            append(selectedCard?.expYear ?: "--")
                        }
                        bind.paymentCardItem.isVisible = true
                    } else {
                        paymentCardId = ""
                        bind.paymentCardIcon.isVisible = false
                        bind.paymentCardNumber.isVisible = false
                        bind.paymentCardItem.isVisible = false
                        bind.addCardBtn.isVisible = true
                    }

                    updateStepper()

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

        viewModel.storePaymentMethodRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    it.value.data

                    bind.stepProgress.progress = 3

                    bind.stepCount.text = buildString {
                        append("3 of 3")
                    }

                    bind.completeVerification.isVisible = false

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

        App.checkKycResponse.observe(this) {
            updateStepper()
        }

    }

    fun uploadUserId() {
        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                idResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
            }
        }
    }

    fun uploadUserSelfie() {
        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                selfieResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
            }
        }
    }

    private fun updateStepper() {

        val kycActive = App.checkKycResponse.value?.kycStatus == "active"
        var idDone = false
        val phoneDone = isPhoneVerified
        val paymentDone = paymentCardId.isNotEmpty()

        if (sellerData?.idCard?.isNotEmpty() == true && sellerData?.image?.isNotEmpty() == true) {
            idDone=true
            bind.cardImage.isVisible = true
            bind.selfie.isVisible = true
            bind.cardImage.loadUrl(this, sellerData?.idCard ?: "")
            bind.selfie.loadUrl(this, sellerData?.image ?: "")
            bind.verificationIcon.isVisible = true
            bind.verifyId.isVisible =false
            bind.uploadId.isClickable = false
            bind.uploadSelfie.isClickable = false
        } else {
            idDone=false
            bind.cardImage.isVisible = false
            bind.selfie.isVisible = false
            bind.verificationIcon.isVisible = false
            bind.verifyId.isVisible = true

            bind.uploadId.isClickable = true
            bind.uploadSelfie.isClickable = true
        }

        val completedSteps = listOf(
            idDone,
            phoneDone,
            kycActive,
            paymentDone
        ).count { it }

        bind.stepProgress.progress = completedSteps
        bind.stepCount.text = "$completedSteps of 4"

        val step2Enabled = idDone
        val step3Enabled = step2Enabled && phoneDone
        val step4Enabled = step3Enabled && kycActive

        // Step 2 (Phone): only available after step 1 is done.
        bind.verifyPhoneTitle.isVisible = step2Enabled && !phoneDone
        bind.phoneNumberLayout.isVisible = step2Enabled && !phoneDone && !bind.otpLayout.isVisible
        bind.otpLayout.isVisible = step2Enabled && !phoneDone && bind.otpLayout.isVisible
        bind.verifyPhone.isVisible = step2Enabled && !phoneDone
        bind.verifyOtp.isVisible = step2Enabled && !phoneDone && bind.otpLayout.isVisible
        bind.editPhone.isVisible = step2Enabled && !phoneDone && bind.editPhone.isVisible
        bind.resend.isVisible = step2Enabled && !phoneDone && bind.resend.isVisible
        bind.verificationPhoneIcon.isVisible = phoneDone
        bind.verifyNumberText.text = if (phoneDone) {
            phoneNumber.ifEmpty { sellerData?.phoneNumber ?: "" }.ifEmpty { "Phone number verified" }
        } else {
            "Verify your phone number"
        }
        bind.verifyPhone.isEnabled = step2Enabled
        bind.verifyOtp.isEnabled = step2Enabled
        bind.phoneNumber.isEnabled = step2Enabled
        bind.otp.isEnabled = step2Enabled
        bind.editPhone.isEnabled = step2Enabled
        bind.resend.isEnabled = step2Enabled

        // Step 3 (KYC): only available after step 2 is done.
        bind.addKycBtn.isVisible = step3Enabled && !kycActive
        bind.kycCheck.isVisible = step3Enabled && kycActive
//        bind.kycDescription.isVisible = step3Enabled

        log("ACTIVE $kycActive")
        bind.kycDescription.text = if (kycActive) {
            "KYC verified successfully"
        } else {
            "Stripe identity verification(KYC) to start selling."
        }
        bind.kycDescription.setTextColor(
            ContextCompat.getColor(
                this,
                if (kycActive) R.color.success else R.color.onSurfaceVariant
            )
        )

        // Step 4 (Payment): only available after step 3 is done.
        bind.addCardBtn.isVisible = step4Enabled && !paymentDone
        bind.addCardBtn.isEnabled = step4Enabled
        bind.paymentMethodCheck.isVisible = step4Enabled && paymentDone
        bind.paymentCardItem.isVisible = step4Enabled && paymentDone

        when (sellerData?.status) {
            "pending" -> {
                bind.completeVerification.isVisible = false
                bind.status.setTextColor(ContextCompat.getColor(this, R.color.warning))
                bind.status.isVisible = true
                bind.manualVerificationIcon.isVisible = false
                bind.statusDescription.text = "Final review by our team"
                bind.statusDescription.setTextColor(ContextCompat.getColor(this, R.color.onSurfaceVariant))
            }

            "verified" -> {
                bind.status.setTextColor(ContextCompat.getColor(this, R.color.success))
                bind.statusDescription.text = "Verified"
                bind.statusDescription.setTextColor(ContextCompat.getColor(this, R.color.success))
                bind.completeVerification.isVisible = false
                bind.status.isVisible = false
                bind.manualVerificationIcon.isVisible = true
            }

            "rejected" -> {
                bind.status.setTextColor(ContextCompat.getColor(this, R.color.error))
                bind.statusDescription.text = sellerData?.reason.toString()
                bind.completeVerification.isVisible = false
                bind.status.isVisible = true
                bind.manualVerificationIcon.isVisible = false
                bind.statusDescription.setTextColor(ContextCompat.getColor(this, R.color.error))
            }

            else -> {
                bind.completeVerification.isVisible = false
                bind.status.isVisible = true
                bind.manualVerificationIcon.isVisible = false
                bind.statusDescription.text = "Final review by our team"
                bind.statusDescription.setTextColor(ContextCompat.getColor(this, R.color.onSurfaceVariant))
            }
        }

    }

}