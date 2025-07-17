package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityMoreBinding
import io.bidswipe.app.utils.bind

class MoreActivity : BaseActivity() {

    private val bind by bind(ActivityMoreBinding::inflate)

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.findNavController()
        val navGraph=navController.navInflater.inflate(R.navigation.more_graph)

        val slug = intent.getStringExtra("slug").toString()

        when (slug) {

            "aboutUs" ->{
                navGraph.setStartDestination(R.id.aboutUsFragment)
            }

            "language" -> {

            }

            "contactUs" ->{
                navGraph.setStartDestination(R.id.contactUsFragment)
            }

            "salesTax" ->{
                navGraph.setStartDestination(R.id.salesTaxExemptionFragment)
            }

            "termsCondition" ->{
                navGraph.setStartDestination(R.id.termsConditionFragment)
            }

            "privacyPolicy" ->{
                navGraph.setStartDestination(R.id.privacyPolicyFragment)
            }

            "faq" ->{
                navGraph.setStartDestination(R.id.FAQFragment)
            }

            "paymentShipping"->{
                navGraph.setStartDestination(R.id.paymentShippingFragment)
            }

            "address"->{
                navGraph.setStartDestination(R.id.addressesFragment)
            }

            "buyer"->{
                navGraph.setStartDestination(R.id.trustedBuyerFragment)
            }

            "notification"->{

            }
            "preferences"->{
                navGraph.setStartDestination(R.id.preferencesFragment)
            }
            "addAddress"->{
                navGraph.setStartDestination(R.id.addShippingAddressFragment)
            }

        }

        navController.graph=navGraph


    }
}