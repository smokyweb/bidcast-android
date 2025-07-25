package io.bidswipe.app.ui.dashboard.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PaymentCardAdapter
import io.bidswipe.app.controller.ShippingAddressAdapter
import io.bidswipe.app.databinding.FragmentPaymentShippingBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.AddPaymentCardActivity
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class PaymentShippingFragment : BaseFragment<MoreViewModel, FragmentPaymentShippingBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentPaymentShippingBinding.inflate(inflater, view, false)

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()
    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()

    private lateinit var cardAdapter: PaymentCardAdapter
    private lateinit var shippingAddressAdapter: ShippingAddressAdapter

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when (status) {
                "default" -> {
                    bind.loader.isVisible = true
                    viewModel.setDefaultShippingAddress(addressList[pos]?.id.toString().request())
                }

                "delete" -> {
                    bind.loader.isVisible = true
                    viewModel.deleteAddress(addressList[pos]?.id.toString().request())
                }

            }

        }

    }

    private val cardClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when (status) {
                "default" -> {
                    bind.loader.isVisible = true
                    viewModel.setDefaultCard(cardList[pos]?.cardId.toString().request())
                }

                "delete" -> {
                    bind.loader.isVisible = true
                    viewModel.deleteCard(cardList[pos]?.cardId.toString().request())
                }

            }

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        cardAdapter = PaymentCardAdapter(cardList, cardClick)

        bind.recycler.adapter = cardAdapter

        shippingAddressAdapter = ShippingAddressAdapter(addressList, mClick)

        bind.addressRecycler.adapter = shippingAddressAdapter

        bind.addPaymentCard.setOnClickListener {
            startActivity(Intent(mCtx, AddPaymentCardActivity::class.java))
        }

        bind.addNewAddress.setOnClickListener {
            findNavController().navigate(ids.goToAddShippingAddressFragment)
        }

        bind.loader.isVisible = true

        viewModel.getPaymentCard()

        viewModel.getShippingAddress()

        viewModel.getShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    addressList.clear()

                    if (mData?.isNotEmpty() == true) {
                        bind.noAddressData.isVisible = false
                        addressList.addAll(mData)
                    } else {
                        bind.noAddressData.isVisible = true
                    }

                    shippingAddressAdapter.notifyDataSetChanged()

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

                    if (mData?.isNotEmpty() == true) {
                        bind.noCardsData.isVisible = false
                        cardList.addAll(mData)
                    } else {
                        bind.noCardsData.isVisible = true
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

        viewModel.setDefaultShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    val mData = it.value.data

                    viewModel.getShippingAddress()

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

        viewModel.setDefaultCardRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    val mData = it.value.data

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

        viewModel.deleteCardRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    val mData = it.value.data

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

        viewModel.deleteAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    val mData = it.value.data

                    viewModel.getShippingAddress()

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