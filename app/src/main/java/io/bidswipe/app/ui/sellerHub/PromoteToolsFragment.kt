package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PromoteFeatureAdapter
import io.bidswipe.app.controller.PromoteMetricAdapter
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentPromoteToolsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PromoteMetricModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPromoteToolsDetailsResponse
import io.bidswipe.app.network.response.GetPromoteToolsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class PromoteToolsFragment : BaseFragment<SellerHubViewModel, FragmentPromoteToolsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentPromoteToolsBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

	    val adapter = ViewPagerAdapter(requireActivity(), "promoteTools")
	    bind.pager.adapter = adapter

	    bind.pager.isUserInputEnabled = false

	    TabLayoutMediator(bind.tabLayout, bind.pager) { tab, position ->
		    tab.text = when (position) {
			    0 -> "Overview"
			    1 -> "Promoted Shows"
			    else -> ""
		    }
	    }.attach()

    }

}