package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.runSafe

class HomeFragment : BaseFragment<DashViewModel,FragmentHomeBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentHomeBinding.inflate(inflater,view,false)

    private lateinit var homeAdapter: HomeAdapter
    private var itemList = mutableListOf<String>()

    private val mClick  = object : RecyclerClicks{
        override fun viewClick(pos: Int) {

        }

        override fun itemClick(pos: Int, status: String) {

            when(status){

                "user" ->{

                    startActivity(Intent(mCtx,SellerProfileActivity::class.java))

                }
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeAdapter = HomeAdapter(itemList, mClick)

        bind.recycler.adapter = homeAdapter

        repeat(6){
            itemList.add("  ")

        }

        homeAdapter.notifyDataSetChanged()

        repeat(5){
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = "For You",
                    selected = false
                )
            )
        }

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }

    }

}