package io.bidswipe.app.ui.agoraStream

import android.os.Build
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gyf.immersionbar.ktx.immersionBar
import io.agora.rtc2.IRtcEngineEventHandler
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAgoraPublisherBinding
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins


class AgoraPublisherActivity : BaseActivity() {

	private val bind by bind(ActivityAgoraPublisherBinding::inflate)

	private val agoraToken =
		"007eJxTYDBf3yluPU/LbtPkOm+/jYqNrd/uPFbOSWZ8HnF4a0PP9YcKDGaJBolJ5uapqYamlibGKWmWJqZGJkYGhilmyZbGFubmX8P+ZzQEMjI0yKuyMjJAIIjPylCUn59ryMAAAG4gH7E="
	private val channelName = "room1"
	private val myAppId = "6a0ab77ee15943df94524201d6c93877"

	private var manager: AgoraManager? = null
	private var liveShowData: LiveShowModel? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		runSafe {
			liveShowData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getSerializableExtra("showData", LiveShowModel::class.java) as LiveShowModel
			} else {
				intent.getSerializableExtra("showData") as LiveShowModel
			}
		}

		manager = AgoraManager(this, myAppId, agoraToken, channelName)

		immersionBar {
			transparentBar()
			supportActionBar(false)
			keyboardEnable(true)
		}

		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.profileLayout.setMargins(
				top = system.top,
				left = resources.dpToPx(16),
				right = resources.dpToPx(16)
			)
			bind.startBtn.setMargins(
				resources.dpToPx(16),
				resources.dpToPx(0),
				resources.dpToPx(16),
				system.bottom
			)
			insets
		}

		requestPerms(Const.PERMISSIONS) {
			if (it) {
				manager?.initializeAgoraSDK()
				manager?.setupPublisherView(bind.publisherView)
			} else {
				errorToast("Permissions not granted!")
			}
		}

		bind.startBtn.setHapticClickListener { manager?.joinChannel() }

		bind.cameraSwitch.setHapticClickListener { manager?.switchCamera {

		} }
	}

	override fun onDestroy() {
		manager?.destroyEngine()
		super.onDestroy()
	}

}