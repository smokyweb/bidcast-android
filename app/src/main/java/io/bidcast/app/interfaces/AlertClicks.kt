package io.bidcast.app.interfaces

import io.bidcast.app.ui.custom.AppBottomSheet


interface AlertClicks {
	fun primaryClick(dialog: AppBottomSheet)

	fun secondaryClick(dialog: AppBottomSheet)
}