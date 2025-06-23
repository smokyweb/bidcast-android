package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CustomSelectorAdapter
import io.bidswipe.app.databinding.FragmentSendGiftBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.layout
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class SendGiftFragment : BaseFragment<ProductViewModel, FragmentSendGiftBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSendGiftBinding.inflate(inflater, view, false)

    private var userList = mutableListOf<UserSearchingResponse.Data?>()

    lateinit var textWatcher: TextWatcher
    private var oldText=""
    private var shippingId = ""
    private var productId = ""
    private var cardId = ""
    private var promoCode = ""
    private var isLoading = false
    private var selectedUserId = ""

    private lateinit var userAdapter : CustomSelectorAdapter

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

        textWatcher =object :TextWatcher{
            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {

            }

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {

            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.toString()?.isNotEmpty() == true && p0.trim().toString()!=oldText){
                    oldText=p0.trim().toString()
                    if (isLoading == false){
                        isLoading = true
                        viewModel.searchUsers(search = p0.toString().request())
                    }

                }

            }

        }

            addTexWatcher()


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

                    viewModel.createOrder(
                        shippingId.toString().request(),
                        productId.request(),
                        cardId.request(),
                        promoCode.ifEmpty { null }?.request(),
                        "1".request(),
                        selectedUserId.request(),
                        bind.message.value().request(),
                        viewModel.checkoutData?.shippingCharges.toString().request(),
                        viewModel.checkoutData?.taxAmount.toString().request(),
                        viewModel.checkoutData?.subTotal.toString().request(),
                        viewModel.checkoutData?.total.toString().request()
                    )

                    
                }

            }
        }

        viewModel.createOrderRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data
                    findNavController().navigate(ids.goToOrderStatusFragment, bundleOf("orderId" to mData?.id.toString()))

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

        viewModel.searchUsersRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    val mData = it.value.data


                    userList.clear()

                    if (mData?.isNotEmpty() ==true){
                        userList.addAll(mData)
                    }

                    if (userList.isEmpty()){
                        bind.userNotFound.isVisible = true
                    }else{
                        bind.userNotFound.isVisible = false
                    }

                    userAdapter = CustomSelectorAdapter(
                        mCtx,
                        layout.user_selector_item,
                        userList
                    ) { index,name ->
                        oldText=name
                        bind.user.setText(name,false)

                        selectedUserId = userList[index]?.id.toString()
                        bind.user.dismissDropDown()
                    }

                    bind.user.setAdapter(userAdapter)
                    bind.user.showDropDown()

                    isLoading = false

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

    private fun addTexWatcher(){

        bind.user.post {  bind.user.addTextChangedListener(textWatcher) }

    }

}