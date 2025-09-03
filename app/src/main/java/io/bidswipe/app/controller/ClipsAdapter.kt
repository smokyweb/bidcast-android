package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ClipsItemBinding

class ClipsAdapter : BaseAdapter<String, ClipsItemBinding>(mutableListOf()) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ClipsItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ClipsItemBinding>,
		position: Int,
		item: String?,
	) {
		with(holder) {
			bind.title.text = item ?: "Category Name"
			bind.categoryImage.setImageResource(R.drawable.avatar)

			bind.root.setOnClickListener {
			}
		}
	}
}