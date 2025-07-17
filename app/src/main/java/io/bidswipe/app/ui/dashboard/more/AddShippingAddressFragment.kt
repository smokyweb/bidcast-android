package io.bidswipe.app.ui.dashboard.more

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentAddShippingAddressBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class AddShippingAddressFragment : BaseFragment<MoreViewModel, FragmentAddShippingAddressBinding>() {

    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentAddShippingAddressBinding.inflate(inflater,view,false)

    private var slug = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slug = activity?.intent?.getStringExtra("slug") ?:""


        bind.header.onBackClick {
            if (slug == "addAddress"){
                finish()
            }else{
                findNavController().popBackStack()
            }
        }

        bind.addAddress.setOnClickListener {

            when{

                bind.name.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please enter name")
                    bind.name.requestFocus()
                    showKeyboard(bind.name)
                }

                bind.phoneNumber.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please enter phone number")
                    bind.phoneNumber.requestFocus()
                    showKeyboard(bind.phoneNumber)
                }

                bind.streetAddress.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please enter street address")
                    bind.streetAddress.requestFocus()
                    showKeyboard(bind.streetAddress)
                }

                bind.zipCode.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please enter zip code")
                    bind.zipCode.requestFocus()
                    showKeyboard(bind.zipCode)
                }

                bind.radioGroup.checkedRadioButtonId == -1->{
                    Alerts.error(mCtx,"Please select address type")
                }
                else->{
                    bind.loader.isVisible = true

                    val buttonId = bind.radioGroup.checkedRadioButtonId

                    val selectedRadioButton = bind.radioGroup.findViewById<RadioButton>(buttonId)

                    val selectedText = selectedRadioButton.text

                    viewModel.addShippingAddress(
                        type = selectedText.toString().request(),
                        name = bind.name.value().request(),
                        phoneNumber = bind.phoneNumber.value().request(),
                        streetAddress = bind.streetAddress.value().request(),
                        pinCode = bind.zipCode.value().request()
                    )
                }

            }



        }

        viewModel.addShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (slug == "addAddress"){

                        activity?.setResult(Activity.RESULT_OK)
                        finish()
                    }else{
                        findNavController().popBackStack()
                    }

                    Alerts.success(mCtx,"Offer Sent")
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

}