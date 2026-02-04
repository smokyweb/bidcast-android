package io.bidswipe.app.ui.sell

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateSurpriseSetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.request.StoreSurpriseSet
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value


class CreateSurpriseSetFragment : BaseFragment<DashViewModel, FragmentCreateSurpriseSetBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentCreateSurpriseSetBinding.inflate(inflater, view, false)

    private var profileId = ""
    private var profiles = mutableListOf<GetShippingProfilesResponse.Data?>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick { finish() }

        bind.manageProducts.setHapticClickListener {
            findNavController().animatedNav(ids.toManageSurpriseProductsFragment)
        }

        bind.surpriseType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.surprise_set_type = if(tab?.position==0) "buy_it_now" else "auction"
                showHideLayouts()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                viewModel.surprise_set_type = if(tab?.position==0) "buy_it_now" else "auction"
                showHideLayouts()
            }
        })

        bind.surpriseType.selectTab(bind.surpriseType.getTabAt(1))

        bind.create.setOnClickListener {
            when {
                bind.name.value().isEmpty() -> {
                    bind.name.requestFocus()
                    Alerts.error(mCtx, "Please enter surprise set name")
                }

                bind.description.value().isEmpty() -> {
                    bind.description.requestFocus()
                    Alerts.error(mCtx, "Please enter surprise set description")
                }

                viewModel.surpriseSetList.isEmpty() -> {
                    Alerts.error(mCtx, "Please add at least one product")
                }

                profileId.isEmpty() -> {
                    Alerts.error(mCtx, "Please select shipping profile")
                }

                else -> {
                    bind.loader.isVisible = true
                    viewModel.storeSurpriseProduct(
                        StoreSurpriseSet(
                            viewModel.surprise_set_type,
                            bind.name.value(),
                            bind.description.value(),
                            viewModel.surpriseBuyPrice.toDoubleOrNull(),
                            profileId.toInt(),
                            viewModel.surpriseSetList,
                            if (bind.autoRandomize.isChecked) 1 else 0,
                            if (bind.quickSpin.isChecked) 1 else 0,
                        )
                    )
                }

            }
        }

        bind.loader.isVisible = true
        viewModel.getShippingProfile()
        viewModel.getShippingProfileRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getShippingProfileRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (mData?.isNotEmpty() == true) {
                        profiles.clear()
                        profiles.addAll(mData)
                    }

                    val profileAdapter = ArrayAdapter(
                        mCtx,
                        android.R.layout.simple_list_item_1,
                        profiles.map { it?.name })

                    bind.shippingProfile.setAdapter(profileAdapter)

                    val adapterBg = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                    bind.shippingProfile.setDropDownBackgroundDrawable(adapterBg)

                    bind.shippingProfile.setOnItemClickListener { _, _, position, _ ->
                        profileId = profiles[position]?.id.toString()
                        bind.shippingProfile.setText(profiles[position]?.name, false)
                    }

                    if (viewModel.shippingProfile.isNotEmpty()) {

                        profileId = viewModel.shippingProfile

                        val selectedShippingProfile = profiles.findLast { profile ->
                            viewModel.shippingProfile == profile?.id.toString()
                        }

                        bind.shippingProfile.setText(selectedShippingProfile?.name, false)
                    }

                    bind.shippingProfile.setHapticClickListener {
                        bind.shippingProfile.showDropDown()

                    }

                }

                is Resource.Error -> {
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

        viewModel.storeSurpriseProductRepo.observe(viewLifecycleOwner) {
            bind.loader.isVisible = false

            when (it) {
                is Resource.Success -> {
                    Alerts.showBottomSheet(
                        mCtx,
                        it.value.message ?: "Surprise Set added successfully",
                        "Success",
                        false,
                        object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                activity?.setResult(RESULT_OK)
                                finish()
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }
                        })
                }

                is Resource.Error -> {
                    viewModel.storeSurpriseProductRepo.value = null
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

    fun showHideLayouts(){
        bind.quickSpinLayout.isVisible=viewModel.surprise_set_type!="buy_it_now"
        bind.autoRandomizeLayout.isVisible=viewModel.surprise_set_type!="buy_it_now"
    }

}