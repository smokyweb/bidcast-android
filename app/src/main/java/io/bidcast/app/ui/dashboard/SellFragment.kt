package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.gms.common.internal.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.ExploreAdapter
import io.bidcast.app.databinding.FragmentSellBinding
import io.bidcast.app.databinding.SellBottomSheetBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.utils.Alerts

class SellFragment : BaseFragment<DashViewModel,FragmentSellBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellBinding.inflate(inflater,view, false)

    private lateinit var imageSheet: BottomSheetBehavior<ConstraintLayout>
    private lateinit var exploreAdapter : ExploreAdapter
    private var exploreList = mutableListOf<String>()

    private lateinit var sellBottomSheetBind : SellBottomSheetBinding
    private lateinit var selectLocationSheet: BottomSheetDialog

    private val mClick = object : RecyclerClicks{
        override fun viewClick(pos: Int) {

        }

        override fun itemClick(pos: Int, status: String) {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sellBottomSheetBind = SellBottomSheetBinding.bind(layoutInflater.inflate(R.layout.sell_bottom_sheet, null, false))
        selectLocationSheet = Alerts.appBottomSheet(mCtx, true, sellBottomSheetBind)

        repeat(5){
            exploreList.add("")
        }

        exploreAdapter = ExploreAdapter(exploreList,mClick)

        sellBottomSheetBind.recycler.adapter = exploreAdapter

        selectLocationSheet.show()




        /*bind.recycler.adapter = exploreAdapter


        setupImageSheet()*/


    }


    /*private fun setupImageSheet() {
        BottomSheetBehavior.from(bind.imageSheet)

        imageSheet = BottomSheetBehavior.from(bind.imageSheet).also {
            it.peekHeight = 0
            it.isHideable = true
            it.isDraggable = false
            it.isFitToContents = false
        }

        imageSheet.addBottomSheetCallback(mSheetCallback)

        imageSheet.state = BottomSheetBehavior.STATE_EXPANDED

        bind.close.setOnClickListener {
            imageSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }


    }*/

    private val mSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    /*val params = CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.MATCH_PARENT,
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 0)
                    bind.coOrdinate.setLayoutParams(params)*/

                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                }

                BottomSheetBehavior.STATE_DRAGGING -> {
                }

                BottomSheetBehavior.STATE_HALF_EXPANDED -> {

                }

                BottomSheetBehavior.STATE_SETTLING -> {

                }

                BottomSheetBehavior.STATE_COLLAPSED -> {

//                    bind.imageSheet.isVisible =false

                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset > 0) {
                try {
//						bind.commentSheet.sheetRoot.itemClick.alpha = slideOffset
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}