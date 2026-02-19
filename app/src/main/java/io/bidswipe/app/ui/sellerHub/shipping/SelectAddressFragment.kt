package io.bidswipe.app.ui.sellerHub.shipping

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.controller.AvailableItemAdapter
import io.bidswipe.app.controller.ShippingAddressAdapter
import io.bidswipe.app.controller.UnsoldItemsAdapter
import io.bidswipe.app.databinding.FragmentManageSurpriseSetSheetBinding
import io.bidswipe.app.databinding.FragmentSelectAddressBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import kotlin.getValue

class SelectAddressFragment (var callBack: (address: GetShippingAddressResponse.Data?) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var mCtx: Context

    private var _binding: FragmentSelectAddressBinding? = null
    private val bind get() = _binding!!

    val viewModel: SellerHubViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            runSafe {
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet = bottomSheetDialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as FrameLayout?

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bottomSheetDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    bottomSheetDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    bottomSheetDialog.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.background)
                }

                bottomSheet?.let {
                    val layoutParams = it.layoutParams
                    layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                    it.layoutParams = layoutParams
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = true
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mCtx = inflater.context
        _binding = FragmentSelectAddressBinding.inflate(inflater, container, false)
        return bind.root
    }

    private lateinit var shippingAddressAdapter: ShippingAddressAdapter
    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()

    private var addAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                bind.loader.isVisible = true
                viewModel.getShippingAddress()
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.close.setHapticClickListener {
            dismiss()
        }

        shippingAddressAdapter = ShippingAddressAdapter(addressList, object : RecyclerClicks{
            override fun itemClick(pos: Int, status: String?) {
                callBack(addressList[pos])
                dismiss()
            }
        })

        bind.addressRecycler.adapter = shippingAddressAdapter

        bind.addNewAddress.setHapticClickListener {
            addAddressLauncher.launch(
                Intent(mCtx, MoreActivity::class.java).putExtra(
                    "slug",
                    "addAddress"
                )
            )
        }

        bind.loader.isVisible = true
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

                    it.parse(mCtx, javaClass.simpleName)
                }

                else -> {}

            }
        }
    }

}