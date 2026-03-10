package io.bidswipe.app.ui.agoraStream

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.databinding.AuctionSettingsSheetBinding
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.PriceFormatter
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

object AuctionSettingsSheetHelper {

    data class Result(
        val startingBid: String,
        val requiredTimeSeconds: Int,
        val counterTimerSeconds: Int,
        val suddenDeath: Boolean
    )

    /**
     * Shows the auction settings bottom sheet and invokes [onConfirm] with the
     * selected values after validation passes.
     *
     * The UI and validation logic are shared between all callers to avoid duplication.
     */
    fun show(
        context: Context,
        initialPrice: String,
        onConfirm: (Result) -> Unit
    ) {
        var selectedCounterTimer = 5
        var selectedRequiredTime = 30

        val binding = AuctionSettingsSheetBinding.bind(
            LayoutInflater.from(context).inflate(
                R.layout.auction_settings_sheet,
                null,
                false
            )
        )

        val sheet = Alerts.appBottomSheet(context, true, binding)

        // Extra timer chips (in seconds)
        val extraTimer = listOf(5, 7, 10)
        extraTimer.forEachIndexed { index, time ->
            val chip = Utils.makeAChip(
                mCtx = context,
                text = "${time}s",
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                binding.timerChips.check(chip.id)
                selectedCounterTimer = time
            }
            binding.timerChips.addView(chip)
        }

        val requiredTimeList = listOf(15, 30, 45)
        val requiredTimeAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            requiredTimeList
        )

        binding.requiredTime.setAdapter(requiredTimeAdapter)
        binding.requiredTime.setText("30s", false)

        binding.requiredTime.setOnItemClickListener { _, _, position, _ ->
            selectedRequiredTime = requiredTimeList[position]
            binding.requiredTime.setText("${requiredTimeList[position]}s", false)
        }

        binding.requiredTime.setHapticClickListener {
            binding.requiredTime.showDropDown()
        }

        binding.startingBid.addTextChangedListener(
            PriceFormatter(
                binding.startingBid
            )
        )
        binding.startingBid.setText(initialPrice)

        binding.close.setHapticClickListener { sheet.dismiss() }

        binding.suddenDeath.setOnCheckedChangeListener { _, isChecked ->
            binding.counterTimerLayout.isVisible = !isChecked
            if (isChecked) selectedCounterTimer = 0
        }

        binding.start.setHapticClickListener {
            when {
                selectedRequiredTime == 0 -> {
                    Alerts.error(context, "Please select required time")
                    return@setHapticClickListener
                }

                selectedCounterTimer == 0 && !binding.suddenDeath.isChecked -> {
                    Alerts.error(context, "Please select counter timer")
                    return@setHapticClickListener
                }

                binding.startingBid.value().isEmpty() -> {
                    Alerts.error(context, "Please enter starting bid")
                    return@setHapticClickListener
                }

                else -> {
                    onConfirm(
                        Result(
                            startingBid = binding.startingBid.value(),
                            requiredTimeSeconds = selectedRequiredTime,
                            counterTimerSeconds = selectedCounterTimer,
                            suddenDeath = binding.suddenDeath.isChecked
                        )
                    )
                    sheet.dismiss()
                }
            }
        }

        sheet.show()
    }
}

