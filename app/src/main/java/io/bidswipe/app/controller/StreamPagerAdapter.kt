package io.bidswipe.app.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.ui.dashboard.watchStream.WatchStreamFragment

class StreamPagerAdapter (
    fragmentActivity: FragmentActivity,
    private val streamList: List<StreamModel>,
    private val roomId: String
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = streamList.size

    override fun createFragment(position: Int): Fragment {

        var stream  = if (position==0){
            streamList.find { it.roomId == roomId }
        }else{
            streamList[position]
        }

        return WatchStreamFragment.newInstance(stream?.roomId.toString(), stream?.streamId ?:"" )
    }
}