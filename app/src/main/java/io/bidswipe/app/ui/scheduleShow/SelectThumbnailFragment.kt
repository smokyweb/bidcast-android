package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.GoodsExampleAdapter
import io.bidswipe.app.controller.ThumbnailTipsAdapter
import io.bidswipe.app.databinding.FragmentSelectThumbnailBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class SelectThumbnailFragment :
	BaseFragment<ScheduleShowViewModel, FragmentSelectThumbnailBinding>() {

	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSelectThumbnailBinding.inflate(inflater, view, false)

	private var tipsList = mutableListOf<GetAllTipsResponse.Data.Tip?>()

	private var goodsList = mutableListOf<String?>()

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent

			bind.imgCard.isVisible = true
			bind.pickImageLayout.isVisible = false

			bind.img.setImageURI(imageUri)

			val imagePath = result.getUriFilePath(mCtx, true)

			if (imagePath != null) {

				viewModel.thumbnail = imagePath

			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val from = activity?.intent?.getStringExtra("from").toString()

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

//        tipsList.clear()
		/* tipsList.addAll(
			 listOf(
				 SellModel(R.drawable.ic_bulb,R.color.secondaryContainer,"Good Lighting","Ensure your main item is well-lit and clearly visible. Natural lighting works best."),
				 SellModel(R.drawable.ic_composition,R.color.tertiaryContainer,"Proper Composition","Center your main item and keep the background clean and uncluttered."),
				 SellModel(R.drawable.camera,R.color.successContainer,"High Quality","Use a high-resolution image that's sharp and clear. Avoid blurry photos."),
				 SellModel(R.drawable.ic_colour_trey,R.color.successContainer,"Attractive Colors","Choose images with vibrant colors that catch attention but aren't overwhelming."),
			 )
		 )*/

		bind.pickThumbnail.setHapticClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(mCtx, isCamera = true, isGallery = true))
				}
			}
		}

		val adapter = ThumbnailTipsAdapter(mList = tipsList, "getStarted", object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {

			}

		})

		bind.recycler.adapter = adapter

		val goodsAdapter = GoodsExampleAdapter(goodsList)

		bind.goodsRecycler.adapter = goodsAdapter

		bind.continueBtn.setHapticClickListener {

			when {

				viewModel.thumbnail.isEmpty() -> {
					Alerts.error(mCtx, "Please select ThumbNail")
				}

				else -> {
					if (from == "dash") {
						findNavController().navigate(ids.selectThumbnail_to_createProductFragment)
					} else {
						findNavController().navigate(ids.goToProductTipsFragment)
					}
				}

			}

		}

		bind.loader.isVisible = true

		viewModel.getAllTips("thumbnail".request())

		viewModel.getAllTipsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data
					tipsList.clear()
					goodsList.clear()

					mData?.tips?.forEach {
						tipsList.add(it)
					}

					mData?.example?.forEach {
						goodsList.add(it)
					}

					goodsAdapter.notifyDataSetChanged()

					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

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

}