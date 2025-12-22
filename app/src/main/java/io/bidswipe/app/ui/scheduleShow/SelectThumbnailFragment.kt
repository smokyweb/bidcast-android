package io.bidswipe.app.ui.scheduleShow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
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
import io.bidswipe.app.utils.cropper.CustomCropImageContract

import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

@SuppressLint("NotifyDataSetChanged")
class SelectThumbnailFragment :
	BaseFragment<ScheduleShowViewModel, FragmentSelectThumbnailBinding>() {

	override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSelectThumbnailBinding.inflate(inflater, view, false)

	private var tipsList = mutableListOf<GetAllTipsResponse.Data.Tip?>()

	private var goodsList = mutableListOf<String?>()
	
	private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
		if (result.isSuccessful) {
			val imagePath = result.getUriFilePath(mCtx, true)
			if (imagePath != null) {
				bind.imgCard.isVisible = true
				bind.pickImageLayout.isVisible = false
				bind.img.setImageURI(imagePath.toUri())
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

		if(viewModel.thumbnail.isNotEmpty()){
			bind.imgCard.isVisible = true
			bind.pickImageLayout.isVisible = false
			if(viewModel.thumbnail.contains(Const.BASE_URL)){
				bind.img.loadUrl(mCtx,viewModel.thumbnail)
			}else{
				bind.img.setImageURI(viewModel.thumbnail.toUri())
			}
		}

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
						if(!viewModel.showId.isNullOrEmpty()){
							findNavController().navigate(ids.action_selectThumbnailFragment_to_addProductFragment)
						}else {
							findNavController().navigate(ids.selectThumbnail_to_createProductFragment)
						}
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

					mData?.tips?.forEach { tip ->
						tipsList.add(tip)
					}

					mData?.example?.forEach { example->
						goodsList.add(example)
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