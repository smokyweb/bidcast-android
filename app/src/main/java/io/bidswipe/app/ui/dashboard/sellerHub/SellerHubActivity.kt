package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivitySellerHubBinding
import io.bidswipe.app.utils.bind

class SellerHubActivity : BaseActivity() {

	private val bind by bind(ActivitySellerHubBinding::inflate)

	private lateinit var navHostFragment: NavHostFragment
	private lateinit var navController: NavController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
		navController = navHostFragment.findNavController()
		val navGraph = navController.navInflater.inflate(R.navigation.seller_hub_nav_graph)

		val slug = intent.getStringExtra("slug").toString()

		when (slug) {
			"inventory" -> {
				navGraph.setStartDestination(R.id.inventoryFragment)
			}
			"order" -> {
				navGraph.setStartDestination(R.id.myOrdersFragment)
			}
			"offers" -> {
				navGraph.setStartDestination(R.id.sellerOffersFragment)
			}
			"wallet" -> {
				navGraph.setStartDestination(R.id.walletFragment)
			}
            "shows" -> {
                navGraph.setStartDestination(R.id.showsFragment)
            }
			 "shipping" -> {
                navGraph.setStartDestination(R.id.shippingFragment)
            }
			"tips" -> {
                navGraph.setStartDestination(R.id.tipsFragment)
            }
			"program" -> {
				navGraph.setStartDestination(R.id.affiliateProgramFragment)
			}
			"training" -> {
				navGraph.setStartDestination(R.id.howToSellFragment2)
			}
			"sellerStatus" -> {
				navGraph.setStartDestination(R.id.sellerStatusFragment)
			}

			"shop" -> {
				navGraph.setStartDestination(R.id.premierShopFragment)
			}
			"promote" -> {
				navGraph.setStartDestination(R.id.promoteToolsFragment)
			}
			"sellerAnalytics" -> {
				navGraph.setStartDestination(R.id.analyticsFragment)
			}
			else -> {
				navGraph.setStartDestination(R.id.inventoryFragment)
			}

		}

		navController.graph = navGraph

	}
}