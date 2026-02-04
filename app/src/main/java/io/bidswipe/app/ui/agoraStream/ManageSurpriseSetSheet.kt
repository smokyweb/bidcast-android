package io.bidswipe.app.ui.agoraStream

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.bidswipe.app.R
import io.bidswipe.app.controller.AvailableItemAdapter
import io.bidswipe.app.controller.UnsoldAdapterAdapter
import io.bidswipe.app.databinding.FragmentManageSurpriseSetSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.network.response.AuctionType
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.setHapticClickListener

class ManageSurpriseSetSheet : BottomSheetDialogFragment() {

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            from = arguments?.getString("from") ?: "live_show"
            auctionTypeId = arguments?.getInt("auction_type_id") ?: AuctionType.LIVE.id
        }

        bind.close.setHapticClickListener {
            dismiss()
        }

        val unSoldlist = mutableListOf(SurpriseProductModel("", "", 1), SurpriseProductModel("", "", 2))
        val unSoldAdapter = UnsoldAdapterAdapter(
            mList = unSoldlist,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                }
            })

        bind.unsoldItems.adapter = unSoldAdapter
        bind.unsoldHeading.title.text = "Unsold (${unSoldlist.sumOf { it.quantity ?: 0 }})"

        val availableList = mutableListOf(SurpriseProductModel("P1", "", 10), SurpriseProductModel("P2", "", 12))
        val availabledapter = AvailableItemAdapter(
            mList = availableList,
            object : RecyclerClicks {
                @SuppressLint("NotifyDataSetChanged")
                override fun itemClick(pos: Int, status: String?) {
                }
            })

        bind.availableItems.adapter = availabledapter
        bind.availableHeading.title.text = "Available (${availableList.sumOf { it.quantity ?: 0 }})"

    }

}