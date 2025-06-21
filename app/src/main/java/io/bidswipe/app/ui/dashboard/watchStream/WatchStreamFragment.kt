package io.bidswipe.app.ui.dashboard.watchStream

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback
import im.zego.zegoexpress.constants.ZegoViewMode
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo
import im.zego.zegoexpress.entity.ZegoCanvas
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CommentAdapter
import io.bidswipe.app.databinding.FragmentWatchStreamBinding
import io.bidswipe.app.model.CommentModel
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

class WatchStreamFragment : BaseFragment<StreamViewModel, FragmentWatchStreamBinding>() {
    override fun getModel(): Class<StreamViewModel> = StreamViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentWatchStreamBinding.inflate(inflater, view, false)

    private lateinit var roomID: String
    private lateinit var streamID: String
    private var commentList = mutableListOf<CommentModel?>()
    private lateinit var commentAdapter: CommentAdapter

    companion object {
        fun newInstance(roomID: String, streamID: String) = WatchStreamFragment().apply {
            arguments = Bundle().apply {
                putString("roomID", roomID)
                putString("streamID", streamID)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomID = requireArguments().getString("roomID") ?: ""
        streamID = requireArguments().getString("streamID") ?: ""
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log("RoomId: $roomID")

        commentAdapter = CommentAdapter(commentList)

        bind.recycler.adapter = commentAdapter

        bind.message.setEndIconOnClickListener {
            if (bind.text.value().isNotEmpty()) {
                sendMessage(bind.text.value())
            }

        }

        viewModel.selectedStream.observe(viewLifecycleOwner) { stream ->
            if (stream.roomId == roomID) {

                bind.userImage.loadUrl(
                    mCtx,
                    stream.seller?.image.toString(),
                    placeHolder = draw.user_image
                )

                bind.userName.text = stream.seller?.name.toString()
                bind.productName.text = stream.product?.name
                bind.productImage.loadUrl(
                    mCtx,
                    stream?.product?.image.toString(),
                    placeHolder = draw.product_img
                )
                bind.quantity.text = buildString {
                    append("Price: ")
                    append(stream.product?.price.toString())
                }

                bind.max.text = stream.product?.price.toString()

                bind.bid.onSlideCompleteListener = object : OnSlideCompleteListener {
                    override fun onSlideComplete(view: SlideToActView) {
                        log("SWIPED")

                        bind.loader.isVisible = true

                        viewModel.createBid(
                            stream.showId?.request(),
                            userId.request(),
                            stream.product?.id.toString().request(),
                            "100".request()
                        )

                    }
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
    }

    override fun onResume() {
        super.onResume()
        loginAndPlay()
        fetchMessage()
    }

    override fun onPause() {
        super.onPause()
        stopStream()
    }

    private fun loginAndPlay() {
        val user = ZegoUser(userName.replace(" ", ".") + "_" + userId, userImage)
        ZegoExpressEngine.getEngine().loginRoom(roomID, user, ZegoRoomConfig())
        val canvas = ZegoCanvas(bind.hostView).apply {
            viewMode = ZegoViewMode.ASPECT_FILL
        }

        ZegoExpressEngine.getEngine().startPlayingStream(roomID, canvas)
    }

    private fun stopStream() {
        ZegoExpressEngine.getEngine().stopPlayingStream(roomID)
        ZegoExpressEngine.getEngine().logoutRoom(roomID)
    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

    fun sendMessage(message: String) {

        log("RoomID: ${roomID} Message:${message}")

        ZegoExpressEngine.getEngine()
            .sendBroadcastMessage(roomID, message, object : IZegoIMSendBroadcastMessageCallback {
                override fun onIMSendBroadcastMessageResult(errorCode: Int, messageID: Long) {
                    if (errorCode == 0) {
                        bind.text.setText("")
                        commentList.add(CommentModel(userImage, userName, message))
                        commentAdapter.notifyItemInserted(commentList.size - 1)
                        bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
                        Log.d("CHAT", "Message sent successfully")
                    } else {
                        Log.e("CHAT", "Failed to send message")
                    }
                }
            })

    }

    fun fetchMessage() {

        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {

            override fun onIMRecvBroadcastMessage(
                roomID: String?,
                messageList: kotlin.collections.ArrayList<ZegoBroadcastMessageInfo?>?
            ) {
                Log.d("ZEGO", "Broadcast message received for room: $roomID")
                if (messageList != null) {
                    for (msgInfo in messageList) {
                        Log.d(
                            "BROADCAST",
                            "Received broadcast message from ${msgInfo?.fromUser?.userName}: ${msgInfo?.message}"
                        )

                        val name = msgInfo?.fromUser?.userID?.split("_")?.get(0)?.replace(".", " ")

                        commentList.add(
                            CommentModel(
                                msgInfo?.fromUser?.userName,
                                name,
                                msgInfo?.message
                            )
                        )
                        commentAdapter.notifyItemInserted(commentList.size - 1)
                        bind.recycler.post { bind.recycler.smoothScrollToPosition(commentList.size) }
                    }
                }
            }
        })
    }

}