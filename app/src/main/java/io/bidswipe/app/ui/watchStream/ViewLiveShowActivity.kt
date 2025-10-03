package io.bidswipe.app.ui.watchStream

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import io.bidswipe.app.utils.ChatManager
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.StreamPagerAdapter
import io.bidswipe.app.databinding.ActivityViewLiveShowBinding
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.SocketManager
import io.bidswipe.app.utils.StreamingManager
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.runSafe

class ViewLiveShowActivity : BaseActivity() {

	private val bind by bind(ActivityViewLiveShowBinding::inflate)
	private val viewModel by viewModels<StreamViewModel>()

	private var pos = 0
    private var roomId = ""
    private var publisherId = ""
    private var streamList = arrayListOf<String>()
	private lateinit var viewPager : ViewPager2
	private lateinit var streamPagerAdapter : StreamPagerAdapter
	private var chatManager : ChatManager? = null
	private var streamingManager : StreamingManager? = null
    private var roomIdsList = arrayListOf<String>()

    private lateinit var socketUrl: String

    //    private var chatManager : ChatManager? = null
    private var socketManager: SocketManager? = null
	
	private var eventListener = object : ValueEventListener {
		@SuppressLint("NotifyDataSetChanged")
		override fun onDataChange(snapshot : DataSnapshot) {
			runSafe {
//				if (snapshot.childrenCount.toInt() != streamList.size) {
                /*streamList.clear()
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    for (data in snapshot.children) {
                        log("EVENT LISTENER Stream Data ${LiveShowModelOld().fromMap(data)}")
                        streamList.add(LiveShowModelOld().fromMap(data))
                    }
                }


                log("EVENT LISTENER Stream List $showId")

            roomIdsList.forEach {
                streamList.add(LiveShowModelOld(
                    showId = it,
                    roomId = it
                ))
            }

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
*/
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

        roomId = intent.getStringExtra("roomId") ?: ""

        val roomIds = intent.getStringExtra("roomIdsList")

        publisherId = intent.getStringExtra("userId") ?: ""

        if (roomId.isNotEmpty()) {
            val data: Uri? = intent.data
            data?.let { uri ->
                roomId = uri.getQueryParameter("showId").toString()
                // Use the param or the path to navigate or update UI
                log(" SHOW ID : $roomId")
            }
        }

//		streamList.find { it.showId == showId }


//		FireRef.LIVE_SESSIONS.addValueEventListener(eventListener)
//		createEngine()

        streamList.clear()

        streamList.addAll(roomIds?.split(",") ?: emptyList())

        pos = streamList.indexOf(roomId)

		log("POSITION : $pos ")

        if (streamList.isNotEmpty()) {
            viewPager = bind.viewPager

            viewModel.setStreams(streamList)

            log("STREAM LIST : ${streamList}")

            streamPagerAdapter = StreamPagerAdapter(this@ViewLiveShowActivity, viewModel)
            viewPager.adapter = streamPagerAdapter
            viewPager.currentItem = pos
            viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        } else {
            finishAfterTransition()
        }

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
//		FireRef.LIVE_SESSIONS.removeEventListener(eventListener)
		destroyEngine()

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


        /*streamingManager = StreamingManager.getInstance(applicationContext)
        streamingManager?.createEngine(
            appId = Const.APP_ID.toLong() ,
            appSign = Const.APP_SIGN ,
            scenario = ZegoScenario.BROADCAST
        )*/

	}

	private fun destroyEngine() {
        socketManager?.disconnect()
//		streamingManager?.destroyEngine()
	}

}