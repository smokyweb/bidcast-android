package io.bidswipe.app.ui.dashboard.tutorials

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTutorialsBinding
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.bind
import kotlin.getValue

class TutorialsActivity : BaseActivity() {

    private val bind by bind(ActivityTutorialsBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    var scheduleShowLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                if (result.data != null){
                    viewModel.currentStep = 2

                    log(
                        "DATA : ${result.data?.getParcelableExtra<TutorialShowModel>("title" ) }"
                    )

                    viewModel.showData.value = result.data?.getParcelableExtra("title")

                }

            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

    }
}