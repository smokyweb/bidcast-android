package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.databinding.FragmentExploreTypeBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.runSafe


class ExploreTypeFragment : BaseFragment<DashViewModel,FragmentExploreTypeBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentExploreTypeBinding.inflate(inflater,view,false)

    private lateinit var homeAdapter: HomeAdapter
    private var itemList = mutableListOf<String>()

    private val mClick  = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {


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


        repeat(1){
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = "Gaming",
                    selected = true
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