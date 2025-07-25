package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPreferencesBinding
import io.bidswipe.app.network.Resource
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import java.util.Locale


class PreferencesFragment : BaseFragment<MoreViewModel, FragmentPreferencesBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentPreferencesBinding.inflate(inflater, view, false)

    private var countries = Locale.getISOCountries()

    private var countryList = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            callAPI()
        }

        for (countryCode in countries) {
            val locale = Locale("", countryCode)
            val countryName = locale.getDisplayCountry()
            if (!TextUtils.isEmpty(countryName)) {
                countryList.add(countryName)
            }
        }

        bind.country.setAdapter(
            ArrayAdapter(
                mCtx,
                android.R.layout.simple_list_item_1,
                countryList
            )
        )
        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.country.setDropDownBackgroundDrawable(draw)

        bind.country.setOnClickListener {
            bind.country.showDropDown()
        }

        log(countryList.toString())

        bind.loader.isVisible = true
        viewModel.settingsList()
        viewModel.settingsListRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.settingsListRepo.value = null

                    bind.country.setText(it.value.data?.countryOfResidence ?: "", false)
                    bind.directMessages.isChecked = it.value.data?.directMessage == true
                    bind.receiveGifts.isChecked = it.value.data?.receiveGifts == true
                    bind.privateEntry.isChecked = it.value.data?.enablePrivateEntry == true

                    bind.rewardStatus.isChecked = it.value.data?.showRewardStatus == true
                    bind.sellerTools.isChecked = it.value.data?.showSellerTools == true

                    bind.enableClips.isChecked = it.value.data?.enableClips == true
                    bind.savePastShows.isChecked = it.value.data?.savePastShows == true

                    bind.activityStatus.isChecked = it.value.data?.activityStatus == true
                    bind.syncPhoneContact.isChecked = it.value.data?.syncPhoneContacts == true
                    bind.suggestAccount.isChecked = it.value.data?.suggestMyAccount == true
                    bind.hapticFeedback.isChecked = it.value.data?.hapticFeedback == true

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
            if (bind.syncPhoneContact.isChecked) "1".request() else "0".request(),
            if (bind.suggestAccount.isChecked) "1".request() else "0".request(),
            if (bind.hapticFeedback.isChecked) "1".request() else "0".request(),
        )
    }
}