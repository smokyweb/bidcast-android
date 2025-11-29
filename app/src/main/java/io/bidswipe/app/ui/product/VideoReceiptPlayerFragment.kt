package io.bidswipe.app.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentVideoReceiptPlayerBinding
import io.bidswipe.app.utils.setHapticClickListener

@OptIn(UnstableApi::class)
class VideoReceiptPlayerFragment : BaseFragment<ProductViewModel, FragmentVideoReceiptPlayerBinding>() {
	override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	): FragmentVideoReceiptPlayerBinding = FragmentVideoReceiptPlayerBinding.inflate(inflater, view, false)

	private var exoPlayer: ExoPlayer? = null
	private var videoUrl: String? = null
	private var isMuted = false
	private var areSubtitlesEnabled = false
	private var soundButton: MaterialButton? = null
	private var captionButton: MaterialButton? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		videoUrl = arguments?.getString("videoUrl")

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		if (!videoUrl.isNullOrEmpty()) {
			initializePlayer()
		} else {
			errorToast("Video URL not found")
			findNavController().popBackStack()
		}
	}

	@OptIn(UnstableApi::class)
	private fun initializePlayer() {
		videoUrl?.let { url ->
			bind.loader.isVisible = true

			exoPlayer = ExoPlayer.Builder(mCtx).build()
			bind.playerView.player = exoPlayer
			bind.playerView.useController = true
			bind.playerView.setShowSubtitleButton(false)

			val mediaItem = MediaItem.Builder()
				.setUri(url)
				.build()

			exoPlayer?.setMediaItem(mediaItem)
			exoPlayer?.prepare()

			setupControllerButtons()

			exoPlayer?.addListener(object : Player.Listener {
				override fun onPlaybackStateChanged(playbackState: Int) {
					if (playbackState == Player.STATE_READY) {
						bind.loader.isVisible = false
					}
				}

				override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
					bind.loader.isVisible = false
					errorToast("Failed to load video: ${error.message}")
				}
			})

			exoPlayer?.play()
		}
	}

	private fun setupControllerButtons() {
		bind.playerView.post {
			val controllerView = bind.playerView.findViewById<ViewGroup>(R.id.buttonLayout)
			controllerView?.let { layout ->
				soundButton = layout.findViewById(R.id.btnSound)
				captionButton = layout.findViewById(R.id.btnCaption)

				soundButton?.setHapticClickListener {
					toggleMute()
				}

				captionButton?.setHapticClickListener {
					toggleSubtitles()
				}

				updateSoundButtonState()

				updateCaptionButtonState()
			}
		}
	}

	private fun toggleMute() {
		exoPlayer?.let { player ->
			isMuted = !isMuted
			player.volume = if (isMuted) 0f else 1f
			updateSoundButtonState()
		}
	}

	private fun updateSoundButtonState() {
		soundButton?.let { button ->
			if (isMuted) {
				button.icon = ContextCompat.getDrawable(mCtx, R.drawable.ic_sound_off)
				button.text = getString(R.string.unmute)
				button.iconTint = ContextCompat.getColorStateList(mCtx, R.color.error)
			} else {
				button.icon = ContextCompat.getDrawable(mCtx, R.drawable.ic_sound)
				button.text = getString(R.string.sound)
				button.iconTint = ContextCompat.getColorStateList(mCtx, R.color.onSurface)
			}
		}
	}

	private fun toggleSubtitles() {
		exoPlayer?.let { player ->
			val trackGroups = player.currentTracks.groups
			val textTracks = trackGroups.filter { 
				it.type == C.TRACK_TYPE_TEXT
			}
			
			if (textTracks.isNotEmpty()) {
				areSubtitlesEnabled = !areSubtitlesEnabled
				val currentParams = player.trackSelectionParameters
				
				if (areSubtitlesEnabled) {
					val firstTextTrack = textTracks.first()
					val trackSelectionOverride = TrackSelectionOverride(
						firstTextTrack.mediaTrackGroup,
						0
					)
					
					player.trackSelectionParameters = currentParams
						.buildUpon()
						.setOverrideForType(trackSelectionOverride)
						.build()
				} else {
					player.trackSelectionParameters = currentParams
						.buildUpon()
						.clearOverridesOfType(C.TRACK_TYPE_TEXT)
						.build()
				}
				
				updateCaptionButtonState()
			} else {
				errorToast("No subtitles available for this video")
			}
		}
	}

	private fun updateCaptionButtonState() {
		captionButton?.let { button ->
			if (areSubtitlesEnabled) {
				button.iconTint = ContextCompat.getColorStateList(mCtx, R.color.primary)
				button.setTextColor(ContextCompat.getColor(mCtx, R.color.primary))
			} else {
				button.iconTint = ContextCompat.getColorStateList(mCtx, R.color.onSurface)
				button.setTextColor(ContextCompat.getColor(mCtx, R.color.onSurface))
			}
		}
	}

	private fun releasePlayer() {
		exoPlayer?.release()
		exoPlayer = null
		soundButton = null
		captionButton = null
	}

	override fun onPause() {
		super.onPause()
		exoPlayer?.pause()
	}

	override fun onResume() {
		super.onResume()
		exoPlayer?.play()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		releasePlayer()
	}
}