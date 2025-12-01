package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.SelectPaymentCardAdapter
import io.bidswipe.app.databinding.ActivitySellerVerificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPaymentCardsResponse
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

	private var cardList = mutableListOf<GetPaymentCardsResponse.Data.PaymentProfile?>()

	var cardImage = ""
	var selfie = ""
	var phoneNumber = ""
	var isPhoneVerified = false
	var paymentCardId = ""

	private lateinit var cardAdapter: SelectPaymentCardAdapter

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


	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			cardList.forEachIndexed { index, item ->

				item?.selected = index == pos

				paymentCardId = item?.customerPaymentProfileId.toString()

				cardAdapter.notifyDataSetChanged()

			}

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
			finish()
		}

		bind.root.setHapticClickListener {
			hideKeyboard()
		}

		bind.main.setOnTouchListener { _, _ ->
			hideKeyboard()
			return@setOnTouchListener true
		}

		cardAdapter = SelectPaymentCardAdapter(cardList, mClick)

		bind.recycler.adapter = cardAdapter

		bind.uploadId.setHapticClickListener {
			uploadUserId()
		}

		bind.uploadSelfie.setHapticClickListener {
			uploadUserSelfie()
		}

		bind.verifyId.setHapticClickListener {

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

			when {

				bind.phoneNumber.value().isEmpty() -> {
					Alerts.error(this, "Please Enter Phone Number")
					showKeyboard(bind.phoneNumber)
				}

				else -> {
					bind.loader.isVisible = true

					viewModel.storePhoneNumber(bind.phoneNumber.value().request())
				}

			}

		}

		bind.verifyOtp.setHapticClickListener {

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
			addCardLauncher.launch(this.goToAddCard("verification"))
		}

		bind.completeVerification.setHapticClickListener {
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

			bind.loader.isVisible = true
			viewModel.storePhoneNumber(phoneNumber.request())

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

					val mData = it.value.data

					bind.status.text = mData?.status?.replaceFirstChar {
						if (it.isLowerCase()) it.titlecase(Locale.getDefault())
						else it.toString()
					}

					if (mData?.idCard?.isNotEmpty() == true && mData.image?.isNotEmpty() == true) {
						bind.cardImage.isVisible = true
						bind.selfie.isVisible = true
						bind.cardImage.loadUrl(this, mData.idCard)
						bind.selfie.loadUrl(this, mData.image)
					} else {
						bind.cardImage.isVisible = false
						bind.selfie.isVisible = false
					}

					when (mData?.status) {

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
							bind.status.setTextColor(ContextCompat.getColor(this, R.color.warningClr))
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
							bind.stepCount.text = buildString {
								append("0 of 3")
							}
							bind.addCardBtn.isVisible = false
							bind.status.setTextColor(ContextCompat.getColor(this, R.color.error))
							bind.statusDescription.text = mData.reason.toString()
						}

						else -> {
							bind.addCardBtn.isVisible = true
						}

					}

					viewModel.getPaymentCard()

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
					phoneNumber = mData?.phoneNumer.toString()
					bind.verifyPhoneTitle.text = buildString {
						append("OTP has been sent on ******")
						append(mData?.phoneNumer?.drop(6))
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

					it.value.data
					isPhoneVerified = true
					bind.stepProgress.progress = 2
					bind.stepCount.text = buildString {
						append("2 of 3")
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

					cardList.clear()

					if (mData?.paymentProfiles?.isNotEmpty() == true) {

						paymentCardId = mData.paymentProfiles[0]?.customerPaymentProfileId.toString()

						mData.paymentProfiles[0]?.selected = true

						cardList.add(mData.paymentProfiles[0])

					}

					if (cardList.isNotEmpty()) {
						bind.recycler.isVisible = true
						bind.noCardView.isVisible = false
					} else {
						bind.recycler.isVisible = true
						bind.noCardView.isVisible = false
					}

					cardAdapter.notifyDataSetChanged()

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

}