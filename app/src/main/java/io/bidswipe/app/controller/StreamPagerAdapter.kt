package io.bidswipe.app.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.ui.dashboard.watchStream.StreamViewModel
import io.bidswipe.app.ui.dashboard.watchStream.WatchStreamFragment

class StreamPagerAdapter (
    fragmentActivity: FragmentActivity,
    private val viewModel: StreamViewModel
//    private val streamList: List<LiveShowModel>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = viewModel.streams.value?.size ?: 0

    override fun createFragment(position: Int): Fragment {

        var stream  = viewModel.streams.value?.get(position) ?: throw IllegalStateException("Stream data not available")

        viewModel.selectStream(stream)

        return WatchStreamFragment.newInstance(stream.roomId.toString(), stream.roomId.toString())
    }
}