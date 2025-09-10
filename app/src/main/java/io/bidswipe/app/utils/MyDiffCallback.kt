package io.bidswipe.app.utils

import androidx.recyclerview.widget.DiffUtil
import io.bidswipe.app.network.response.GetMyShowResponse

class MyDiffCallback: DiffUtil.ItemCallback<GetMyShowResponse.Data?>() {

	override fun areItemsTheSame(
		oldItem: GetMyShowResponse.Data,
		newItem: GetMyShowResponse.Data
	): Boolean {
		return oldItem.id == newItem.id
	}

	override fun areContentsTheSame(
		oldItem: GetMyShowResponse.Data,
		newItem: GetMyShowResponse.Data
	): Boolean {
		return oldItem == newItem // Data class comparison
	}
}