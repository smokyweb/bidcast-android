package io.bidswipe.app.ui.dashboard.scheduleShow

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.ktx.navigationBarHeight
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback
import im.zego.zegoexpress.callback.IZegoRoomLoginCallback
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.controller.LiveMoreAdapter
import io.bidswipe.app.controller.PromoteSheetAdapter
import io.bidswipe.app.controller.ShareSheetAdapter
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.ActivityLiveShowBinding
import io.bidswipe.app.databinding.CreateClipSheetBinding
import io.bidswipe.app.databinding.EndShowSheetBinding
import io.bidswipe.app.databinding.LiveShowMoreMenuBinding
import io.bidswipe.app.databinding.PromoteShowSheetBinding
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.databinding.ShopSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.CommentModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setMargins
import io.bidswipe.app.utils.value
import org.json.JSONObject

class LiveShowActivity : BaseActivity() {

    private val bind by bind(ActivityLiveShowBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    var isFrontCamera = true
    var roomID = ""
    var showId = ""
    private var liveStatus = true

    private var startTimeMillis: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var durationRunnable: Runnable

    private val updateStatusHandler = Handler(Looper.getMainLooper())
    private var runnable: Runnable ?= null

    private var commentList = mutableListOf<CommentModel?>()
    private lateinit var commentAdapter : CommentAdapter
    private var zoomLevel = 1

    private var eventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snapshot: DataSnapshot) {
            log("Value : ${snapshot.value}")

            if (snapshot.value == "sold") {

            } else {

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

        bind.root.setMargins(0,0,0,navigationBarHeight)

        commentAdapter = CommentAdapter(commentList)

        bind.recycler.adapter = commentAdapter

        showId= intent.getStringExtra("showId") ?:""

        createEngine()

        startListenEvent()

        bind.more.setOnClickListener {
            showMoreSheet()
        }

        bind.promote.setOnClickListener {
            showPromoteSheet()
        }

        bind.clip.setOnClickListener {
            createClipSheet()
        }

        bind.share.setOnClickListener {
            shareSheet()
        }

        bind.cutButton.setOnClickListener {
            endShowSheet()
        }

        bind.message.setEndIconOnClickListener {

           if (bind.text.value().isNotEmpty()){
               sendMessage(bind.text.value())
           }

        }

        bind.cameraSwitch.setOnClickListener {

            if (isFrontCamera){
                ZegoExpressEngine.getEngine().useFrontCamera(false)
                isFrontCamera = false

            }else{
                ZegoExpressEngine.getEngine().useFrontCamera(true)
                isFrontCamera = true

            }

        }

        bind.shop.setOnClickListener {

            shopSheet()

        }

        bind.loader.isVisible = true

        viewModel.generateToken(showId.request())

        bind.startBtn.setOnClickListener {

            bind.loader.isVisible = true

            viewModel.updateLiveStatus(showId.request(),"true".request())

        }

        viewModel.generateTokenRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    roomID =  mData?.roomId.toString()

                    log("ROOM ID FOR HOST: $roomID ")

                    loginRoom( mData?.roomId.toString())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                                finish()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                                finish()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.updateLiveStatusRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (liveStatus){

                        try {
                            addDataOnFirebase(mData)
                            startPublish()
                            startLiveDurationTimer()

                            startUpdatingFirebaseEvery5Minutes()
                            Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).child("product").child("status").addValueEventListener(eventListener)

                            bind.startBtn.isVisible = false
                        } catch (e: Exception) {
                           e.printStackTrace()
                        }

                    }else{
                        finish()
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                                finish()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                                finish()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

    }

    fun shopSheet() {
        var shopSheetBind = ShopSheetBinding.bind(layoutInflater.inflate(R.layout.shop_sheet, null, false))
        var shopSheet = Alerts.appBottomSheet(this, true, shopSheetBind)

        var productList = mutableListOf<GetMyInventoryResponse.Data?>()

        val categoryList = mutableListOf("Auction","Buy Now", "Freebie", "Sold")

        categoryList.forEach {it->
            shopSheetBind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = this, text =it , selected = false
                )
            )
        }

