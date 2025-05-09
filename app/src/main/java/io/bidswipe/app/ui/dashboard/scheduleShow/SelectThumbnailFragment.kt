package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.GoodsExampleAdapter
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentSelectThumbnailBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids

class SelectThumbnailFragment : BaseFragment<ScheduleShowViewModel,FragmentSelectThumbnailBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSelectThumbnailBinding.inflate(inflater,view,false)

    private var tipsList = mutableListOf<SellModel>()

    private var goodsList = mutableListOf("","","","")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        tipsList.clear()
        tipsList.addAll(
            listOf(
                SellModel(R.drawable.ic_bulb,R.color.secondaryContainer,"Good Lighting","Ensure your main item is well-lit and clearly visible. Natural lighting works best."),
                SellModel(R.drawable.ic_composition,R.color.tertiaryContainer,"Proper Composition","Center your main item and keep the background clean and uncluttered."),
                SellModel(R.drawable.camera,R.color.successContainer,"High Quality","Use a high-resolution image that's sharp and clear. Avoid blurry photos."),
                SellModel(R.drawable.ic_colour_trey,R.color.successContainer,"Attractive Colors","Choose images with vibrant colors that catch attention but aren't overwhelming."),
            )
        )

        val adapter = SellAdapter(mList = tipsList, "getStarted",object: RecyclerClicks {
            override fun viewClick(pos: Int) {

            }

            override fun itemClick(pos: Int, status: String) {

            }

        })

        bind.recycler.adapter = adapter

        val gAdapter = GoodsExampleAdapter(goodsList)

        bind.goodsRecycler.adapter = gAdapter

        bind.continueBtn.setOnClickListener {
            findNavController().navigate(ids.goToProductTipsFragment)
        }



    }

}