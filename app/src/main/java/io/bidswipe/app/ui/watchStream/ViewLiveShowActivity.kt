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
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr

class ViewLiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityViewLiveShowBinding::inflate)
	private val viewModel by viewModels<StreamViewModel>()

	private var pos = 0
	private var roomId = ""
	private var streamList = arrayListOf<StreamModel>()
	private lateinit var viewPager: ViewPager2
	private lateinit var streamPagerAdapter: StreamPagerAdapter
	private lateinit var socketUrl: String

	//    private var chatManager : ChatManager? = null
	private var socketManager: SocketManager? = null

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

		streamList = intent.getParcelableArrayListExtra<StreamModel>("streamList") as ArrayList<StreamModel>

		log("STREAM LIST: ${streamList[0].streamId} ")

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
				roomId = uri.getQueryParameter("showId").toString()
				// Use the param or the path to navigate or update UI
				log(" SHOW ID : $roomId")
			}
		}

		pos = streamList.indexOf(streamList.find { it.roomId == roomId })

		if (streamList.isNotEmpty()) {
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

		/*
				socketManager?.onRoomCreated { json ->

					val showData = LiveShowModel.fromJson(json)

					if (!streamList.contains(StreamModel(showData.showId.toString(), showData.rtcToken))){
						streamList.add(showData.showId.toString())
					}

					streamPagerAdapter.notifyDataSetChanged()

				}
		*/

		// Initialize ChatManager here if you want the ZIM SDK ready at Activity scope
		/*chatManager = ChatManager(
			application = application ,
			appId = Const.APP_ID.toLong() ,
			appSign = Const.APP_SIGN ,
			userId = userId ,
			userName = userName ,
			userImage = userImage
		)
*/
	}

	override fun onDestroy() {
		super.onDestroy()
		App.manager.destroyEngine()
	}

	private fun createEngine() {

		socketUrl = Const.SOCKET_URL
		socketManager = SocketManager.getInstance(this)
		socketManager?.initialize(socketUrl, mapOf("uid" to userId))
		socketManager?.connect(onConnected = {
//                socketManager?.emitViewerJoin(roomID)
		}) { err ->
			log("Socket connect error: $err")

		}

	}

	fun moveItem(list: MutableList<String>, fromIndex: Int, toIndex: Int) {
		if (fromIndex in list.indices && toIndex in list.indices) {
			val item = list.removeAt(fromIndex)
			list.add(toIndex, item)
		}
	}

}