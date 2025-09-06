package io.bidswipe.app.interfaces

import io.bidswipe.app.ui.custom.AppBottomSheet

interface AlertClicks {
	fun primaryClick(dialog : AppBottomSheet)

	fun secondaryClick(dialog : AppBottomSheet)
}