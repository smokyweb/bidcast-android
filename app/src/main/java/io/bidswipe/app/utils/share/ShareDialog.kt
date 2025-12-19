package io.bidswipe.app.utils.share

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.bidswipe.app.controller.ShareChatAdapter
import io.bidswipe.app.controller.ShareTarget
import io.bidswipe.app.controller.ShareTargetAdapter
import io.bidswipe.app.databinding.ShareSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.ui.agoraStream.ProductsForLiveShowFragment
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.loadUrl
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable

object ShareHelper {
    fun openShareSheet(
        fragmentManager: FragmentManager,
        imageUrl: String?,
        text: String?,
        sellerInfo: Seller? = null,
        shareText: String? = null,
        type: String? = null,
        isLive: Boolean = false
    ) {
        val payload = SharePayload(
            imageUrl = imageUrl,
            text = text,
            sellerInfo = sellerInfo,
            shareText = shareText,
            type = type,
            isLive = isLive
        )
        ShareDialog.newInstance(payload).show(fragmentManager, "CustomShareSheet")
    }
}

class ShareDialog : BottomSheetDialogFragment() {

    private lateinit var payload: SharePayload

    companion object {
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_TEXT = "arg_text"
        private const val ARG_LINK = "arg_shareText"
        private const val ARG_SELLER = "arg_seller"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_IS_LIVE = "arg_isLive"


        fun newInstance(payload: SharePayload): ShareDialog {
            return ShareDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE, payload.imageUrl)
                    putString(ARG_TEXT, payload.text)
                    putString(ARG_LINK, payload.shareText)
                    putSerializable(ARG_SELLER, payload.sellerInfo)
                    putString(ARG_TYPE, payload.type)
                    putBoolean(ARG_IS_LIVE, payload.isLive)
                }
            }
        }
    }

    private var _binding: ShareSheetBinding? = null
    private val bind get() = _binding!!
    private lateinit var mCtx: Context

    private val chatList = mutableListOf<ChatModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mCtx = inflater.context
        _binding = ShareSheetBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        payload = SharePayload(
            imageUrl = requireArguments().getString(ARG_IMAGE),
            text = requireArguments().getString(ARG_TEXT),
            sellerInfo = requireArguments().getSerializable(ARG_SELLER) as Seller?,
            shareText = requireArguments().getString(ARG_LINK),
            type = requireArguments().getString(ARG_TYPE),
            isLive = requireArguments().getBoolean(ARG_IS_LIVE)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Log.d("TAG", "onViewCreated: $payload")

        chatList.clear()
        chatList.add(0, ChatModel())

        bind.showImg.loadUrl(mCtx, payload.imageUrl ?: "")
        bind.showTitle.text = payload.text?.capitalize()
        if (payload.sellerInfo != null) {
            bind.sellerInfo.isVisible = true
            bind.userName.text = payload.sellerInfo?.name
            bind.userImage.loadUrl(
                mCtx,
                payload.sellerInfo?.image ?: "",
                userName = payload.sellerInfo?.name ?: ""
            )
        } else {
            bind.sellerInfo.isVisible = false
        }

        when (payload.type) {
            "product" -> {
                bind.liveBadge.isVisible = false
                bind.shareTitle.text = "Share Product"
                bind.showSubtitle.isVisible = false
                bind.sellerCardView.isVisible = false
                bind.normalShareView.isVisible = true
            }

            "show" -> {
                bind.liveBadge.isVisible = payload.isLive
                bind.shareTitle.text = "Share Show"
                bind.showSubtitle.isVisible = true
                bind.sellerCardView.isVisible = false
                bind.normalShareView.isVisible = true
            }

            "seller" -> {
                bind.shareTitle.text = "Share Seller Profile"
                bind.showSubtitle.isVisible = false
                bind.sellerCardView.isVisible = true

                bind.sellerImage.loadUrl(mCtx, payload.sellerInfo?.image ?: "", userName = payload.sellerInfo?.name)
                bind.followers.text = payload.sellerInfo?.followers ?: "0"
                bind.rating.text = payload.sellerInfo?.rating ?: "0"
                bind.sellerName.text = payload.sellerInfo?.name?.capitalize()
            }

            else -> {
                bind.showSubtitle.isVisible = false
                bind.liveBadge.isVisible = false
                bind.sellerCardView.isVisible = false
                bind.normalShareView.isVisible = true
            }
        }

        bind.close.setOnClickListener { dismiss() }

        val targets = loadShareTargets()
        targets.add(0, ShareTarget("Copy"))
        bind.shareTargetsRecycler.adapter =
            ShareTargetAdapter(targets, mClicks = object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {
                    if (pos == 0) {
                        val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("share_text", payload.shareText)
                        clipboard.setPrimaryClip(clip)
                    } else {
                        val info = targets[pos].resolveInfo
                        if (info != null) {
                            shareTo(info)
                        }
                    }
                    dismiss()
                }
            })

        bind.chats.adapter = ShareChatAdapter(chatList, mClicks = object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                if (pos == 0) {
                    //search  sheet for user search
                    val bottomSheetFragment = SearchUsers()
                    bottomSheetFragment.arguments= bundleOf("share_text" to payload.shareText)
                    bottomSheetFragment.show(parentFragmentManager, "SEARCH_SHEET")
                } else {
                    var name = ""
                    var img = ""
                    var id = ""
                    if (chatList[pos].users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
                        name = chatList[pos].users?.receiverName.toString()
                        img = chatList[pos].users?.receiverImage.toString()
                        id = chatList[pos].users?.receiverId.toString()
                    } else {
                        name = chatList[pos].users?.senderName.toString()
                        img = chatList[pos].users?.senderImage.toString()
                        id = chatList[pos].users?.senderId.toString()
                    }

                    val intent = Intent(mCtx, ChatActivity::class.java).apply {
                        putExtra("id", id)
                        putExtra("name", name)
                        putExtra("image", img)
                        putExtra("productImage", payload.imageUrl)
                        putExtra("shareText", payload.shareText)
                    }
                    startActivity(intent)

                    dismiss()
                }
            }
        })

        FireRef.CHAT_LIST.child(Prefs(mCtx).getUserData()?.id.toString()).orderByChild("timestamp")
            .addValueEventListener(mValueEventListener)

    }

    private var mValueEventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snap: DataSnapshot) {

            chatList.clear()

            snap.children.forEach {
                val chat = ChatModel().fromMap(it)
                chatList.add(chat)
            }
            chatList.reverse()
            chatList.add(0, ChatModel())
            bind.chats.adapter?.notifyDataSetChanged()
        }

        override fun onCancelled(error: DatabaseError) {
            error.toException().printStackTrace()
        }
    }

    private fun loadShareTargets(): MutableList<ShareTarget> {
        val shareType = if (payload.imageUrl != null) "image/*" else "text/plain"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = shareType
        }

        val pm = requireContext().packageManager

        val resolvedActivities = pm.queryIntentActivities(intent, 0)

        val priorityList = listOf(
            "com.android.mms",
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.whatsapp"
        )

        val sortedResolveInfoList = resolvedActivities.sortedBy { resolveInfo ->
            when (resolveInfo.activityInfo.packageName) {
                in priorityList -> 0
                else -> 1
            }
        }

        return sortedResolveInfoList.map {
            ShareTarget(
                title = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm),
                resolveInfo = it
            )
        }.toMutableList()
    }

    private fun shareTo(resolveInfo: ResolveInfo) {
        // Capture screenshot of the specified view to share
        val screenshotUri = captureViewToUri(bind.screenshotView)

        val shareType = when {
            screenshotUri != null -> "image/*"
            payload.imageUrl != null -> "image/*"
            else -> "text/plain"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = shareType
            putExtra(Intent.EXTRA_TEXT, payload.shareText)
            when {
                screenshotUri != null -> {
                    putExtra(Intent.EXTRA_STREAM, screenshotUri)
                }

                payload.imageUrl != null -> {
                    putExtra(Intent.EXTRA_STREAM, payload.imageUrl)
                }
            }
            setClassName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(intent)
    }

    /**
     * Captures the given view (`screenshotView` in the layout) into a Bitmap,
     * writes it to a PNG file in the cache directory, and returns a content Uri.
     */
    private fun captureViewToUri(view: View): Uri? {
        if (view.width == 0 || view.height == 0) return null

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return try {
            val cacheDir = requireContext().cacheDir
            val file = File(cacheDir, "share_screenshot_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${requireContext().packageName}.provider"
            FileProvider.getUriForFile(requireContext(), authority, file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }
}

data class SharePayload(
    val imageUrl: String?,
    val text: String?,
    val sellerInfo: Seller? = null,
    val shareText: String? = "",
    var type: String? = "",
    var isLive: Boolean = false
)

data class Seller(
    val id: String? = null,
    val image: String? = null,
    val name: String? = null,
    val rating: String? = null,
    val followers: String? = null
) : Serializable