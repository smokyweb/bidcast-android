package io.bidswipe.app.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.ui.dashboard.watchStream.WatchStreamFragment

class StreamPagerAdapter (
    fragmentActivity: FragmentActivity,
    private val streamList: List<LiveShowModel>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = streamList.size

    override fun createFragment(position: Int): Fragment {

        var stream  = streamList[position]

        return WatchStreamFragment.newInstance(stream.roomId.toString(), stream.roomId.toString())
    }
}