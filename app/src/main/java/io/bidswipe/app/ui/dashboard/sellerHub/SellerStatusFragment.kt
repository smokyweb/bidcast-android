package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OrdersAdapter
import io.bidswipe.app.controller.StatusAdapter
import io.bidswipe.app.databinding.FragmentSellerStatusBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class SellerStatusFragment : BaseFragment<SellerHubViewModel, FragmentSellerStatusBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSellerStatusBinding.inflate(inflater,view,false)

    private var itemList = mutableListOf("", "")

    private lateinit var adapter: StatusAdapter

    private val mClick = object : RecyclerClicks {

        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        adapter = StatusAdapter(itemList,mClick)

        bind.recycler.adapter = adapter

    }

}