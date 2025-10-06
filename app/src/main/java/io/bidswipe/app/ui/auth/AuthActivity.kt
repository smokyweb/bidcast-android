package io.bidswipe.app.ui.auth

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAuthBinding
import io.bidswipe.app.utils.bind

class AuthActivity : BaseActivity() {

	private val bind by bind(ActivityAuthBinding::inflate)

	private var referralCode = ""

	private lateinit var navHostFragment : NavHostFragment
	private lateinit var navController : NavController

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

			val data: Uri? = intent.data
			data?.let { uri ->
				referralCode = uri.getQueryParameter("referralCode").toString()
				// Use the param or the path to navigate or update UI
				log(" referralCode : $referralCode")
			}


		navHostFragment =
			supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
		navController = navHostFragment.findNavController()
		val navGraph = navController.navInflater.inflate(R.navigation.auth_nav_graph)

		navController.graph = navGraph

		if (referralCode.isNotEmpty()){
			val bundle = Bundle()
			bundle.putString("referralCode", referralCode)
			navController.navigate(R.id.createAccountFragment, bundle)
		}


	}

}