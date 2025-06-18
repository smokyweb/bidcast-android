package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.entity.ZegoEngineProfile
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.StreamPagerAdapter
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind

class ViewLiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityViewLiveShowBinding::inflate)
    private val viewModel by viewModels<StreamViewModel>()

    private var pos  = 0
    private var streamList  = arrayListOf<LiveShowModel>()
    private lateinit var viewPager: ViewPager2
    private lateinit var streamPagerAdapter: StreamPagerAdapter

    private var eventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snapshot: DataSnapshot) {
            streamList.clear()

            if (snapshot.exists() && snapshot.childrenCount > 0) {
//                bind.noChats.isVisible = false
                for (data in snapshot.children) {
                    log("EVENT LISTENER " + data.toString())

                    streamList.add(data.getValue(LiveShowModel::class.java)!!)

                }

            } else {
//                bind.noChats.isVisible = true
            }

            viewPager = bind.viewPager

            viewModel.setStreams(streamList)

            streamPagerAdapter = StreamPagerAdapter(this@ViewLiveShowActivity, viewModel)
            viewPager.adapter = streamPagerAdapter
            viewPager.currentItem = pos
            viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL


        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        pos = intent.getIntExtra("position",0)

//        streamList = intent.getParcelableArrayListExtra<StreamModel>("roomIdsList") !!

        Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).addValueEventListener(eventListener)

//        log("ROOMIDS: ${streamList.get(0).roomId}")



        createEngine()

    }

    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
    }

    private fun createEngine() {
        val profile = ZegoEngineProfile().apply {
            appID = Const.APP_ID.toLong()
            appSign = Const.APP_SIGN
            scenario = ZegoScenario.GENERAL
            application = applicationContext as Application
        }

        ZegoExpressEngine.createEngine(profile, null)
    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

}