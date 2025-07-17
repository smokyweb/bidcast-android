package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MessagesAdapter
import io.bidswipe.app.databinding.FragmentMessagesBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class MessagesFragment : BaseFragment<DashViewModel,FragmentMessagesBinding>() {


    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentMessagesBinding.inflate(inflater,view,false)

    private lateinit var messagesAdapter: MessagesAdapter

    private var mList = mutableListOf("","","")


    private var mClick = object : RecyclerClicks{
       
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesAdapter = MessagesAdapter(mList, mClick)
        bind.recycler.adapter = messagesAdapter

        bind.swipeRefreshLayout.setOnRefreshListener {

        }

        bind.noInternet.onClick {
            bind.noInternet.isVisible =false

        }

    }

}