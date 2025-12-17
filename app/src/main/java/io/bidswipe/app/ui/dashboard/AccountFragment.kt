package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.GridAdapter
import io.bidswipe.app.controller.MoreAdapter
import io.bidswipe.app.databinding.FragmentAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.MoreModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.ui.sellerHub.SellerVerificationActivity
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toAuth

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentAccountBinding.inflate(inflater, view, false)

	private var moreList = mutableListOf<MoreModel>()
	private var gridList = mutableListOf<MoreModel>()

	private var accountGridList = mutableListOf<MoreModel>()

	private lateinit var moreAdapter: MoreAdapter
	private lateinit var gridAdapter: GridAdapter
	private lateinit var accountGridAdapter: GridAdapter
	private var kycUrl = ""

	private val onTabSelectedListener = object : OnTabSelectedListener {
		override fun onTabSelected(tab: TabLayout.Tab?) {
			tab?.let {
				bind.switcher.displayedChild = it.position
				it.view.findViewById<TextView>(android.R.id.text1)?.apply {
					setTypeface(typeface, Typeface.BOLD)
				}
			}
		}

		override fun onTabUnselected(tab: TabLayout.Tab?) {
			tab?.let {
				it.view.findViewById<TextView>(android.R.id.text1)?.apply {
					setTypeface(typeface, Typeface.NORMAL)
				}
			}
		}

		override fun onTabReselected(tab: TabLayout.Tab?) {
			tab?.let {
				bind.switcher.displayedChild = it.position
			}
		}
	}

	private val mClicks = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			when (moreList[pos].slug) {
				"logout" -> logoutDialog()
				"aboutUs" -> handlePageUrl(DashViewModel.SLUG_ABOUT_US)
				"privacyPolicy" -> handlePageUrl(DashViewModel.SLUG_PRIVACY_POLICY)
				"faq" -> handlePageUrl(DashViewModel.SLUG_FAQ)
				"termsCondition" -> handlePageUrl(DashViewModel.SLUG_TERMS)
				else -> {
					startActivity(
						Intent(mCtx, MoreActivity::class.java)
							.putExtra("slug", moreList[pos].slug)
							.putExtra("title", moreList[pos].title)
					)
				}

			}
		}

	}

	private fun handlePageUrl(slug: String) {
		val title = when (slug) {
			DashViewModel.SLUG_ABOUT_US -> "About Us"
			DashViewModel.SLUG_PRIVACY_POLICY -> "Privacy Policy"
			DashViewModel.SLUG_FAQ -> "FAQ"
			DashViewModel.SLUG_TERMS -> "Terms & Conditions"
			else -> "Content"
		}
		val intent = Intent(mCtx, MoreActivity::class.java).apply {
			putExtra("slug", slug)
			putExtra("title", title)
		}
		startActivity(intent)
	}

	private val accountGridClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			when (accountGridList[pos].slug) {

				"notification" -> {
					startActivity(
						Intent(mCtx, NotificationActivity::class.java).putExtra(
							"slug",
							accountGridList[pos].slug
						)
					)
				}

				"buyer" -> {
					startActivity(
						Intent(mCtx, TrustedBuyerActivity::class.java).putExtra(
							"slug",
							accountGridList[pos].slug
						)
					)
				}

				"interests" -> {
					startActivity(
						Intent(mCtx, ChooseInterestActivity::class.java).putExtra("fromAccount", true)
					)
				}

				else -> {
					startActivity(
						Intent(mCtx, MoreActivity::class.java).putExtra(
							"slug",
							accountGridList[pos].slug
						)
					)
				}
			}

		}

	}

	private val gridClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			when (gridList[pos].slug) {

				"sellerVerification" -> {
					startActivity(
						Intent(mCtx, SellerVerificationActivity::class.java).putExtra(
							"slug",
							gridList[pos].slug
						)
					)
				}

				else -> {
					startActivity(
						Intent(mCtx, SellerHubActivity::class.java).putExtra(
							"slug",
							gridList[pos].slug
						).putExtra("url", kycUrl)
					)

				}

			}

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onMorePrimaryClick {
			startActivity(Intent(mCtx, NotificationActivity::class.java).putExtra("slug", "notification"))
		}

		App.getProfile()

		App.profileResponse.observe(viewLifecycleOwner) {

			bind.userName.text = it?.name?.asCapital() ?: ""

			bind.sellerSince.isVisible = it?.username.isNullOrEmpty() == false

			bind.sellerSince.text = it?.username ?: "N/A"
			bind.userProfile.loadUrl(mCtx, it?.profileImage.toString())
		}

		bind.tabs.addOnTabSelectedListener(onTabSelectedListener)

		moreList.clear()
		moreList.add(MoreModel(R.drawable.ic_vacation, "About Us", "aboutUs"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Contact Us", "contactUs"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Sales Tax Exemption", "salesTax"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Terms & Conditions", "terms-condition"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Privacy Policy", "privacy-policy"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "F.A.Q", "faq"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Blocked Users", "blockedUsers"))
		moreList.add(MoreModel(R.drawable.ic_vacation, "Logout", "logout"))

		moreAdapter = MoreAdapter(moreList, mClicks)
		bind.accountView.moreRecycler.adapter = moreAdapter

		gridList.clear()
		gridList.add(MoreModel(R.drawable.ic_box, "Inventory", "inventory"))
		gridList.add(MoreModel(R.drawable.ic_mic, "Shows", "shows"))
		gridList.add(MoreModel(R.drawable.ic_order, "Orders", "order"))
		gridList.add(MoreModel(R.drawable.ic_wallet, "Wallet", "wallet"))
		gridList.add(MoreModel(R.drawable.ic_tag, "Offers", "offers"))
		gridList.add(MoreModel(R.drawable.ic_tag, "Tips", "tips"))
		gridList.add(MoreModel(R.drawable.ic_shipping, "Shipping", "shipping"))
		gridList.add(MoreModel(R.drawable.ic_people, "Affiliate Program", "program"))
		gridList.add(MoreModel(R.drawable.ic_training, "Seller Training", "training"))
		gridList.add(MoreModel(R.drawable.ic_shop, "Premier Shop", "shop"))
		gridList.add(MoreModel(R.drawable.ic_graph, "Seller Status", "sellerStatus"))
		gridList.add(MoreModel(R.drawable.ic_graph, "Seller Analytics", "sellerAnalytics"))
		gridList.add(MoreModel(R.drawable.ic_speaker, "Promote Tools", "promote"))
		gridList.add(MoreModel(R.drawable.ic_checked_tag, "Seller Verification", "sellerVerification"))
		gridList.add(MoreModel(R.drawable.ic_payment_verification, "Identity Verification", "identityVerification"))

		gridAdapter = GridAdapter(gridList, gridClick)
		bind.sellerHub.gridRecycler.adapter = gridAdapter

		accountGridList.clear()
		accountGridList.add(MoreModel(R.drawable.ic_box, "Payment & Shipping", "paymentShipping"))
		accountGridList.add(MoreModel(R.drawable.ic_mic, "Addresses", "address"))
		accountGridList.add(MoreModel(R.drawable.ic_order, "Trusted Buyer", "buyer"))
		accountGridList.add(MoreModel(R.drawable.notification, "Notifications", "notification"))
		accountGridList.add(MoreModel(R.drawable.ic_tag, "Preferences", "preferences"))
		accountGridList.add(MoreModel(R.drawable.explore, "Interests", "interests"))

		accountGridAdapter = GridAdapter(accountGridList, accountGridClick)
		bind.accountView.gridRecycler.adapter = accountGridAdapter

		bind.editIcon.setHapticClickListener {
			startActivity(Intent(mCtx, UpdateAccountActivity::class.java))
		}

		viewModel.logoutRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					successToast(it.value.message.toString())
					Prefs(mCtx).clear()
					startActivity(mCtx.toAuth())
					finish()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.logoutRepo.value = null

					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})

				}

				else -> {}

			}
		}

	}

	override fun onDestroyView() {
		bind.tabs.removeOnTabSelectedListener(onTabSelectedListener)
		super.onDestroyView()
	}

	private fun logoutDialog() {
		AppBottomSheet(
			mCtx,
			R.drawable.ic_logout,
			"Logout",
			"Are you sure you want to logout?",
			primaryBtnText = "Logout",
			secondaryBtnText = "Cancel",
			canCancel = true,
			showSecondary = true,
			iconPadding = 16,
			alertType = AlertType.WARNING,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
					bind.loader.isVisible = true
					viewModel.logout()
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}

		).show()

	}

}