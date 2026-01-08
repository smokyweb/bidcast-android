package io.bidswipe.app.ui.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSelectCategoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class SelectCategoryFragment : BaseFragment<ScheduleShowViewModel, FragmentSelectCategoryBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentSelectCategoryBinding.inflate(inflater, view, false)

    private var categoryList = mutableListOf<GetCategoryResponse.Data?>()
    private var auctionTypeList = mutableListOf<GetAuctionTypeResponse.Data?>()
    private var repeatModes = mutableListOf( "No Repeat", "Daily", "Weekly")
    private var categoryId = ""
    private var auctionId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from").toString()

        bind.header.onBackClick {
            if (from == "dash") {
                findNavController().popBackStack()
            } else {
                finish()
            }
        }

        if (!viewModel.showId.isNullOrEmpty()) {
            auctionId = viewModel.auctionId
            categoryId = viewModel.categoryId
            bind.repeat.setText(viewModel.repeatType,false)
            bind.explicitSwitch.isChecked = viewModel.explicitContent == "1"
            bind.language.setText(viewModel.primaryLanguage)

           bind.publicButton.isChecked= viewModel.discoverability =="public"
           bind.privateButton.isChecked= viewModel.discoverability =="private"
        }

        bind.publicButton.isChecked = true

        bind.continueBtn.setHapticClickListener {
            when {
                categoryId.isEmpty() -> {
                    Alerts.error(mCtx, "Please Select a Category")
                }

                auctionId.isEmpty() -> {
                    Alerts.error(mCtx, "Please select an Auction Type")
                }

                else -> {

                    viewModel.auctionId = auctionId
                    viewModel.categoryId = categoryId
                    viewModel.repeatMode = if (bind.repeat.value().isEmpty()) "0" else "1"
                    viewModel.repeatType = bind.repeat.value().ifEmpty { null }.toString()
                    viewModel.explicitContent = if (bind.explicitSwitch.isChecked) "1" else "0"
                    viewModel.primaryLanguage = bind.language.value().ifEmpty { null }.toString()

                    if (bind.publicButton.isChecked) {
                        viewModel.discoverability = "public"
                    } else {
                        viewModel.discoverability = "private"
                    }


                    findNavController().navigate(ids.goToSelectThumbnailFragment)
                }

            }
        }

        bind.category.setOnItemClickListener { _, _, position, _ ->
            categoryId = categoryList[position]?.id.toString()
        }

        bind.category.setHapticClickListener {
            bind.category.showDropDown()
        }

        bind.auctionType.setOnItemClickListener { _, _, position, _ ->
            auctionId = auctionTypeList[position]?.id.toString()
        }

        bind.auctionType.setHapticClickListener {
            bind.auctionType.showDropDown()
        }

        val repeatModeAdapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, repeatModes.map { it })
        bind.repeat.setAdapter(repeatModeAdapter)
        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.repeat.setDropDownBackgroundDrawable(draw)
        bind.repeat.setOnItemClickListener { _, _, position, _ ->
            viewModel.repeatMode = repeatModes[position]
        }

        bind.repeat.setHapticClickListener {
            bind.repeat.showDropDown()
        }

        val languageAdapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, Const.languages.map { it.title })
        bind.language.setAdapter(languageAdapter)
        bind.language.setDropDownBackgroundDrawable(draw)
        bind.language.setOnItemClickListener { _, _, position, _ ->
        }

        bind.language.setHapticClickListener {
            bind.language.showDropDown()
        }

        bind.publicButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bind.privateButton.isChecked = false
            }
        }

        bind.privateButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bind.publicButton.isChecked = false
            }
        }

        bind.publicButtonLayout.setHapticClickListener {
            bind.publicButton.isChecked = true
        }

        bind.privateButtonLayout.setHapticClickListener {
            bind.privateButton.isChecked = true
        }

        bind.loader.isVisible = true
        viewModel.getCategory()
        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    if (it.value.data?.isNotEmpty() == true) {
                        bind.loader.isVisible = false
                        viewModel.getCategoryRepo.value = null
                        categoryList.clear()
                        categoryList.addAll(it.value.data)

                        val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, categoryList.map { it?.name })
                        bind.category.setAdapter(adapter)
                        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                        bind.category.setDropDownBackgroundDrawable(draw)

                        if(categoryId.isNotEmpty()){
                            bind.category.setText(categoryList.find { it?.id.toString() == categoryId }?.name, false)
                        }

                    }
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

        viewModel.getAuctionType()
        viewModel.getAuctionTypeRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    if (it.value.data?.isNotEmpty() == true) {
                        auctionTypeList.clear()
                        auctionTypeList.addAll(it.value.data)

                        val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, auctionTypeList.map { it?.name })
                        bind.auctionType.setAdapter(adapter)
                        val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
                        bind.auctionType.setDropDownBackgroundDrawable(draw)

                        if(auctionId.isNotEmpty()){
                            bind.auctionType.setText(auctionTypeList.find { it?.id.toString() == auctionId }?.name, false)
                        }

                    }
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