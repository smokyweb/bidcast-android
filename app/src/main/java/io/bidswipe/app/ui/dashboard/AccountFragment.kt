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
import io.bidswipe.app.network.response.SellerHubResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.more.TrustedBuyerActivity
import io.bidswipe.app.ui.scheduleShow.ShowDetailsActivity
import io.bidswipe.app.ui.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toAuth
import io.bidswipe.app.utils.toListProduct
import io.bidswipe.app.utils.toScheduleShow

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentAccountBinding.inflate(inflater, view, false)

	private var moreList = mutableListOf<MoreModel>()

	private var accountGridList = mutableListOf<MoreModel>()

	private lateinit var moreAdapter: MoreAdapter
	private lateinit var gridAdapter: GridAdapter
	private lateinit var accountGridAdapter: GridAdapter
	private var upcomingShow: SellerHubResponse.Data.UpcomingShow? = null

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

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onMorePrimaryClick {
			log("TOUVHCHCCH ")
			viewModel.isDrawerOpened.value = viewModel.isDrawerOpened.value == false
		}

		App.getProfile()

		bind.swipeRefreshLayout.setOnRefreshListener {
			App.getProfile()
			viewModel.getSellerHubInfo()
		}

		App.profileResponse.observe(viewLifecycleOwner) {

			bind.userName.text = it?.name?.asCapital() ?: ""

			bind.sellerSince.isVisible = it?.username.isNullOrEmpty() == false

			bind.sellerSince.text = it?.username ?: "N/A"
			bind.userProfile.loadUrl(mCtx, it?.profileImage.toString())
		}

		bind.tabs.addOnTabSelectedListener(onTabSelectedListener)

		moreList.clear()
		moreList.add(MoreModel(R.drawable.ic_about_us, "About Us", "aboutUs"))
		moreList.add(MoreModel(R.drawable.ic_outlined_message, "Contact Us", "contactUs"))
		moreList.add(MoreModel(R.drawable.ic_document, "Sales Tax Exemption", "salesTax"))
		moreList.add(MoreModel(R.drawable.ic_document, "Terms & Conditions", "terms-condition"))
		moreList.add(MoreModel(R.drawable.ic_privacy, "Privacy Policy", "privacy-policy"))
		moreList.add(MoreModel(R.drawable.ic_faq, "F.A.Q", "faq"))
		moreList.add(MoreModel(R.drawable.ic_people, "Blocked Users", "blockedUsers"))
		moreList.add(MoreModel(R.drawable.ic_logout_outline, "Logout", "logout"))

		moreAdapter = MoreAdapter(moreList, mClicks)
		bind.accountView.moreRecycler.adapter = moreAdapter

		accountGridList.clear()
		accountGridList.add(MoreModel(R.drawable.ic_box, "Payment & Shipping", "paymentShipping"))
		accountGridList.add(MoreModel(R.drawable.ic_location, "Addresses", "address"))
		accountGridList.add(MoreModel(R.drawable.ic_identity_verification, "Trusted Buyer", "buyer"))
		accountGridList.add(MoreModel(R.drawable.notification, "Notifications", "notification"))
		accountGridList.add(MoreModel(R.drawable.ic_tag_outline, "Preferences", "preferences"))
		accountGridList.add(MoreModel(R.drawable.ic_heart, "Interests", "interests"))

		accountGridAdapter = GridAdapter(accountGridList, accountGridClick)
		bind.accountView.gridRecycler.adapter = accountGridAdapter

		bind.sellerHub.payoutCard.setHapticClickListener {
			startActivity(
				Intent(mCtx, SellerHubActivity::class.java).putExtra(
					"slug",
					"wallet"
				)
			)
		}

		bind.sellerHub.totalOrderCard.setHapticClickListener {
			startActivity(
				Intent(mCtx, SellerHubActivity::class.java).putExtra(
					"slug",
					"order"
				)
			)
		}

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

		bind.sellerHub.viewAll.setHapticClickListener {

			startActivity(
				Intent(mCtx, SellerHubActivity::class.java).putExtra(
					"slug",
					"shows"
				)
			)

		}

		bind.sellerHub.createProduct.setHapticClickListener {
			startActivity(mCtx.toListProduct())
		}

		bind.sellerHub.createShow.setHapticClickListener {
			startActivity(mCtx.toScheduleShow(from = "dash"))
		}

		bind.sellerHub.upcomingShow.setHapticClickListener {
			upcomingShow
			startActivity(Intent(mCtx, ShowDetailsActivity::class.java).putExtra("showId", upcomingShow?.id.toString()))
		}

		bind.sellerHub.vacationMode.setOnCheckedChangeListener { _, status ->
			viewModel.updateVacationModeStatus(status.toString().request())
		}

		bind.loader.isVisible = true
		viewModel.getSellerHubInfo()
		viewModel.getSellerHubInfoRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false
					val mData = it.value.data

					//UPCOMING SHOW
					if (mData?.upcomingShow != null) {

						upcomingShow = mData.upcomingShow

						bind.sellerHub.noShows.isVisible = false
						bind.sellerHub.upcomingShow.isVisible = true

						val item = mData.upcomingShow
						bind.sellerHub.name.text = item.title?.asCapital() ?: "N/A"
						bind.sellerHub.category.text = item.category?.name?.asCapital() ?: "N/A"
						bind.sellerHub.time.text = buildString {
							append(
								Utils.getFormattedDateTime(
									"yyyy-mm-dd",
									"mm-dd-yyyy",
									item.date ?: ""
								)
							)
							append(" ")
							append(Const.BULLET)
							append(" ")
							append(
								Utils.getFormattedDateTime(
									"HH:mm:ss",
									"hh:mm a",
									item.time ?: ""
								)
							)
						}

//                        bind.sellerHub.sales.text = buildString {
//                            append((item?.totalSalesAmount ?: 0).toString().asMoney())
//                            append(" sales ")
//                            append(Const.BULLET + " ")
//                            append(item?.totalOrders ?: 0)
//                            append(" orders")
//                        }

						bind.sellerHub.image.loadUrl(mCtx, item.imgThumbnail?.first() ?: "")

					}

					//ACCOUNT HEALTH
					bind.sellerHub.onTimePercent.text = mData?.accountHealth?.onTimeScanRate ?: "N/A"
					bind.sellerHub.defectFreeOrderRate.text = mData?.accountHealth?.defectFreeOrderRate ?: "N/A"
					bind.sellerHub.policyStanding.text = mData?.accountHealth?.policyStanding ?: "N/A"

					val total = (mData?.totalOrders ?: 0)
					bind.sellerHub.totalOrders.text = buildString {
						append(total.toString())
						if (total < 1) append(" Item") else append(" Items")
					}
					bind.sellerHub.payoutAmount.text = (mData?.payouts ?: 0.0).toString().asMoney()

					bind.sellerHub.vacationMode.isChecked = mData?.vacationMode ?: false

					bind.sellerHub.revenue.text = (mData?.revenue ?: 0).toString().asMoney()
					bind.sellerHub.itemCount.text = (mData?.items ?: 0).toString()
					bind.sellerHub.rating.text = (mData?.rating ?: 0.0).toString()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getSellerHubInfoRepo.value = null
					bind.swipeRefreshLayout.isRefreshing = false

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
			R.drawable.ic_logout_outline,
			"Logout",
			"Are you sure you want to logout?",
			primaryBtnText = "Logout",
			secondaryBtnText = "Cancel",
			canCancel = true,
			iconPadding = 36,
			showSecondary = true,
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