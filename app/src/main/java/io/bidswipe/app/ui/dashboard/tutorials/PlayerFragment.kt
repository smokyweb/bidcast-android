package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPlayerBinding
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids

class PlayerFragment : BaseFragment<DashViewModel,FragmentPlayerBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentPlayerBinding.inflate(inflater, view, false)

    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }


        player = ExoPlayer.Builder(mCtx).build()
        bind.player.player = player

        bind.player.useController = true

        bind.player.showController()

        bind.nextButton.setOnClickListener {
            findNavController().navigate(ids.goToSellFragment)
        }




    }

}