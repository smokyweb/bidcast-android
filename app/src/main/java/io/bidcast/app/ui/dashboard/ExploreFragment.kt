package io.bidcast.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.ExploreAdapter
import io.bidcast.app.databinding.FragmentExploreBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.utils.ids

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