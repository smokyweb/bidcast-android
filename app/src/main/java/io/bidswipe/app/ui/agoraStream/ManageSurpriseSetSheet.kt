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
import android.widget.ArrayAdapter
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
import io.bidswipe.app.controller.UnsoldAdapterAdapter
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.databinding.FragmentManageSurpriseSetSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.PriceFormatter
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class ManageSurpriseSetSheet(var callBack: () -> Unit) : BottomSheetDialogFragment() {

    private lateinit var mCtx: Context

    private var _binding: FragmentManageSurpriseSetSheetBinding? = null
    private val bind get() = _binding!!

    val viewModel: DashViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
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
                unSoldList.addAll(it?.units ?: emptyList())
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

        val unSoldAdapter = UnsoldAdapterAdapter(
            mList = unSoldList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                }
            })

        bind.unsoldItems.adapter = unSoldAdapter
        bind.unsoldHeading.title.text = "Unsold (${unSoldList.count { it?.status == "available" }})"

        val availabledapter = AvailableItemAdapter(
            mList = availableList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                }
            })

        bind.availableItems.adapter = availabledapter
        bind.availableHeading.title.text = "Available (${availableList.sumOf { it?.quantity ?: 0 }})"

        bind.unsoldHeading.onCloseCLick {
            bind.unsoldExpandView.toggle()
        }

        bind.availableHeading.onCloseCLick {
            bind.availableExpandView.toggle()
        }

        bind.startAuction.setHapticClickListener {
            auctionSettingsSheet()

//                App.socketManager?.startAuctionBreakSpot(
//                    viewModel.currentRoomId,
//                    surpriseSet?.id.toString(),
//                    surpriseSet?.items?.first { it?.status=="available" }?.id.toString(),
//                    surpriseSet?.items?.first { it?.status=="available" }?.units?.first{it?.status=="available"}?.id.toString(),
//                    (surpriseSet?.price ?: 0.0).toString(),
//                    null, null, null,
//                )
//                dismiss()
        }
    }

    private fun auctionSettingsSheet() {
        var selectedCounterTimer = 5
        var selectedRequiredTime = 30

        val auctionSettingsSheetBind = AuctionSettingsSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.auction_settings_sheet,
                null,
                false
            )
        )

        val sheet = Alerts.appBottomSheet(mCtx, true, auctionSettingsSheetBind)

        val extraTimer = listOf(5, 7, 10)
        extraTimer.forEachIndexed { index, time ->
            val chip = Utils.makeAChip(
                mCtx = mCtx,
                text = "${time}s",
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                auctionSettingsSheetBind.timerChips.check(chip.id)
                selectedCounterTimer = time
            }
            auctionSettingsSheetBind.timerChips.addView(chip)
        }

        val requiredTimeList = listOf(15, 30, 45)
        val requiredTimeAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            requiredTimeList
        )

        auctionSettingsSheetBind.requiredTime.setAdapter(requiredTimeAdapter)

        auctionSettingsSheetBind.requiredTime.setText("30s", false)

        auctionSettingsSheetBind.requiredTime.setOnItemClickListener { _, _, position, _ ->
            selectedRequiredTime = requiredTimeList[position]
            auctionSettingsSheetBind.requiredTime.setText("${requiredTimeList[position]}s", false)
        }

        auctionSettingsSheetBind.requiredTime.setHapticClickListener {
            auctionSettingsSheetBind.requiredTime.showDropDown()
        }

        auctionSettingsSheetBind.startingBid.addTextChangedListener(
            PriceFormatter(
                auctionSettingsSheetBind.startingBid
            )
        )

        auctionSettingsSheetBind.startingBid.setText("1")

        auctionSettingsSheetBind.close.setHapticClickListener { sheet.dismiss() }

        auctionSettingsSheetBind.suddenDeath.setOnCheckedChangeListener { _, v ->
            auctionSettingsSheetBind.counterTimerLayout.isVisible = !v
            if (v) selectedCounterTimer = 0
        }

        auctionSettingsSheetBind.start.setHapticClickListener {

            when {
                selectedRequiredTime == 0 -> {
                    Alerts.error(mCtx, "Please select required time")
                    return@setHapticClickListener
                }

                selectedCounterTimer == 0 && !auctionSettingsSheetBind.suddenDeath.isChecked -> {
                    Alerts.error(mCtx, "Please select counter timer")
                    return@setHapticClickListener
                }

                auctionSettingsSheetBind.startingBid.value().isEmpty() -> {
                    Alerts.error(mCtx, "Please enter starting bid")
                    return@setHapticClickListener
                }

                else -> {
                    App.socketManager?.startAuctionBreakSpot(
                        viewModel.currentRoomId,
                        surpriseSet?.id.toString(),
                        surpriseSet?.items?.first { it?.status == "available" }?.id.toString(),
                        surpriseSet?.items?.first { it?.status == "available" }?.units?.first { it?.status == "available" }?.id.toString(),
                        auctionSettingsSheetBind.startingBid.value(),
                        selectedRequiredTime,
                        selectedCounterTimer,
                        auctionSettingsSheetBind.suddenDeath.isChecked,
                    )
                    sheet.dismiss()
                    dismiss()
                    callBack()
                }
            }

        }

        sheet.show()

    }

}