package io.bidswipe.app.ui.watchStream

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ktx.immersionBar
import io.agora.rtc2.Constants
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.StreamPagerAdapter
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr

class ViewLiveShowActivity : BaseActivity() {

    val bind by bind(ActivityViewLiveShowBinding::inflate)
    private val viewModel by viewModels<StreamViewModel>()

    private var pos = 0
    private var roomId = ""
    private var streamList = arrayListOf<StreamModel>()
    private lateinit var viewPager: ViewPager2
    private lateinit var streamPagerAdapter: StreamPagerAdapter

    // Callback for PIP mode entry confirmation
    private var pipModeEnteredCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        immersionBar {
            transparentBar()
            navigationBarDarkIcon(true)
            navigationBarColor(clr.transparent)
            supportActionBar(false)
            fitsSystemWindows(false)
            keyboardEnable(true)
        }

        roomId = intent.getStringExtra("roomId") ?: ""

        if (intent.hasExtra("streamList"))
            streamList = intent.getParcelableArrayListExtra<StreamModel>("streamList") as ArrayList<StreamModel>

        App.manager = AgoraManager(this, Const.APP_ID_AGORA)

        requestPerms(Const.PERMISSIONS) {
            if (it) {
                App.manager.initializeAgoraSDK(Constants.CLIENT_ROLE_AUDIENCE)
            } else {
                errorToast("Permissions not granted!")
            }
        }

        if (streamList.isEmpty()) {
            finishAfterTransition()
        }

        if (roomId.isNotEmpty()) {
            val data: Uri? = intent.data
            data?.let { uri ->
                uri.getQueryParameter("showId")?.takeIf { it.isNotEmpty() }?.let { deepLinkRoomId ->
                    roomId = deepLinkRoomId
                }
                log(" SHOW ID : $roomId")
            }
        }

        if (streamList.isNotEmpty()) {
            val targetIndex = streamList.indexOfFirst { it.roomId == roomId }

            if (targetIndex > 0) {
                val selectedStream = streamList[targetIndex]
                streamList[targetIndex] = streamList[0]
                streamList[0] = selectedStream
                pos = 0
            } else {
                pos = targetIndex.takeIf { it >= 0 } ?: 0
            }

            viewPager = bind.viewPager
            viewModel.setStreams(streamList)
            log("STREAM LIST : $streamList")
            streamPagerAdapter = StreamPagerAdapter(this@ViewLiveShowActivity, viewModel)
            viewPager.adapter = streamPagerAdapter
            viewPager.currentItem = pos
            viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        } else {
            finishAfterTransition()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.manager.destroyEngine()
    }


    fun setPipModeEnteredCallback(callback: (() -> Unit)?) {
        pipModeEnteredCallback = callback
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // This is called when user presses Home button or opens another app
        // You can optionally enter PIP here, but we're doing it manually on shop click
    }

}