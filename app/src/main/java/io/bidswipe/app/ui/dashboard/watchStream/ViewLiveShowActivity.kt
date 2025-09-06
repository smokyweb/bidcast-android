package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import im.zego.zegoexpress.constants.ZegoScenario
import io.bidswipe.app.utils.ChatManager
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.StreamPagerAdapter
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.StreamingManager
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.runSafe

class ViewLiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityViewLiveShowBinding::inflate)
	private val viewModel by viewModels<StreamViewModel>()

	private var pos = 0
	private var streamList = arrayListOf<LiveShowModel>()
	private lateinit var viewPager : ViewPager2
	private lateinit var streamPagerAdapter : StreamPagerAdapter
	private var chatManager : ChatManager? = null
	private var streamingManager : StreamingManager? = null

	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {

			runSafe {
				if (snapshot.childrenCount.toInt() != streamList.size) {
					streamList.clear()
					if (snapshot.exists() && snapshot.childrenCount > 0) {
						for (data in snapshot.children) {
							log("EVENT LISTENER Stream Data ${LiveShowModel().fromMap(data)}")
							streamList.add(LiveShowModel().fromMap(data))
						}
					}

					if (streamList.isNotEmpty()) {
						viewPager = bind.viewPager

						viewModel.setStreams(streamList)

						streamPagerAdapter = StreamPagerAdapter(this@ViewLiveShowActivity , viewModel)
						viewPager.adapter = streamPagerAdapter
						viewPager.currentItem = pos
						viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
					} else {
						finishAfterTransition()
					}

				}
			}

		}

		override fun onCancelled(error : DatabaseError) {

		}

	}

	override fun onCreate(savedInstanceState : Bundle?) {
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

		val showId = intent.getStringExtra("showId")

//		streamList.find { it.showId == showId }

		pos = streamList.indexOf(streamList.find { it.showId == showId })

		FireRef.LIVE_SESSIONS.addValueEventListener(eventListener)

		createEngine()

		// Initialize ChatManager here if you want the ZIM SDK ready at Activity scope
		chatManager = ChatManager(
			application = application ,
			appId = Const.APP_ID.toLong() ,
			appSign = Const.APP_SIGN ,
			userId = userId ,
			userName = userName ,
			userImage = userImage
		)

	}

	override fun onDestroy() {
		super.onDestroy()
		destroyEngine()
		chatManager?.shutdown()
	}

	private fun createEngine() {
		streamingManager = StreamingManager.getInstance(applicationContext)
		streamingManager?.createEngine(
			appId = Const.APP_ID.toLong() ,
			appSign = Const.APP_SIGN ,
			scenario = ZegoScenario.BROADCAST
		)

	}

	private fun destroyEngine() {
		streamingManager?.destroyEngine()
	}

}