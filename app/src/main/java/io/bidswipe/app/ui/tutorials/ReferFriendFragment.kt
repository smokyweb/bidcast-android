package io.bidswipe.app.ui.tutorials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentReferFriendBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.setHapticClickListener

class ReferFriendFragment : BaseFragment<DashViewModel, FragmentReferFriendBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentReferFriendBinding.inflate(inflater, view, false)

	val tipsList = mutableListOf<SellModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val type = requireActivity().intent.getStringExtra("type")

		bind.continueBtn.isVisible = type != "refer"

		bind.header.onBackClick {
			if ( type == "refer") finish() else findNavController().popBackStack()
		}

		tipsList.clear()
		tipsList.addAll(
			listOf(
				SellModel(
					R.drawable.ic_box,
					R.color.secondaryContainer,
					"Add Products Early",
					"Adding products before the stream helps you organize better and gives viewers time to preview items."
				),
				SellModel(
					R.drawable.ic_gallery,
					R.color.tertiaryContainer,
					"Quality Photos Matter",
					"Upload clear, high-quality photos showing different angles of your products to build trust."
				),
				SellModel(
					R.drawable.ic_tag_outline,
					R.color.successContainer,
					"Set Clear Pricing",
					"Define your starting prices and reserve prices to help buyers make informed decisions."
				),
			)
		)

		bind.recycler.adapter = SellAdapter(mList = tipsList, "getStarted", object :
			RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}

		})

		bind.referralCode.setText(buildString {
			append(Const.BASE_URL)
			append("/referral-code?referralCode=${App.profileResponse.value?.referralCode ?:""}")
		})

		bind.copyBtn.setHapticClickListener {
			val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			val clip = ClipData.newPlainText("label", bind.referralCode.text)
			clipboard.setPrimaryClip(clip)
		}

		bind.continueBtn.setHapticClickListener {

			findNavController().navigate(ids.goToCompleteYourProfileFragment)

		}

	}

}