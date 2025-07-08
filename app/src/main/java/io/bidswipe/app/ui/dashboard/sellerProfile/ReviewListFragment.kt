package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ReviewAdapter
import io.bidswipe.app.databinding.FragmentReviewListBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ReviewListFragment : BaseFragment<SellerViewModel, FragmentReviewListBinding>() {
    override fun getModel(): Class<SellerViewModel>  = SellerViewModel::class.java

    private lateinit var adapter : ReviewAdapter

    private var list = mutableListOf("","","")

    private var mClick  = object  : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {

        }

    }

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentReviewListBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReviewAdapter(list,mClick)

        bind.recycler.adapter = adapter

    }


}