package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentLiveRehearsalBinding
import io.bidswipe.app.ui.dashboard.DashViewModel

class LiveRehearsalFragment : BaseFragment<DashViewModel,FragmentLiveRehearsalBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentLiveRehearsalBinding.inflate(inflater,view,false)

    private val cameraListener = object : CameraListener() {

        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()

        }

        override fun onVideoRecordingEnd() {
            super.onVideoRecordingEnd()

        }
        override fun onVideoTaken(result : VideoResult) {
            super.onVideoTaken(result)
            log("VIDEO RECORDING FINISHED : " + result.file.absolutePath)
        }


        override fun onCameraError(exception : CameraException) {
            super.onCameraError(exception)
            exception.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.camera.also {
            it.setLifecycleOwner(viewLifecycleOwner)
            it.addCameraListener(cameraListener)
            it.facing = Facing.FRONT
            it.mode = Mode.VIDEO
        }


    }

    override fun onResume() {
        super.onResume()
        bind.camera.open()
    }

    override fun onPause() {
        super.onPause()
        bind.camera.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        bind.camera.destroy()
    }

}