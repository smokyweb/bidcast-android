package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentTrustedBuyerBinding
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish

class TrustedBuyerFragment : BaseFragment<MoreViewModel, FragmentTrustedBuyerBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentTrustedBuyerBinding.inflate(inflater,view,false)

    private val imageResult = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val imageUri = result.uriContent

            val imagePath = result.getUriFilePath(mCtx, true)

        }
    }

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

    }

}