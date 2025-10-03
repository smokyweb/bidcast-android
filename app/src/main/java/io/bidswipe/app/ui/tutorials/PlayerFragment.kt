package io.bidswipe.app.ui.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPlayerBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class PlayerFragment : BaseFragment<DashViewModel, FragmentPlayerBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentPlayerBinding.inflate(inflater, view, false)

	private var lessonList = mutableListOf<String>()

	private var playPos = 0

	private var player: ExoPlayer? = null

	@OptIn(UnstableApi::class)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		player = ExoPlayer.Builder(mCtx).build()
		bind.player.player = player

		bind.player.useController = true
		bind.player.setShowSubtitleButton(true)
		bind.player.showController()
		bind.nextButton.setHapticClickListener {

			playPos = playPos + 1

			if (playPos < lessonList.size) {

				bind.header.setHeaderText("Lesson ${playPos + 1}/${lessonList.size}")

				player?.setMediaItem(
					MediaItem.Builder()
						.setUri(lessonList[playPos].toString()).build()
				)

				player?.prepare()

				player?.play()
			} else {
				findNavController().navigate(ids.goToSellFragment)
			}
//
		}

		/* val playerControlView = bind.player.findViewById<TextView>(R.id.mute)

		 playerControlView.setHapticClickListener {

			 playerControlView.setHapticClickListener {
				 // Toggle mute state
				 val currentMuteState = player?.isDeviceMuted
				 currentMuteState?.let { it1 -> player?.setDeviceMuted(!it1) }

				 // Change the button text based on the new mute state
				 if (currentMuteState == true) {
					 playerControlView.text = "Unmute"
				 } else {
					 playerControlView.text = "Mute"
				 }
			 }


		 }*/


		// Check if PlayerControlView is available


		bind.loader.isVisible = true

		viewModel.getLesson()

		viewModel.getLessonRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {

					viewModel.getLessonRepo.value = null
					bind.loader.isVisible = false

					val mData = it.value.data

					lessonList.clear()

					mData?.forEach {

						lessonList.add(it?.video.toString())

					}

					bind.header.setHeaderText("Lesson 1/${lessonList.size}")

					player?.setMediaItem(
						MediaItem.Builder()
							.setUri(lessonList[playPos].toString()).build()
					)

					player?.prepare()

					player?.play()

				}

				is Resource.Error -> {
					viewModel.getLessonRepo.value = null
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}
					})

				}

				else -> {}

			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		player = null
		player?.release()
	}

}