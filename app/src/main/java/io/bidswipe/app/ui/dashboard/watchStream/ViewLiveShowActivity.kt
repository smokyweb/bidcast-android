package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.net.Uri
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
import io.bidswipe.app.model.LiveShowModelOld
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
	private var showId = ""
	private var publisherId = ""
	private var streamList = arrayListOf<LiveShowModelOld>()
	private lateinit var viewPager : ViewPager2
	private lateinit var streamPagerAdapter : StreamPagerAdapter
	private var chatManager : ChatManager? = null
	private var streamingManager : StreamingManager? = null
	
	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {
			runSafe {
//				if (snapshot.childrenCount.toInt() != streamList.size) {
					streamList.clear()
					if (snapshot.exists() && snapshot.childrenCount > 0) {
						for (data in snapshot.children) {
							log("EVENT LISTENER Stream Data ${LiveShowModelOld().fromMap(data)}")
							streamList.add(LiveShowModelOld().fromMap(data))
						}
					}
					log("EVENT LISTENER Stream List $showId")
					streamList.add(LiveShowModelOld(
						showId = showId,
						roomId = "live_room_${publisherId}_$showId"
					))
					
					pos = streamList.indexOf(streamList.find { it.showId == showId })
				
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

//				}
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

		 showId = intent.getStringExtra("showId") ?:""
		 publisherId = intent.getStringExtra("userId") ?:""

		if (showId.isNotEmpty()){
			val data: Uri? = intent.data
			data?.let { uri ->
				showId = uri.getQueryParameter("showId").toString()
				// Use the param or the path to navigate or update UI
				log(" SHOW ID : $showId" )
			}
		}

//		streamList.find { it.showId == showId }

		log("POSITION : $pos ")

		FireRef.LIVE_SESSIONS.addValueEventListener(eventListener)
//		createEngine()

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