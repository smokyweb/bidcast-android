package io.bidswipe.app.ui.dashboard.more

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FeaturedAdapter
import io.bidswipe.app.controller.TeamAdapter
import io.bidswipe.app.databinding.FragmentAboutUsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class AboutUsFragment : BaseFragment<MoreViewModel, FragmentAboutUsBinding>() {
	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentAboutUsBinding.inflate(inflater, view, false)

	private lateinit var featureAdapter: FeaturedAdapter

	private lateinit var teamAdapter: TeamAdapter

	private var featureList = mutableListOf<AboutUsResponse.Data.Feature?>()

	private var teamList = mutableListOf<AboutUsResponse.Data.Team?>()
	private var socialMediaLinks = mutableListOf<AboutUsResponse.Data.SocialMedia?>()

	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {

			finish()

		}

		featureAdapter = FeaturedAdapter(featureList, mClick)
		bind.gridRecycler.adapter = featureAdapter

		teamAdapter = TeamAdapter(teamList, mClick)
		bind.teamRecycler.adapter = teamAdapter

		bind.twitter.setHapticClickListener {
			log("MediaLink  = ${socialMediaLinks.find { it?.platform == 3 }?.url}")

			val url = socialMediaLinks.find { it?.platform == 3 }?.url
			launchWeb(url?.url.toString())
		}

		bind.insta.setHapticClickListener {
			val url = socialMediaLinks.find { it?.platform == 1 }?.url
			launchWeb(url?.url.toString())
		}

		bind.faceBook.setHapticClickListener {
			val url = socialMediaLinks.find { it?.platform == 2 }?.url
			launchWeb(url?.url.toString())
		}

		bind.linkedIn.setHapticClickListener {
			val url = socialMediaLinks.find { it?.platform == 0 }?.url
			launchWeb(url?.url.toString())
		}

		bind.loader.isVisible = true

		viewModel.aboutUs()

		viewModel.aboutUsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.getTermsConditionsRepo.value = null

					val mData = it.value.data
					bind.mission.text = mData?.mission

					mData?.impact?.forEach {
						when (it?.label) {
							"Users" -> {

								bind.users.text = it.value

							}

							"Auction" -> {
								bind.auctions.text = it.value
							}

							"Sale" -> {
								bind.sales.text = it.value
							}
						}
					}

					if (mData?.socialMedia != null) {
						socialMediaLinks.addAll(mData.socialMedia)
					}

					bind.email.title.text = mData?.contactEmail
					bind.email.icon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_mail))
					bind.phoneNumber.title.text = mData?.contactPhone
					bind.phoneNumber.icon.setImageDrawable(
						ContextCompat.getDrawable(
							mCtx,
							draw.ic_phone
						)
					)

					bind.email.subTitle.isVisible = false
					bind.phoneNumber.subTitle.isVisible = false

					if (mData?.team != null) {
						teamList.addAll(mData.team)
					}

					if (mData?.features != null) {
						featureList.addAll(mData.features)
					}
					featureAdapter.notifyDataSetChanged()
					teamAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getTermsConditionsRepo.value = null
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

	private fun launchWeb(url: String) {
		log(url)

		val builder = CustomTabsIntent.Builder()

		val params = CustomTabColorSchemeParams.Builder()

		params.setToolbarColor(ContextCompat.getColor(mCtx, R.color.primary))

		builder.setDefaultColorSchemeParams(params.build())

		builder.setShowTitle(true)

		builder.setShareState(CustomTabsIntent.SHARE_STATE_ON)

		builder.setInstantAppsEnabled(true)

		val customBuilder = builder.build()

		customBuilder.intent.setPackage("com.android.chrome")

		customBuilder.launchUrl(requireActivity(), Uri.parse(url))

	}

}