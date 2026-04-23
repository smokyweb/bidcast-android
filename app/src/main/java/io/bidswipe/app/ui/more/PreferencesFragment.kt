package io.bidswipe.app.ui.more

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CountrySelectorAdapter
import io.bidswipe.app.databinding.CountryPickerSheetBinding
import io.bidswipe.app.databinding.FragmentPreferencesBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.CountryModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class PreferencesFragment : BaseFragment<MoreViewModel, FragmentPreferencesBinding>() {

	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPreferencesBinding.inflate(inflater, view, false)

	private var countries = Locale.getISOCountries()

	private var countryList = mutableListOf<CountryModel?>()
	private var languageList = mutableListOf<CountryModel?>()

	private lateinit var countryAdapter: CountrySelectorAdapter
	private lateinit var languageAdapter: CountrySelectorAdapter
	private var countryPickerDialog: BottomSheetDialog? = null
	private var languagePickerDialog: BottomSheetDialog? = null

	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			countryList.forEachIndexed { index, countryModel ->
				countryModel?.selected = index == pos
			}

			bind.country.text=(countryList[pos]?.countryName)

			countryPickerDialog?.dismiss()

			countryAdapter.notifyDataSetChanged()
		}
	}

	private var languageClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			languageList.forEachIndexed { index, lang ->
				lang?.selected = index == pos
			}

			val selectedTitle = languageList[pos]?.countryName ?: return
			bind.languageValue.text = selectedTitle

			languagePickerDialog?.dismiss()
			languageAdapter.notifyDataSetChanged()

			applySelectedLanguage(selectedTitle)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			callAPI()
		}

		for (countryCode in countries) {
			val locale = Locale("", countryCode)
			val countryName = locale.displayCountry
			if (!TextUtils.isEmpty(countryName)) {
				countryList.add(CountryModel(countryName))
			}
		}

		countryAdapter = CountrySelectorAdapter(countryList, mClick)
		languageList = Const.languages.map { CountryModel(countryName = it.title) }.toMutableList()
		languageAdapter = CountrySelectorAdapter(languageList, languageClick)
		setupLanguageSelection()

		bind.country.setHapticClickListener {
			countryPickerSheet()
//			bind.country.showDropDown()
		}
		bind.languageValue.setHapticClickListener {
			languagePickerSheet()
		}

		bind.privacy.setHapticClickListener {
			showPrivacyInfo()
		}

		log(countryList.toString())

		bind.loader.isVisible = true
		viewModel.settingsList()
		viewModel.settingsListRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.settingsListRepo.value = null

					val mData = it.value.data

					countryList.find { it?.countryName == mData?.countryOfResidence }?.selected = true

					bind.country.text=(mData?.countryOfResidence ?: "")
					bind.directMessages.isChecked = mData?.directMessage == true
					bind.receiveGifts.isChecked = mData?.receiveGifts == true
					bind.privateEntry.isChecked = mData?.enablePrivateEntry == true

					bind.rewardStatus.isChecked = mData?.showRewardStatus == true
					bind.sellerTools.isChecked = mData?.showSellerTools == true

					bind.enableClips.isChecked = mData?.enableClips == true
					bind.savePastShows.isChecked = mData?.savePastShows == true

					bind.activityStatus.isChecked = mData?.activityStatus == true
