package io.bidswipe.app.ui.dashboard.more

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.R.*
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentTrustedBuyerBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import java.io.File

class TrustedBuyerFragment : BaseFragment<MoreViewModel, FragmentTrustedBuyerBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentTrustedBuyerBinding.inflate(inflater,view,false)

    private var idPhoto = ""

    private val imageResult = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val imageUri = result.uriContent

            val imagePath = result.getUriFilePath(mCtx, true)

            bind.uploadLayout.isVisible = false
            bind.imgCard.isVisible = true

            bind.img.setImageURI(imageUri)

            idPhoto = imagePath.toString()

        }
    }

    @SuppressLint("ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        bind.fileBtn.setOnClickListener {
            requestPerms(Const.STR_PERMS) { per ->
                if (per) {
                    imageResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
                }
            }
        }

        bind.submit.setOnClickListener {
            bind.loader.isVisible = true

            val imageName = System.currentTimeMillis().toString() + "_id_photo.jpeg"
            val idPart = Utils.imagePart("image", imageName, File(idPhoto ))

            viewModel.storeBuyerIdentity(idPart)

        }


        bind.loader.isVisible = true

        viewModel.fetchBuyerIdentity()

        viewModel.fetchBuyerIdentityRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    runSafe {
                        bind.loader.isVisible = false

                        val mData = it.value.data

                        if (mData?.image?.isNotEmpty() == true){


                            bind.uploadLayout.isVisible = false
                            bind.imgCard.isVisible = true

                            bind.img.loadUrl(mCtx,mData.image.toString())

                            bind.firstDivider.dividerColor = ContextCompat.getColor(mCtx, color.primary)
                            bind.secondCard.setCardBackgroundColor(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(mCtx, R.color.primary)
                                )
                            )

                            bind.secondText.setTextColor(ContextCompat.getColor(mCtx, R.color.background))

                        }else{

                        }


                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
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


        viewModel.storeBuyerIdentityRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    runSafe {
                        bind.loader.isVisible = false

                        val mData = it.value.data


                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
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