        shopSheetBind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }

       val shopAdapter = ShopSheetAdapter(productList,object  : RecyclerClicks{
           override fun itemClick(pos: Int, status: String?) {

               productList.forEachIndexed { index,item ->

                   item?.selected = index == pos

                   shopSheetBind.recycler.adapter?.notifyDataSetChanged()

               }

           }

       })

        shopSheetBind.recycler.adapter = shopAdapter

        /*shopSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })*/

        shopSheetBind.loader.isVisible = true

        viewModel.getMyInventory("active".request(),"1".request())

        viewModel.getMyInventoryRepo.observe(this) {
            when (it) {
                is Resource.Success -> {

                    shopSheetBind.loader.isVisible = false

                    val mData = it.value.data

                    productList.clear()

                    mData?.forEach {
                        productList.add(it)
                    }

                    shopAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    shopSheetBind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(this, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        shopSheetBind.close.setOnClickListener {
            shopSheet.dismiss()
        }

        shopSheet.show()
    }


    fun showMoreSheet() {
        var moreSheetBind = LiveShowMoreMenuBinding.bind(layoutInflater.inflate(R.layout.live_show_more_menu, null, false))
        var moreSheet = Alerts.appBottomSheet(this, true, moreSheetBind)


        moreSheetBind.optionList.adapter = LiveMoreAdapter(Const.liveMoreMenu, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        moreSheetBind.zoomInLayout.setOnClickListener {
            zoomLevel++
            ZegoExpressEngine.getEngine().setCameraZoomFactor(zoomLevel.toFloat())
            moreSheet.dismiss()
        }

        moreSheetBind.micLayout.setOnClickListener {
            if (ZegoExpressEngine.getEngine().isMicrophoneMuted){
                ZegoExpressEngine.getEngine().muteMicrophone(false)
                moreSheetBind.muteIcon.setImageResource(draw.ic_mic)
            }else{
                ZegoExpressEngine.getEngine().muteMicrophone(true)
                moreSheetBind.muteIcon.setImageResource(draw.ic_mute)
            }
//            moreSheet.dismiss()
        }

        moreSheetBind.switchCameraLayout.setOnClickListener {
            if (isFrontCamera){
                ZegoExpressEngine.getEngine().useFrontCamera(false)
                isFrontCamera = false

            }else{
                ZegoExpressEngine.getEngine().useFrontCamera(true)
                isFrontCamera = true
            }
            moreSheet.dismiss()
        }


        moreSheetBind.close.setOnClickListener {
            moreSheet.dismiss()
        }

        moreSheet.show()
    }

    fun showPromoteSheet() {
        var promoteSheetBind = PromoteShowSheetBinding.bind(layoutInflater.inflate(R.layout.promote_show_sheet, null, false))
        var promoteSheet = Alerts.appBottomSheet(this, true, promoteSheetBind)
        var mList = mutableListOf<String?>()

        repeat(3) {
            mList.add("")
        }

        promoteSheetBind.optionList.adapter = PromoteSheetAdapter( mList, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        promoteSheetBind.close.setOnClickListener {
            promoteSheet.dismiss()
        }


        promoteSheet.show()
    }

    fun createClipSheet() {
        var clipSheetBind = CreateClipSheetBinding.bind(layoutInflater.inflate(R.layout.create_clip_sheet, null, false))
        var clipSheet = Alerts.appBottomSheet(this, true, clipSheetBind)
        var mList = mutableListOf<String?>()

        repeat(3) {
            mList.add("")
        }


        clipSheetBind.close.setOnClickListener {
            clipSheet.dismiss()
        }


        clipSheet.show()
    }

    fun shareSheet() {
        var shareSheetBind = ShareSheetBinding.bind(layoutInflater.inflate(R.layout.share_sheet, null, false))
        var shareSheet = Alerts.appBottomSheet(this, true, shareSheetBind)
        var mList = mutableListOf<String?>()

        repeat(2) {
            mList.add("")
        }

        shareSheetBind.optionList.adapter = ShareSheetAdapter( mList, object : RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }
        })

        shareSheetBind.close.setOnClickListener {
            shareSheet.dismiss()
        }

        shareSheet.show()
    }

    fun endShowSheet() {
        var endShowSheetBind = EndShowSheetBinding.bind(layoutInflater.inflate(R.layout.end_show_sheet, null, false))
        var endShowSheet = Alerts.appBottomSheet(this, true, endShowSheetBind)

        endShowSheetBind.close.setOnClickListener {
            endShowSheet.dismiss()
        }

        endShowSheetBind.endBtn.setOnClickListener {
            endShowSheet.dismiss()
            stopUpdatingFirebase()
            liveStatus = false
            bind.loader.isVisible = true
            viewModel.updateLiveStatus(showId.request(),"false".request())
        }
        endShowSheet.show()

    }

    override fun onDestroy() {
        super.onDestroy()

        Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).removeValue()

        stopPublish()
        stopLiveDurationTimer()
        logoutRoom()
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

    private fun startListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {

            override fun onRoomStreamUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                streamList: ArrayList<ZegoStream>,
                extendedData: JSONObject
            ) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData)
                if (streamList.isNotEmpty()) {
                    val streamID = streamList[0].streamID
                }
            }

            override fun onRoomUserUpdate(
                roomID: String,
                updateType: ZegoUpdateType,
                userList: ArrayList<ZegoUser>
            ) {
                super.onRoomUserUpdate(roomID, updateType, userList)
                val context = applicationContext
                for (user in userList) {
                    val message = when (updateType) {
                        ZegoUpdateType.ADD -> "${user.userID} logged in to the room."
                        ZegoUpdateType.DELETE -> "${user.userID} logged out of the room."
                        else -> ""
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onRoomStateChanged(
                roomID: String,
                reason: ZegoRoomStateChangedReason,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onRoomStateChanged(roomID, reason, errorCode, extendedData)
                val context = applicationContext
                when (reason) {
                    ZegoRoomStateChangedReason.LOGIN_FAILED ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.LOGIN_FAILED", Toast.LENGTH_LONG).show()

                    ZegoRoomStateChangedReason.RECONNECT_FAILED ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.RECONNECT_FAILED", Toast.LENGTH_LONG).show()

                    ZegoRoomStateChangedReason.KICK_OUT ->
                        Toast.makeText(context, "ZegoRoomStateChangedReason.KICK_OUT", Toast.LENGTH_LONG).show()

                    else -> {
                        // Other room states can be handled here if needed
                    }
                }
            }

            override fun onPublisherStateUpdate(
                streamID: String,
                state: ZegoPublisherState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData)
                if (errorCode != 0) {
                    // Handle publish error
                }

                if (state == ZegoPublisherState.NO_PUBLISH) {
                    Toast.makeText(applicationContext, "ZegoPublisherState.NO_PUBLISH", Toast.LENGTH_LONG).show()
                }
            }

            override fun onPlayerStateUpdate(
                streamID: String,
                state: ZegoPlayerState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData)

                if (errorCode != 0) {
                    Toast.makeText(
                        applicationContext,
                        "onPlayerStateUpdate, state: $state errorCode: $errorCode",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (state == ZegoPlayerState.NO_PLAY) {
                    Toast.makeText(applicationContext, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_LONG).show()
                }
            }

            override fun onIMRecvBroadcastMessage(
                roomID: String?,
                messageList: java.util.ArrayList<ZegoBroadcastMessageInfo?>?
            ) {
                super.onIMRecvBroadcastMessage(roomID, messageList)
                Log.d("ZEGO", "Barrage message received for room: $roomID")
                if (messageList != null) {
                    for (msg in messageList) {
                        Log.d("CHAT", "Received message from ${msg?.fromUser?.userName}: ${msg?.message}")

                        val name = msg?.fromUser?.userID?.split("_")?.get(0)?.replace("."," ")

                        commentList.add(CommentModel(msg?.fromUser?.userName, name, msg?.message))
                        commentAdapter.notifyItemInserted(commentList.size - 1)
                        bind.recycler.post {  bind.recycler.smoothScrollToPosition(commentList.size) }
//                        bind.recycler.smoothScrollToPosition(commentList.lastIndex)
                        // Update UI accordingly
                    }
                }
            }
        })
    }

    private fun stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null)
    }

    fun loginRoom(roomId : String) {
        val user = ZegoUser(userName.replace(" ",".") + "_" + userId , userImage )

        val roomConfig = ZegoRoomConfig()
        roomConfig.isUserStatusNotify = true
        ZegoExpressEngine.getEngine().loginRoom(
            roomId,
            user,
            roomConfig,
            IZegoRoomLoginCallback { error: Int, extendedData: JSONObject? ->
                if (error == 0) {
                    Toast.makeText(this, "Login successful.", Toast.LENGTH_LONG).show()
                    startPreview()

                } else {
                    Toast.makeText(this, "Login failed. error = " + error, Toast.LENGTH_LONG).show()
                }
            })


    }

    fun logoutRoom() {
        ZegoExpressEngine.getEngine().logoutRoom()
    }

    fun startPreview() {
        val previewCanvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
        ZegoExpressEngine.getEngine().startPreview(previewCanvas)
    }

    fun stopPreview() {
        ZegoExpressEngine.getEngine().stopPreview()
    }

    fun startPublish() {
        val previewCanvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }
        ZegoExpressEngine.getEngine().startPreview(previewCanvas)
        val streamID: String = roomID
        ZegoExpressEngine.getEngine().startPublishingStream(streamID)
    }

    fun stopPublish() {
        ZegoExpressEngine.getEngine().stopPublishingStream()
    }

    fun sendMessage(message: String){

        log("RoomID: ${roomID} Message:${message}")

        ZegoExpressEngine.getEngine().sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
            @SuppressLint("NotifyDataSetChanged")
            override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
                if (errorCode == 0) {
                    Log.d("CHAT", "Message sent successfully")
                    bind.text.setText("")
                    commentList.add(CommentModel(userImage,userName,message))
                    commentAdapter.notifyItemInserted(commentList.size - 1)
                    bind.recycler.post {  bind.recycler.smoothScrollToPosition(commentList.size) }
                } else {
                    Log.e("CHAT", "Failed to send message")
                }
            }
        })

    }

    fun addDataOnFirebase(data: UpdateLiveStatusResponse.Data?){

        val prod = data?.products?.get(0)

        val user = data?.user

        val product = LiveShowModel.Product(category = prod?.categoryId.toString(), id = prod?.id.toString(), image = (prod?.images?.get(0) ?:"").toString(),name=prod?.title , price =prod?.pricing?.toString())

        val seller = LiveShowModel.Seller(
            id =user?.id.toString(),
            image = user?.profileImage,
            isFollowed = false,
            name = user?.name,
            rating = user?.rating ?:""
        )

        val liveShow= LiveShowModel(
            product = product,
            roomId = roomID,
            seller = seller,
            showDetail = "",
            thumbnail = data?.thumbnail?.get(0) ?:"",
            viewerCount = "",
            highestBid = "",
            isLive = true,
            time = Utils.getTimeFromTimestamp(System.currentTimeMillis()/1000,"yyyy-MM-dd_HH:mm:ss_a"),
            showId = showId
            )

        Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).setValue(liveShow).addOnCompleteListener {

        }

    }

    fun startUpdatingFirebaseEvery5Minutes() {

        runnable = object : Runnable {
            override fun run() {
                val updateValue = System.currentTimeMillis()
                Const.fireBaseRef.getReference(Const.LIVE_SESSIONS).child(roomID).child("time").setValue(Utils.getTimeFromTimestamp(System.currentTimeMillis()/1000,"yyyy-MM-dd_HH:mm:ss_a"))
                    .addOnSuccessListener {
                        Log.d("FirebaseUpdate", "Successfully updated value: $updateValue")
                    }
                    .addOnFailureListener {
                        Log.e("FirebaseUpdate", "Failed to update value", it)
                    }

                updateStatusHandler.postDelayed(this,  4 * 60 * 1000)
            }
        }

        runnable?.let { updateStatusHandler.post(it) }
    }

    fun stopUpdatingFirebase() {
        runnable?.let { updateStatusHandler.removeCallbacks(it) }
    }

    private fun startLiveDurationTimer() {
        startTimeMillis = System.currentTimeMillis()

        durationRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / (1000 * 60)) % 60
                val hours = (elapsed / (1000 * 60 * 60))

                val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                bind.duration.text = buildString {
                    append("Show Time: ")
                    append(formatted)
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(durationRunnable)
    }

    private fun stopLiveDurationTimer() {
        if (this::durationRunnable.isInitialized){
            handler.removeCallbacks(durationRunnable)
        }
    }

}