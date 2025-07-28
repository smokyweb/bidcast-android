package io.bidswipe.app.ui.dashboard.more

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentAddShippingAddressBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetStatesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class AddShippingAddressFragment :
    BaseFragment<MoreViewModel, FragmentAddShippingAddressBinding>() {

    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentAddShippingAddressBinding.inflate(inflater, view, false)

    private var slug = ""
    private var stateList = mutableListOf<GetStatesResponse.Data?>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slug = activity?.intent?.getStringExtra("slug") ?: ""

        bind.header.onBackClick {
            if (slug == "addAddress") {
                finish()
            } else {
                findNavController().popBackStack()
            }
        }

        bind.state.setOnItemClickListener { _, _, position, _ ->

        }

        bind.state.setOnClickListener {
            bind.state.showDropDown()
        }

        bind.addAddress.setOnClickListener {

            when {

                bind.name.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter name")
                    bind.name.requestFocus()
                    showKeyboard(bind.name)
                }

                bind.phoneNumber.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter phone number")
                    bind.phoneNumber.requestFocus()
                    showKeyboard(bind.phoneNumber)
                }

                bind.streetAddress.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter street address")
                    bind.streetAddress.requestFocus()
                    showKeyboard(bind.streetAddress)
                }

                bind.zipCode.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter zip code")
                    bind.zipCode.requestFocus()
                    showKeyboard(bind.zipCode)
                }

                bind.city.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter city")
                    bind.city.requestFocus()
                    showKeyboard(bind.city)
                }

                bind.state.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please select state")
                }

                bind.radioGroup.checkedRadioButtonId == -1 -> {
                    Alerts.error(mCtx, "Please select address type")
                }

                else -> {
                    bind.loader.isVisible = true

                    val buttonId = bind.radioGroup.checkedRadioButtonId

                    val selectedRadioButton = bind.radioGroup.findViewById<RadioButton>(buttonId)

                    val selectedText = selectedRadioButton.text

                    viewModel.addShippingAddress(
                        type = selectedText.toString().request(),
                        name = bind.name.value().request(),
                        phoneNumber = bind.phoneNumber.value().request(),
                        streetAddress = bind.streetAddress.value().request(),
                        pinCode = bind.zipCode.value().request(),
                        city = bind.city.value().request(),
                        state = bind.state.value().request()
                    )
                }

            }


        }

        viewModel.addShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.addShippingAddressRepo.value = null

                    val mData = it.value.data

                    App.getProfile()

                    if (slug == "addAddress") {

                        activity?.setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        findNavController().popBackStack()
                    }


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

        bind.loader.isVisible = true
        viewModel.getStates()
        viewModel.getStatesRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    if (it.value.data?.isNotEmpty() == true) {
                        bind.loader.isVisible = false
                        stateList.clear()
                        stateList.addAll(it.value.data)

                        val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, stateList.map { it?.iso2 })
                        bind.state.setAdapter(adapter)
                        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                        bind.state.setDropDownBackgroundDrawable(draw)
                    }
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