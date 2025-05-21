package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentReferFriendBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids

class ReferFriendFragment : BaseFragment<DashViewModel,FragmentReferFriendBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentReferFriendBinding.inflate(inflater,view,false)

    val tipsList = mutableListOf<SellModel>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        tipsList.clear()

        tipsList.addAll(
            listOf(
                SellModel(R.drawable.ic_box, R.color.secondaryContainer, "Add Products Early", "Adding products before the stream helps you organize better and gives viewers time to preview items."),
                SellModel(R.drawable.ic_image, R.color.tertiaryContainer, "Quality Photos Matter", "Upload clear, high-quality photos showing different angles of your products to build trust."),
                SellModel(R.drawable.ic_tag, R.color.successContainer, "Set Clear Pricing", "Define your starting prices and reserve prices to help buyers make informed decisions."),
            )
        )

        bind.recycler.adapter = SellAdapter(mList = tipsList, "getStarted",object:
            RecyclerClicks {
         
            override fun itemClick(pos: Int, status: String?) {

            }

        })

        bind.continueBtn.setOnClickListener {

            findNavController().navigate(ids.goToCompleteYourProfileFragment)

        }

    }

}