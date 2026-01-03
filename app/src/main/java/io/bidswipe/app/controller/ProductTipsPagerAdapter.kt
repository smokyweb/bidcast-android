package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductTipItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel

class ProductTipsPagerAdapter(mList : MutableList<String> , val type : String?) :
	BaseAdapter<String , ProductTipItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ProductTipItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ProductTipItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

			val tipsList = mutableListOf<SellModel>()

			tipsList.clear()

			when (type) {
				"liveTips" -> {
					bind.exampleLayout.isVisible = false

					tipsList.addAll(
						listOf(
							SellModel(
								R.drawable.ic_video ,
								R.color.secondaryContainer ,
								"Private Rehearsal" ,
								"Practice in private mode to get comfortable with the platform and learn at your own pace."
							) ,
							SellModel(
								R.drawable.ic_mic ,
								R.color.tertiaryContainer ,
								"Test Sound & Audio" ,
								"Check your microphone and speakers to ensure clear communication with your audience."
							) ,
							SellModel(
								R.drawable.ic_hammer ,
								R.color.successContainer ,
								"Live Auction Practice" ,
								"Walk through an example auction scenario to familiarize yourself with the process."
							) ,
						)
					)

				}

				"showTips" -> {
					tipsList.addAll(
						listOf(
							SellModel(
								R.drawable.ic_pencil ,
								R.color.secondaryContainer ,
								"Write an Engaging Title" ,
								"Create a clear, descriptive title that captures attention. Keep it concise and relevant to your content."
							) ,
							SellModel(
								R.drawable.ic_calendar ,
								R.color.tertiaryContainer ,
								"Schedule in Advance" ,
								"Only sell authentic and legitimate productsPlan your shows ahead of time to maintain consistency and give your audience time to prepare."
							) ,
							SellModel(
								R.drawable.ic_gallery ,
								R.color.successContainer ,
								"Choose a Quality Thumbnail" ,
								"Select an eye-catching thumbnail that represents your content well. Use high-resolution images."
							) ,
						)
					)
				}

				"bringInBuyers" -> {
					bind.exampleLayout.isVisible = false
					tipsList.addAll(
						listOf(
							SellModel(
								R.drawable.ic_gift ,
								R.color.secondaryContainer ,
								"Referral Program" ,
								"Share your referral code with friends and earn rewards. Both you and your referred friends get special bonuses on their first purchase."
							) ,
							SellModel(
								R.drawable.ic_person_outlined ,
								R.color.tertiaryContainer ,
								"Complete Your Profile" ,
								"A complete profile builds trust with buyers. Add a professional photo, detailed bio, and showcase your expertise in your field."
							) ,
							SellModel(
								R.drawable.ic_share ,
								R.color.successContainer ,
								"Share Your Shows" ,
								"Promote your upcoming shows on social media. Use our easy sharing tools to post directly to Instagram, Facebook, and Twitter."
							) ,
						)
					)
				}

				else -> {
					tipsList.addAll(
						listOf(
							SellModel(
								R.drawable.ic_box ,
								R.color.secondaryContainer ,
								"Add Products Early" ,
								"Adding products before the stream helps you organize better and gives viewers time to preview items."
							) ,
							SellModel(
								R.drawable.ic_gallery ,
								R.color.tertiaryContainer ,
								"Quality Photos Matter" ,
								"Upload clear, high-quality photos showing different angles of your products to build trust."
							) ,
							SellModel(
								R.drawable.ic_tag_outline ,
								R.color.successContainer ,
								"Set Clear Pricing" ,
								"Define your starting prices and reserve prices to help buyers make informed decisions."
							) ,
						)
					)
				}
			}

			bind.recycler.adapter =
				SellAdapter(mList = tipsList , "getStarted" , object : RecyclerClicks {

					override fun itemClick(pos : Int , status : String?) {

					}
				})

		}
	}
}