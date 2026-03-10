package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.controller.AvailableItemAdapter
import io.bidswipe.app.controller.UnsoldItemsAdapter
import io.bidswipe.app.databinding.FragmentManageSurpriseSetSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class ManageSurpriseSetSheet(var callBack: (status: String) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var mCtx: Context

    private var _binding: FragmentManageSurpriseSetSheetBinding? = null
    private val bind get() = _binding!!

    val viewModel: DashViewModel by activityViewModels()

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

    var from = "live_show"
    var auctionTypeId = AuctionType.LIVE.id

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mCtx = inflater.context
        _binding = FragmentManageSurpriseSetSheetBinding.inflate(inflater, container, false)
        return bind.root
    }

    private var surpriseSet: GetSurpriseProductsResponse.Data? = null
    private var availableList = mutableListOf<GetSurpriseProductsResponse.Data.Item?>()
    private var unSoldList = mutableListOf<GetSurpriseProductsResponse.Data.Item.Unit?>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            from = arguments?.getString("from") ?: "live_show"
            auctionTypeId = arguments?.getInt("auction_type_id") ?: AuctionType.LIVE.id
        }

        surpriseSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(
                "setData",
                GetSurpriseProductsResponse.Data::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("setData")
        }

        bind.close.setHapticClickListener {
            dismiss()
        }

        if (surpriseSet != null) {
            bind.desc.text = surpriseSet?.description
            bind.productName.text = surpriseSet?.name?.asCapital()
            bind.price.isVisible = surpriseSet?.type == "buy_it_now"
            bind.price.text = buildString {
                append(surpriseSet?.price?.toString()?.asMoney())
            }

            val totalQuantity = surpriseSet?.items?.sumOf { it?.quantity ?: 0 }
            val soldQuantity = surpriseSet?.items?.sumOf { it?.soldQuantity ?: 0 }
            bind.stepProgress.max = totalQuantity ?: 0
            bind.stepProgress.progress = (soldQuantity ?: 0)
            bind.itemsLeftText.text = buildString {
                append((totalQuantity ?: 0) - (soldQuantity ?: 0))
                append("/")
                append(totalQuantity ?: 0)
                append(" left")
            }

            surpriseSet?.items?.forEach {
                unSoldList.addAll(it?.units?.filter {it1-> it1?.status=="available" } ?: emptyList())
                availableList.add(it)
            }

            if (surpriseSet?.type == "auction") {
                bind.startAuctionLayout.isVisible = true
                bind.unsoldLayout.isVisible = false

                bind.soldCount.text = buildString {
                    append(surpriseSet?.items?.sumOf { it?.soldQuantity ?: 0 })
                    append(" of ")
                    append(surpriseSet?.items?.sumOf { it?.quantity ?: 0 })
                    append(" sold")
                }

            } else {
                bind.startAuctionLayout.isVisible = false
                bind.unsoldLayout.isVisible = true
            }
        }

        val unSoldAdapter = UnsoldItemsAdapter(
            mList = unSoldList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                    when (status) {
                        "start_auction" -> {
                            App.socketManager?.startAuctionBreakSpot(
                                viewModel.currentRoomId,
                                surpriseSet?.id.toString(),
                                unSoldList[pos]?.productSetItemId.toString(),
                                unSoldList[pos]?.id.toString(),
                                (unSoldList[pos]?.price ?: 0.0).toString(),
                                null, null, null,
                            )
                            dismiss()
                            callBack("dismiss")
                        }

                        "set_next" -> {

                        }
                    }
                }
            }) { position, price, desc ->
            bind.loader.isVisible = true
            viewModel.editSurpriseProduct(unSoldList[position]?.id.toString().request(), price.request(), desc.request())
        }

        bind.unsoldItems.adapter = unSoldAdapter
        bind.unsoldHeading.title.text = "Unsold (${unSoldList.count { it?.status == "available" }})"

        val availableAdapter = AvailableItemAdapter(
            mList = availableList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                }
            })

        bind.availableItems.adapter = availableAdapter
        bind.availableHeading.title.text = "Available (${availableList.sumOf { it?.quantity ?: 0 }})"

        bind.unsoldHeading.onCloseCLick {
            bind.unsoldExpandView.toggle()
        }

        bind.availableHeading.onCloseCLick {
            bind.availableExpandView.toggle()
        }

        bind.startAuction.setHapticClickListener {
            auctionSettingsSheet()
        }

        bind.deleteCard.setHapticClickListener {
            AppBottomSheet(
                mCtx,
                R.drawable.trash,
                "Delete!",
                "Are you sure you want to delete?",
                primaryBtnText = "Yes",
                secondaryBtnText = "No",
                canCancel = true,
                showSecondary = true,
                iconPadding = 16,
                alertType = AlertType.ERROR,
                clicks = object : AlertClicks {
                    override fun primaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                        bind.loader.isVisible = true
                        viewModel.deleteSurpriseSet(surpriseSet?.id.toString().request())
                    }

                    override fun secondaryClick(dialog: AppBottomSheet) {
                        dialog.dismiss()
                    }
                }

            ).show()

        }

        viewModel.deleteSurpriseSetRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.deleteSurpriseSetRepo.value=null
                    dismiss()
                    callBack("delete")
                }

                is Resource.Error -> {
                    viewModel.deleteSurpriseSetRepo.value=null
                      it.parse(mCtx)
                }

                else -> {}

            }
        }

        viewModel.editSurpriseProductRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.editSurpriseProductRepo.value=null
                    dismiss()
                    callBack("edit")
                }

                is Resource.Error -> {
                    viewModel.editSurpriseProductRepo.value=null
                    it.parse(mCtx)
                }

                else -> {}
            }
        }
    }

    private fun auctionSettingsSheet() {
        val initialPrice = (surpriseSet?.price ?: 1.0).toString()

        AuctionSettingsSheetHelper.show(
            context = mCtx,
            initialPrice = initialPrice
        ) { result ->
            val available = surpriseSet?.items?.find { it?.status == "available" }

            if (available != null) {
                val availableUnits = available.units?.find { it?.status == "available" }
                if (availableUnits != null) {
                    App.socketManager?.startAuctionBreakSpot(
                        viewModel.currentRoomId,
                        surpriseSet?.id.toString(),
                        available.id.toString(),
                        availableUnits.id.toString(),
                        result.startingBid,
                        result.requiredTimeSeconds,
                        result.counterTimerSeconds,
                        result.suddenDeath,
                    )
                } else {
                    Alerts.error(mCtx, "Something went wrong")
                }
            } else {
                Alerts.error(mCtx, "Something went wrong")
            }
            dismiss()
            callBack("dismiss")
        }
    }

}