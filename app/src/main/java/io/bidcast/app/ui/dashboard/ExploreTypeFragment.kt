package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.HomeAdapter
import io.bidcast.app.databinding.FragmentExploreBinding
import io.bidcast.app.databinding.FragmentExploreTypeBinding
import io.bidcast.app.utils.Utils
import io.bidcast.app.utils.runSafe


class ExploreTypeFragment : BaseFragment<DashViewModel,FragmentExploreTypeBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentExploreTypeBinding.inflate(inflater,view,false)

    private lateinit var homeAdapter: HomeAdapter
    private var itemList = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        homeAdapter = HomeAdapter(itemList)

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