package io.bidswipe.app.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidswipe.app.ui.watchStream.StreamViewModel
import io.bidswipe.app.ui.watchStream.WatchStreamFragment
//import io.bidswipe.app.ui.watchStream.WatchStreamSocketFragment

class StreamPagerAdapter(
	fragmentActivity : FragmentActivity ,
	private val viewModel : StreamViewModel ,
//    private val streamList: List<LiveShowModel>
) : FragmentStateAdapter(fragmentActivity) {

	override fun getItemCount() : Int = viewModel.streams.value?.size ?: 0

	override fun createFragment(position : Int) : Fragment {
		val stream = viewModel.streams.value?.get(position) ?: throw IllegalStateException("Stream data not available")
		viewModel.selectStream(stream)
        return WatchStreamFragment.newInstance(stream.roomId, stream.streamId)
	}
}