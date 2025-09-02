package io.bidswipe.app.ui.dashboard.tutorials

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTutorialsBinding
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.setNewStart
import kotlin.getValue

class TutorialsActivity : BaseActivity() {

    private val bind by bind(ActivityTutorialsBinding::inflate)
    private val viewModel by viewModels<DashViewModel>()

    private var type = ""
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

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


        type = intent.getStringExtra("type").toString()

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        val id = when (type.trim()) {
            "promoteTools" -> R.id.howToSellFragment
            else -> {
                R.id.getStartedFragment
            }
        }
        navController.setNewStart(id, R.navigation.tutorials_nav_graph)

    }
}