package io.bidswipe.app.ui.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPlayerBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class PlayerFragment : BaseFragment<DashViewModel, FragmentPlayerBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentPlayerBinding.inflate(inflater, view, false)

    private var lessonList = mutableListOf<GetLessonsResponse.Data?>()
    private var playPos = 0
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        setupPlayer()
        setupClicks()

        bind.loader.isVisible = true
        viewModel.getLesson()

        observeLessons()
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        player = ExoPlayer.Builder(mCtx).build()
        bind.player.player = player
        bind.playerControls.player = player

        bind.player.useController = false
        bind.playerControls.show()
    }

    private fun setupClicks() {

        bind.previousButton.setHapticClickListener {
            if (playPos > 0) {
                playPos--
                playLesson(playPos)
            }
        }

        bind.nextButton.setHapticClickListener {

            if (playPos < lessonList.size - 1) {
                playPos++
                playLesson(playPos)
            } else {
                findNavController().navigate(ids.goToSellFragment)
            }
        }
    }

    private fun observeLessons() {
        viewModel.getLessonRepo.observe(viewLifecycleOwner) {
            when (it) {

                is Resource.Success -> {
                    viewModel.getLessonRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    playPos = 0
                    lessonList.clear()
                    lessonList.addAll(mData ?: emptyList())

                    if (lessonList.isNotEmpty()) {
                        playLesson(playPos)
                    }
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

    private fun playLesson(position: Int) {
        val lesson = lessonList.getOrNull(position) ?: return
        val lessonVideo = lesson.video.orEmpty()
        if (lessonVideo.isEmpty()) return

        // UI Content
        bind.header.setHeaderText("Lesson")
        bind.lessonCount.text = "Lesson ${position + 1}/${lessonList.size}"
        bind.lessonTitle.text = lesson.title.orEmpty()
        bind.lessonDescription.text = HtmlCompat.fromHtml(
            lesson.description.orEmpty(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).trim()

        bind.previousButton.visibility =
            if (position > 0) View.VISIBLE else View.INVISIBLE

        // Player setup
        player?.apply {
            stop()
            clearMediaItems()

            setMediaItem(
                MediaItem.Builder()
                    .setUri(lessonVideo)
                    .build()
            )
            prepare()
            playWhenReady = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
    }
}