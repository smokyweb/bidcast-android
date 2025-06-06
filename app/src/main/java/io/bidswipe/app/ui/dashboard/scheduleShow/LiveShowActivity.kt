package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.activity.viewModels
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind

class LiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        bind.more.setOnClickListener {
            showMoreSheet()
        }


    }

    fun showMoreSheet() {
        var moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu, null, false))
        var moreSheet = Alerts.appBottomSheet(this, true, moreSheetBind)


        moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        moreSheetBind.close.setOnClickListener {
            moreSheet.dismiss()
        }


        moreSheet.show()
    }
}