package io.bidswipe.app.ui.dashboard.sellerHub

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SelectPaymentCardAdapter
import io.bidswipe.app.databinding.FragmentSellerVerificationBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.goToAddCard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value
import java.io.File

class SellerVerificationFragment : BaseFragment<SellerHubViewModel, FragmentSellerVerificationBinding>() {

    override fun getModel(): Class<SellerHubViewModel>  = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSellerVerificationBinding.inflate(inflater,view,false)

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()

    var cardImage = ""
    var selfie = ""
    var isIdVerified = false
    var isPhoneVerified = false
    var cardToken = ""
    var cardId = ""

    private lateinit var cardAdapter : SelectPaymentCardAdapter

    private var addCardLauncher =

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            val imagePath = result.getUriFilePath(mCtx, true)
            if (imagePath != null) {

                bind.cardImage.isVisible = true
                bind.cardImage.loadUrl(mCtx,imageUri.toString())

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
            val imagePath = result.getUriFilePath(mCtx, true)
            if (imagePath != null) {

                bind.selfie.isVisible = true
                bind.selfie.loadUrl(mCtx,imageUri.toString())

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                    Alerts.error(mCtx,"Please select Id card")
                }

                selfie.isEmpty() ->{
                    Alerts.error(mCtx,"Please select Selfie")
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
                    Alerts.error(mCtx,"Please Enter Phone Number")
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
                    Alerts.error(mCtx,"Please Enter OTP")
                    showKeyboard(bind.otp)
                }

                else ->{
                    bind.loader.isVisible = true

                    viewModel.verifyNumberOtp(bind.otp.value().request() )
                }

            }

        }

        bind.addCardBtn.setOnClickListener {
            addCardLauncher.launch(mCtx.goToAddCard("verification"))
        }

        bind.completeVerification.setOnClickListener {
            when{

                cardImage.isEmpty() ->{

                    Alerts.error(mCtx,"Please select Id card")
                }

                selfie.isEmpty() ->{
                    Alerts.error(mCtx,"Please select Selfie")
                }

                !isPhoneVerified ->{
                    Alerts.error(mCtx,"Please verify your phone number")
                }


                cardToken.isEmpty() ->{
                    Alerts.error(mCtx,"Please select Payment Card")
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

        viewModel.fetchSellerVerificationRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    val mData = it.value.data


                    when(mData?.status){

                        "pending" ->{
                            bind.verifyOtp.isVisible = true
                            bind.verificationIcon.isVisible = true

                            bind.stepProgress.setProgress(3)
                            bind.stepCount.setText("3 of 4")

                            bind.status.text = mData.status
                            bind.status.setTextColor(R.color.success)

                            bind.phoneNumberLayout.isVisible = false
                            bind.otpLayout.isVisible = false
                            bind.verifyOtp.isVisible = false
                            bind.verifyPhone.isVisible = false
                            bind.verificationPhoneIcon.isVisible = true

                            bind.completeVerification.isVisible = false

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

                            bind.status.text = mData.status
                            bind.status.setTextColor(R.color.success)
                            bind.completeVerification.isVisible = false

                        }

                    }


                    viewModel.getPaymentCard()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.storeSellerVerificationRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    bind.stepProgress.setProgress(3)

                    bind.stepCount.setText("3 of 4")

                    bind.completeVerification.isVisible = false

                    Alerts.success(mCtx,it.value.message.toString())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.storePhoneNumberRepo.observe(viewLifecycleOwner) {
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
                        it.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.verifyNumberOtpRepo.observe(viewLifecycleOwner) {
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
                        it.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.getPaymentCardRepo.observe(viewLifecycleOwner) {
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
                        it.parse(mCtx, TAG, object : AlertClicks {
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

        viewModel.storePaymentMethodRepo.observe(viewLifecycleOwner) {
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
                        it.parse(mCtx, TAG, object : AlertClicks {
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

    fun uploadUserId() {
        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                idResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
            }
        }
    }

    fun uploadUserSelfie() {
        requestPerms(Const.STR_PERMS) { per ->
            if (per) {
                selfieResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
            }
        }
    }

}