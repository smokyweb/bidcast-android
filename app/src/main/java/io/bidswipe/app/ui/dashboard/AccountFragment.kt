package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
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
import io.bidswipe.app.ui.dashboard.more.MoreActivity
import io.bidswipe.app.ui.dashboard.sellerHub.SellerHubActivity
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.toAuth

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentAccountBinding.inflate(inflater , view , false)

    private var moreList = mutableListOf<MoreModel>()
    private var gridList = mutableListOf<MoreModel>()

    private lateinit var moreAdapter: MoreAdapter
    private lateinit var gridAdapter: GridAdapter

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }

        override fun onTabUnselected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }

        override fun onTabReselected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }
    }

    private val mClicks = object : RecyclerClicks {
        override fun viewClick(pos: Int) {
            when(moreList[pos].slug){

                "aboutUs" ->{

                }

                "language" -> {

//                    startActivity(Intent(mCtx , LanguageActivity::class.java))
                }

                "contactUs" ->{

                }

                "salesTax" ->{

                }

                "termsCondition" ->{
                    startActivity(Intent(mCtx , MoreActivity::class.java).putExtra("to",moreList[pos].slug))
                }

                "privacyPolicy" ->{
                    startActivity(Intent(mCtx , MoreActivity::class.java).putExtra("to",moreList[pos].slug))
                }

                "faq" ->{

                }

                "logout" -> {
                    logoutDialog()
                }

            }
        }

        override fun itemClick(pos: Int, status: String) {


        }

    }

    private val gridClick = object : RecyclerClicks {
        override fun viewClick(pos: Int) {
            when(gridList[pos].slug){

                "inventory" ->{
                    startActivity(Intent(mCtx , SellerHubActivity::class.java))
                }

            }
        }

        override fun itemClick(pos: Int, status: String) {


        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.tabs.addOnTabSelectedListener(onTabSelectedListener)

        moreList.add(MoreModel(R.drawable.ic_vacation,"About Us","aboutUs"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Contact Us", "contactUs"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Change Language", "language"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Sales Tax Exemption", "salesTax"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Terms & Conditions", "termsCondition"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Privacy Policy","privacyPolicy"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"F.A.Q", "faq"))
        moreList.add(MoreModel(R.drawable.ic_vacation,"Logout","logout"))

        moreAdapter = MoreAdapter(moreList, mClicks)

        bind.accountView.moreRecycler.adapter = moreAdapter


        gridList.add(MoreModel(R.drawable.ic_box,"Inventory","inventory"))
        gridList.add(MoreModel(R.drawable.ic_mic,"Shows", "Shows"))
        gridList.add(MoreModel(R.drawable.ic_order,"My Order", "order"))
        gridList.add(MoreModel(R.drawable.ic_walllet,"Wallet", "wallet"))
        gridList.add(MoreModel(R.drawable.ic_tag,"Offers", "offers"))
        gridList.add(MoreModel(R.drawable.ic_tag,"Tips","tips"))
        gridList.add(MoreModel(R.drawable.ic_vehicle,"Shipping", "shipping"))
        gridList.add(MoreModel(R.drawable.ic_people,"Affiliate Program","program"))
        gridList.add(MoreModel(R.drawable.ic_vacation,"Seller Training","training"))
        gridList.add(MoreModel(R.drawable.ic_vacation,"Premier Shop","shop"))
        gridList.add(MoreModel(R.drawable.ic_vacation,"Seller Status","sellerStatus"))
        gridList.add(MoreModel(R.drawable.ic_vacation,"Seller Analytics","sellerAnalytics"))

        gridAdapter= GridAdapter(gridList,gridClick)
        bind.sellerHub.gridRecycler.adapter = gridAdapter

        viewModel.logoutRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    successToast(it.value.message.toString())
                    Prefs(mCtx).clear()
                    startActivity(mCtx.toAuth())
                    finish()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.logoutRepo.value = null
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }
                        })
                    }
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