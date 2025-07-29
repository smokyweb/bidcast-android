package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zim.ZIM
import im.zego.zim.entity.ZIMAppConfig
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.StreamPagerAdapter
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.setMargins

class ViewLiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityViewLiveShowBinding::inflate)
    private val viewModel by viewModels<StreamViewModel>()

    private var pos = 0
    private var streamList = arrayListOf<LiveShowModel>()
    private lateinit var viewPager: ViewPager2
    private lateinit var streamPagerAdapter: StreamPagerAdapter

    private var eventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snapshot: DataSnapshot) {

            if (snapshot.childrenCount.toInt() != streamList.size){
                streamList.clear()
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    for (data in snapshot.children) {
                        log("EVENT LISTENER $data")

                        streamList.add(data.getValue(LiveShowModel::class.java)!!)

                    }

                }

                viewPager = bind.viewPager

                viewModel.setStreams(streamList)

                streamPagerAdapter = StreamPagerAdapter(this@ViewLiveShowActivity, viewModel)
                viewPager.adapter = streamPagerAdapter
                viewPager.currentItem = pos
                viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

            }


        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            navigationBarDarkIcon(true)
            navigationBarColor(clr.surface)
            supportActionBar(false)
            fitsSystemWindows(false)
            keyboardEnable(true)
        }

        bind.root.setMargins(0, 0, 0, navigationBarHeight)

        pos = intent.getIntExtra("position", 0)

//        streamList = intent.getParcelableArrayListExtra<StreamModel>("roomIdsList") !!

	    FireRef.LIVE_SESSIONS.addValueEventListener(eventListener)

//        log("ROOM IDS: ${streamList.get(0).roomId}")

        createEngine()

        val appConfig = ZIMAppConfig().also {
            it.appID = Const.APP_ID.toLong()
            it.appSign = Const.APP_SIGN
        }

       ZIM.create(appConfig, application)

    }

    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
       /* ZIM.getInstance().logout()
        ZIM.getInstance().destroy()*/

    }

    private fun createEngine() {
        val profile = ZegoEngineProfile().apply {
            appID = Const.APP_ID.toLong()
            appSign = Const.APP_SIGN
            scenario = ZegoScenario.BROADCAST
            application = applicationContext as Application
        }

        ZegoExpressEngine.createEngine(profile, null)

    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

}