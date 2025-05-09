package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.ids

class ExploreFragment : BaseFragment<DashViewModel,FragmentExploreBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentExploreBinding.inflate(inflater,view,false)

    private lateinit var exploreAdapter : ExploreAdapter
    private var exploreList = mutableListOf<String>()

    private val mClick = object : RecyclerClicks{
        override fun viewClick(pos: Int) {
            findNavController().navigate(ids.goTopExploreType)
        }

        override fun itemClick(pos: Int, status: String) {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repeat(5){
            exploreList.add("")
        }

        exploreAdapter = ExploreAdapter(exploreList,mClick)

        bind.recycler.adapter = exploreAdapter


    }

}