//					bind.syncPhoneContact.isChecked mData?a?.syncPhoneContacts == true
//					bind.suggestAccount.isChecked = mData?.suggestMyAccount == true
					bind.hapticFeedback.isChecked = mData?.hapticFeedback == true
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.settingsListRepo.value = null
					it.parse(mCtx, TAG)
				}

				else -> {}

			}
		}

		viewModel.settingsStoreRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.settingsStoreRepo.value = null
					finish()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.settingsStoreRepo.value = null
					it.parse(mCtx, TAG)
				}

				else -> {}

			}
		}

		onBackPressed {
			callAPI()
		}
	}

	private fun callAPI() {
		bind.loader.isVisible = true
		App.getProfile()
		viewModel.settingsStore(
			countryOfResidence = bind.country.text.toString().request(),
			directMessage = if (bind.directMessages.isChecked) "1".request() else "0".request(),
			receiveGifts = if (bind.receiveGifts.isChecked) "1".request() else "0".request(),
			enablePrivateEntry = if (bind.privateEntry.isChecked) "1".request() else "0".request(),
			showRewardStatus = if (bind.rewardStatus.isChecked) "1".request() else "0".request(),
			showSellerTools = if (bind.sellerTools.isChecked) "1".request() else "0".request(),
			enableClips = if (bind.enableClips.isChecked) "1".request() else "0".request(),
			savePastShows = if (bind.savePastShows.isChecked) "1".request() else "0".request(),
			activityStatus = if (bind.activityStatus.isChecked) "1".request() else "0".request(),
			syncPhoneContacts = "0".request(),
			suggestMyAccount = "0".request(),
			hapticFeedback = if (bind.hapticFeedback.isChecked) "1".request() else "0".request()
		)
	}

	fun countryPickerSheet() {

		val countryBind = CountryPickerSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.country_picker_sheet,
				null,
				false
			)
		)

		countryPickerDialog = Alerts.appBottomSheet(mCtx, true, countryBind)

		countryBind.title.text = getString(R.string.select_country)
		countryBind.recycler.adapter = countryAdapter

		countryBind.close.setHapticClickListener {
			countryPickerDialog?.dismiss()
		}

		countryPickerDialog?.show()
	}

	private fun languagePickerSheet() {
		val languageBind = CountryPickerSheetBinding.bind(
			layoutInflater.inflate(
				R.layout.country_picker_sheet,
				null,
				false
			)
		)

		languagePickerDialog = Alerts.appBottomSheet(mCtx, true, languageBind)
		languageBind.title.text = getString(R.string.select_language)
		languageBind.recycler.adapter = languageAdapter

		languageBind.close.setHapticClickListener {
			languagePickerDialog?.dismiss()
		}

		languagePickerDialog?.show()
	}

	private fun setupLanguageSelection() {
		val savedLanguageCode = Prefs(mCtx).localeLanguage().ifBlank { "en" }
		val selectedLang = Const.languages.find { it.locale.language == savedLanguageCode }
			?: Const.languages.find { it.locale.language == "en" }
			?: Const.languages.first()

		languageList.forEach { lang ->
			lang?.selected = lang?.countryName == selectedLang.title
		}
		bind.languageValue.text = selectedLang.title
	}

	private fun applySelectedLanguage(selectedTitle: String) {
		val selectedLang = Const.languages.find { it.title == selectedTitle }
			?: Const.languages.find { it.locale.language == "en" }
			?: return

		Prefs(mCtx).putString(Prefs.LANGUAGE, selectedLang.title)
		Prefs(mCtx).putString(Prefs.LOCALE_LANGUAGE, selectedLang.locale.language)

		(activity as? io.bidswipe.app.base.BaseActivity)?.updateLocale(selectedLang.locale)
		activity?.recreate()
	}

	private fun showPrivacyInfo() {
		AppBottomSheet(
			mCtx = mCtx,
			image = R.drawable.ic_question,
			title = "Privacy Settings",
			message = "Configure your privacy preferences to control your account visibility and interactions:\n" +
					"\n" +
					"• Direct Messages: Control who can send you direct messages\n" +
					"\n" +
					"• Receive Gifts: Allow others to send you gifts during live shows\n" +
					"\n" +
					"• Private Entry: Join live shows without appearing in the viewer list\n" +
					"\n" +
					"These settings help you maintain your desired level of privacy while using BidSwipe.",
			primaryBtnText = "Got It",
			secondaryBtnText = "Learn More",
			canCancel = true,
			showSecondary = false,
			iconPadding = 16,
			alertType = AlertType.INFO,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}

				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
			}
		).show()
	}

}