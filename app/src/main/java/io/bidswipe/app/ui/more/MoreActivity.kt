package io.bidswipe.app.ui.more

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
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
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0, system.top, 0, system.bottom)
			CONSUMED
		}
		navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
		navController = navHostFragment.findNavController()

		val navGraph = navController.navInflater.inflate(R.navigation.more_graph)
		
		val slug = intent.getStringExtra("slug").toString()
		
		when (slug) {
			
			"language" -> {

			}

			"coupons" -> {
				navGraph.setStartDestination(R.id.couponsFragment)
			}
			
			"contactUs" -> {
				navGraph.setStartDestination(R.id.contactUsFragment)
			}
			
			"blockedUsers" -> {
				navGraph.setStartDestination(R.id.blockedUsersFragment)
			}
			
			"salesTax" -> {
				navGraph.setStartDestination(R.id.salesTaxExemptionFragment)
			}
			
			"paymentShipping" -> {
				navGraph.setStartDestination(R.id.paymentShippingFragment)
			}
			
			"address" -> {
				navGraph.setStartDestination(R.id.addressesFragment)
			}
			
			"notification" -> {
			}
			
			"preferences" -> {
				navGraph.setStartDestination(R.id.preferencesFragment)
			}
			
			"addAddress" -> {
				navGraph.setStartDestination(R.id.addShippingAddressFragment)
			}
			
			else -> {
				navGraph.setStartDestination(R.id.contentFragment)
			}
		}
		
		navController.graph = navGraph
		
		
	}
}