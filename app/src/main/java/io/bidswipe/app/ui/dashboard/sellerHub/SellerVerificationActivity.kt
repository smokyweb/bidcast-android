package io.bidswipe.app.ui.dashboard.sellerHub

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.R.color.warningClr
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
import io.bidswipe.app.utils.goToAddCard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value
import java.io.File
import kotlin.getValue

class SellerVerificationActivity : BaseActivity() {
/*
    override fun getModel(): Class<SellerHubViewModel>  = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSellerVerificationBinding.inflate(inflater,view,false)*/

    private val bind by bind (ActivitySellerVerificationBinding::inflate)

    private val viewModel by viewModels <SellerHubViewModel>()

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()

    var cardImage = ""
    var selfie = ""
    var isIdVerified = false
    var isPhoneVerified = false
    var cardToken = ""
    var cardId = ""

    private lateinit var cardAdapter : SelectPaymentCardAdapter

    private var addCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bind.loader.isVisible = true
            viewModel.getPaymentCard()
        }
    }

    private val mClick = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {

            cardList.forEachIndexed { index,item ->

                item?.selected = index == pos

                cardToken = item?.cardId.toString()

                cardAdapter.notifyDataSetChanged()

            }

        }

    }

    private val idResult = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val imageUri = result.uriContent
            val imagePath = result.getUriFilePath(this, true)
            if (imagePath != null) {

                bind.cardImage.isVisible = true
                bind.cardImage.loadUrl(this,imageUri.toString())

                cardImage = imagePath

                if (cardImage.isNotEmpty() && selfie.isNotEmpty()){

                    bind.verificationIcon.isVisible = true
                    bind.stepProgress.setProgress(1)
                    bind.stepCount.setText("1 of 4")

                }


                log("ImageUri = $imageUri")

            }
        }
    }

    private val selfieResult = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val imageUri = result.uriContent
            val imagePath = result.getUriFilePath(this, true)
            if (imagePath != null) {

                bind.selfie.isVisible = true
                bind.selfie.loadUrl(this,imageUri.toString())

                selfie = imagePath

                if (cardImage.isNotEmpty() && selfie.isNotEmpty()){

                    bind.verificationIcon.isVisible = true
                    bind.stepProgress.setProgress(1)
                    bind.stepCount.setText("1 of 4")

                }

                log("ImageUri = $imageUri")

            }
        }
    }


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        bind.header.onBackClick {
            finish()
        }

        cardAdapter= SelectPaymentCardAdapter(cardList,mClick)

        bind.recycler.adapter = cardAdapter

        bind.uploadId.setOnClickListener {
            uploadUserId()
        }

        bind.uploadSelfie.setOnClickListener {
            uploadUserSelfie()
        }

        bind.verifyId.setOnClickListener {

            when{

                cardImage.isEmpty() ->{

                    Alerts.error(this,"Please select Id card")
                }

                selfie.isEmpty() ->{
                    Alerts.error(this,"Please select Selfie")
                }

                else->{
                    bind.loader.isVisible = true

                    val idName = System.currentTimeMillis().toString() + "_id_card.jpeg"
                    val imagePart = Utils.imagePart("id_card", idName, File(cardImage ?: ""))

                    val imageName = System.currentTimeMillis().toString() + "_selfie_image.jpeg"
                    val selfiePart = Utils.imagePart("image", imageName, File(selfie ?: ""))

                    log("SELFIE PART : $selfiePart")

                    viewModel.storeSellerId(
                        imagePart,
                        selfiePart
                    )
                }

            }

        }

        bind.verifyPhone.setOnClickListener {

            when{

                bind.phoneNumber.value().isEmpty() ->{
                    Alerts.error(this,"Please Enter Phone Number")
                    showKeyboard(bind.phoneNumber)
                }

                else ->{
                    bind.loader.isVisible = true

                    viewModel.storePhoneNumber(bind.phoneNumber.value().request() )
                }

            }

        }

        bind.verifyOtp.setOnClickListener {

            when{

                bind.otp.value().isEmpty() ->{
                    Alerts.error(this,"Please Enter OTP")
                    showKeyboard(bind.otp)
                }

                else ->{
                    bind.loader.isVisible = true

                    viewModel.verifyNumberOtp(bind.otp.value().request() )
                }

            }

        }

        bind.addCardBtn.setOnClickListener {
            addCardLauncher.launch(this.goToAddCard("verification"))
        }

        bind.completeVerification.setOnClickListener {
            when{

                cardImage.isEmpty() ->{

                    Alerts.error(this,"Please select Id card")
                }

                selfie.isEmpty() ->{
                    Alerts.error(this,"Please select Selfie")
                }

                !isPhoneVerified ->{
                    Alerts.error(this,"Please verify your phone number")
                }


                cardToken.isEmpty() ->{
                    Alerts.error(this,"Please select Payment Card")
                }

                else ->{
                    bind.loader.isVisible = true

                    val idName = System.currentTimeMillis().toString() + "_id_card.jpeg"
                    val imagePart = Utils.imagePart("id_card", idName, File(cardImage ?: ""))

                    val imageName = System.currentTimeMillis().toString() + "_selfie_image.jpeg"
                    val selfiePart = Utils.imagePart("image", imageName, File(selfie ?: ""))

                    viewModel.storeSellerVerification(imagePart,selfiePart, "1".request() , cardToken.request()  )
                }

            }
        }

        bind.loader.isVisible = true

        viewModel.fetchSellerVerification()

        viewModel.fetchSellerVerificationRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data

                    bind.status.text = mData?.status

                    when(mData?.status){

                        "pending" ->{
                            bind.verificationIcon.isVisible = true
                            bind.stepProgress.setProgress(3)
                            bind.stepCount.setText("3 of 4")
                            bind.phoneNumberLayout.isVisible = false
                            bind.otpLayout.isVisible = false
                            bind.verifyOtp.isVisible = false
                            bind.verifyPhone.isVisible = false
                            bind.verificationPhoneIcon.isVisible = true
                            bind.completeVerification.isVisible = false
                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.warningClr))
                        }

                        "verified" ->{
                            bind.verifyPhone.isVisible = false
                            bind.verifyOtp.isVisible = true
                            bind.phoneNumberLayout.isVisible = false
                            bind.otpLayout.isVisible = true
                            bind.verificationIcon.isVisible = true
                            bind.verificationPhoneIcon.isVisible = true
                            bind.stepProgress.setProgress(4)
                            bind.stepCount.setText("4 of 4")
                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.success))
                            bind.completeVerification.isVisible = false
                        }

                        "rejected" ->{

                            bind.status.setTextColor(ContextCompat.getColor(this, R.color.error))
                            bind.statusDescription.text = mData.reason.toString()


                        }

                    }

                    viewModel.getPaymentCard()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.storeSellerVerificationRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    bind.stepProgress.setProgress(3)

                    bind.stepCount.setText("3 of 4")

                    bind.completeVerification.isVisible = false

                    Alerts.success(this,it.value.message.toString())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
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

                    bind.phoneNumberLayout.isVisible = false
                    bind.otpLayout.isVisible = true

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.verifyNumberOtpRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    isPhoneVerified = true
                    bind.stepProgress.setProgress(2)
                    bind.stepCount.setText("2 of 4")

                    bind.phoneNumberLayout.isVisible = false
                    bind.otpLayout.isVisible = false
                    bind.verifyOtp.isVisible = false
                    bind.verifyPhone.isVisible = false
                    bind.verificationPhoneIcon.isVisible = true

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
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

                    if (mData?.isNotEmpty() == true){

                        mData.forEach {
                            if (it?.cardId == cardId) it.selected =true
                            cardList.add(it)
                        }

                    }

                    if (cardList.isNotEmpty()){
                        bind.recycler.isVisible = true
                        bind.noCardView.isVisible = false
                    }else{
                        bind.recycler.isVisible = true
                        bind.noCardView.isVisible = false
                    }

                    cardAdapter.notifyDataSetChanged()


                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.storePaymentMethodRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    bind.stepProgress.setProgress(3)

                    bind.stepCount.setText("3 of 4")

                    bind.completeVerification.isVisible = false

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }


    }



    /*@SuppressLint("ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }*/

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