package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MessagesAdapter
import io.bidswipe.app.databinding.FragmentMessagesBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.FireRef
import io.bidswipe.app.utils.Prefs

class MessagesFragment : BaseFragment<DashViewModel, FragmentMessagesBinding>() {


    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentMessagesBinding.inflate(inflater, view, false)

    private lateinit var messagesAdapter: MessagesAdapter
    private val chatList = mutableListOf<ChatModel>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesAdapter = MessagesAdapter(chatList, mClicks)
        bind.recycler.adapter = messagesAdapter

        bind.noInternet.onClick {
            bind.noInternet.isVisible = false

        }

    }

    private var mValueEventListener = object : ValueEventListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onDataChange(snap: DataSnapshot) {
            Alerts.log(TAG, "CHAT READ $snap ")
            chatList.clear()
            snap.children.forEach {
                val chat = ChatModel().fromMap(it)
                chatList.add(chat)
            }

            if (snap.value.toString() == "null") {
                bind.noData.isVisible = true
                bind.recycler.isVisible = false
            } else {
                bind.noData.isVisible = false
                bind.recycler.isVisible = true
            }

            chatList.reverse()
            messagesAdapter.notifyDataSetChanged()

            bind.loader.isVisible = false

        }

        override fun onCancelled(error: DatabaseError) {
            error.toException().printStackTrace()
            bind.loader.isVisible = false
            Alerts.log(TAG, "CHAT READ ERROR : ${error.message}")
        }
    }
    private val mClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
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

            updateChat(id)
            val intent = Intent(mCtx, ChatActivity::class.java).apply {
                putExtra("id", id)
                putExtra("name", name)
                putExtra("image", img)
            }
            startActivity(intent)

        }
    }

    override fun onStart() {
        super.onStart()
        FireRef.CHAT_LIST.child(userId).orderByChild("timestamp")
            .addValueEventListener(mValueEventListener)
    }

    override fun onStop() {
        super.onStop()
        FireRef.CHAT_LIST.child(userId).orderByChild("timestamp")
            .removeEventListener(mValueEventListener)
    }

    private fun updateChat(id: String) {
        FireRef.CHAT_LIST.child(userId).orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        if ((id == it.child("users")
                                .child("receiverId").value) || (id == it.child("users")
                                .child("senderId").value)
                        ) {
                            it.child("seen").ref.setValue(true)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                    Alerts.log("Chat List", "CHAT READ ERROR : ${error.message}")
                }
            })
    }


}