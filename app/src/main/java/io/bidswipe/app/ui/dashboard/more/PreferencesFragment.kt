package io.bidswipe.app.ui.dashboard.more

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
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.CountryModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import java.util.Locale

class PreferencesFragment : BaseFragment<MoreViewModel, FragmentPreferencesBinding>() {

    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentPreferencesBinding.inflate(inflater, view, false)

	private var countries = Locale.getISOCountries()

    private var countryList = mutableListOf<CountryModel?>()

    private lateinit var countryAdapter: CountrySelectorAdapter
    private var countryPickerDialog: BottomSheetDialog? = null

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            countryList.forEachIndexed { index, countryModel ->
                countryModel?.selected = index == pos
            }

            bind.country.setText(countryList[pos]?.countryName, false)

            countryPickerDialog?.dismiss()

            countryAdapter.notifyDataSetChanged()
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

        bind.country.setHapticClickListener {
            countryPickerSheet()
//			bind.country.showDropDown()
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

                    countryList.find { it?.countryName == mData?.countryOfResidence }?.selected =
                        true

                    bind.country.setText(mData?.countryOfResidence ?: "", false)
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
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
                        it.parse(mCtx, TAG)
					}
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
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
                        it.parse(mCtx, TAG)
					}
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
            bind.country.text.toString().request(),
            if (bind.directMessages.isChecked) "1".request() else "0".request(),
            if (bind.receiveGifts.isChecked) "1".request() else "0".request(),
            if (bind.privateEntry.isChecked) "1".request() else "0".request(),
            if (bind.rewardStatus.isChecked) "1".request() else "0".request(),
            if (bind.sellerTools.isChecked) "1".request() else "0".request(),
            if (bind.enableClips.isChecked) "1".request() else "0".request(),
            if (bind.savePastShows.isChecked) "1".request() else "0".request(),
            if (bind.activityStatus.isChecked) "1".request() else "0".request(),
            "0".request(),
            "0".request(),
			if (bind.hapticFeedback.isChecked) "1".request() else "0".request()
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

        countryBind.recycler.adapter = countryAdapter

        countryBind.close.setHapticClickListener {
            countryPickerDialog?.dismiss()
        }

        countryPickerDialog?.show()
    }


}