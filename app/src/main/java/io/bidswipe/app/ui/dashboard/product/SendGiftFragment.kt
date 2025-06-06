package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSendGiftBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class SendGiftFragment : BaseFragment<ProductViewModel, FragmentSendGiftBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSendGiftBinding.inflate(inflater, view, false)

    private var shippingId = ""
    private var productId = ""
    private var cardId = ""
    private var promoCode = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shippingId = arguments?.getString("shippingId") ?:""
        productId = arguments?.getString("productId") ?:""
        cardId = arguments?.getString("cardId") ?:""
        promoCode = arguments?.getString("promoCode") ?:""

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.root.setOnClickListener {
            hideKeyboard(it)
        }

        bind.continueBtn.setOnClickListener {

            when{

                bind.user.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please select an user")
                    bind.user.requestFocus()
                    showKeyboard(bind.user)
                }

                bind.message.value().isEmpty() ->{
                    Alerts.error(mCtx,"Please enter an message")
                    bind.message.requestFocus()
                    showKeyboard(bind.message)
                }

                else->{
                    hideKeyboard(it)
                    bind.loader.isVisible = true

                    /*viewModel.createOrder(
                        shippingId.toString().request(),
                        productId.request(),
                        cardId.request(),
                        promoCode.ifEmpty { null }?.request(),
                        "1".request(),
                        selectedUserId.request(),
                        bind.message.value().request(),
                    )*/

                    
                }

            }
        }

        viewModel.createOrderRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    findNavController().navigate(ids.goToOrderStatusFragment)

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